package com.database;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import com.common.def.ThreadLocalKey;
import com.common.def.YesNo;
import com.dibs.database.*;
import com.dibs.service.common.ReadTimeAuditable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dibs.database.builder.StatementBuilder;
import com.dibs.database.builder.impl.IfBound;
import com.dibs.database.orm.impl.DefaultBeanClassFactory;
import com.dibs.database.rs.ResultSetProcessor;
import com.dibs.lib.database.exception.SQLExceptionFactory;
import com.dibs.lib.database.exception.SQLRuntimeException;
import com.dibs.lib.shard.Key;
import com.dibs.lib.shard.Shard;
import com.dibs.lib.shard.Shardable;
import com.dibs.lib.shard.Sharder;
import com.dibs.lib.util.Assert;
import com.dibs.lib.util.IdMethodUtil;
import com.dibs.service.common.PageResponse;
import com.dibs.service.common.Pageable;
import com.dibs.service.common.def.ThreadLocalKey;
import com.dibs.service.v1.data.Vertical;
import com.dibs.service.v1.data.YesNo;

public class DbHelper<R>
{

    private static final String READ_REPLICA_ENABLED_PROPERTY = "readReplicaEnabled";

    private static final Logger LOG = LoggerFactory.getLogger(DbHelper.class);

    private static Integer ZERO = new Integer(0);

    public static String DB_SCHEMA = "dbschema";
    public static String SHARD_PREFIX = "shard.";
    public static int MAX_PAGE_SIZE = 200;

    public static Set<Class<?>> ID_METHOD_VALID_TYPES = new HashSet<Class<?>>(//
            Arrays.asList(Long.class, Long.TYPE, String.class, Integer.class, Integer.TYPE)//
    );

    private String sql;
    private List<Object> parameters;
    private String connectionPool;
    private boolean fetchTotalCount;
    private Long generatedId;
    private boolean loadGeneratedId = true;
    private BeanFactory<R> resultClassFactory;

    private DbSchemaAware[] dbSchemaAware;

    private ResultSetProcessor<List<R>> resultSetProcessor;
    private StatementBuilder statementBuilder;

    private TotalResultsProcessor totalResultsProcessor;

    private boolean ignoreThreadLocalPagination;

    private int maxPageSize = MAX_PAGE_SIZE;
    private Integer pageStart;
    private Integer pageSize;
    private YesNo returnTotalResults;
    private String pageContext;
    private TransactionMode transactionMode;
    private Boolean protectWhereClause;

    private boolean forceAccordionAppend = false;

    private Sharder sharder;

    private Shard shard;

    private boolean readReplicaEnabled;

    //override if your non-transactional reads must go to master
    private boolean readReplicaOverride;

    public DbHelper()
    {
        String readReplicaProperty = System.getProperty(READ_REPLICA_ENABLED_PROPERTY);

        if (StringUtils.isEmpty(readReplicaProperty)) {
            LOG.warn("readReplicaEnabled property is not in system properties, will be set to false");
        }

        readReplicaEnabled = Boolean.valueOf(readReplicaProperty);
    }

    public DbHelper(String sql, Object... parameters)
    {
        this();

        setSql(sql).setParameters(parameters);
    }

    public PageResponse<R> executeSelect(Class<R> resultClass) throws SQLRuntimeException
    {
        return setResultClassFactory(new DefaultBeanClassFactory<R>(resultClass)).executeSelect();
    }

    public PageResponse<R> executeSelect() throws SQLRuntimeException
    {
        try
        {
            Assert.isNotNull(resultClassFactory, "resultClassFactory is not provided");

            PageResponse<R> results = null;

            populatePageableFields();

            // if pageSize == 0 we only want to fetch total count
            Integer curPageStart = parsePageStart();
            Integer curPageSize = parsePageSize();

            if (!ZERO.equals(pageSize))
            {
                DbSchemaAware dbSchemaAware = null;

                if (this.dbSchemaAware != null && this.dbSchemaAware.length > 0)
                {
                    dbSchemaAware = this.dbSchemaAware[0];
                }
                else if (parameters instanceof DbSchemaAware)
                {
                    dbSchemaAware = (DbSchemaAware) parameters;
                }

                UnorderedTransaction transaction = UnorderedTransaction.getThreadInstance();

                TransactionMode txMode = getReadTransactionMode();

                if (txMode == TransactionMode.REQUIRED && transaction == null)
                {
                    // required and no transaction --> blow up
                    throw new RuntimeException("Transaction required for this select");
                }
                else if (txMode == TransactionMode.DONT_USE)
                {
                    // force no use --> set transaction to null, will grab new
                    // connection below
                    transaction = null;
                }

                String readConnectionPool = determineReadConnectionPool(transaction, txMode);

                BeanSelectStatement<List<R>, R> selectStatement = transaction == null ? new BeanSelectStatement<List<R>, R>(
                        readConnectionPool, resultClassFactory)
                        : new BeanSelectStatement<List<R>, R>(transaction,
                        resultClassFactory);

                selectStatement.setSql(sql);
                loadParameters(selectStatement);

                if (statementBuilder != null)
                {
                    selectStatement.setStatementBuilder(statementBuilder);
                }

                if (resultSetProcessor != null)
                {
                    selectStatement.setResultSetProcessor(resultSetProcessor);
                }

                if (dbSchemaAware != null)
                {
                    selectStatement.addNamed(DB_SCHEMA, dbSchemaAware.getDbSchema());
                }

                selectStatement.addNamed(Pageable.PAGE_START_QUERY_PARAM, curPageStart);
                selectStatement.addNamed(Pageable.PAGE_SIZE_QUERY_PARAM, curPageSize);
                selectStatement.addNamed(IfBound.FORCE_APPEND_KEY, forceAccordionAppend);


                results = transaction == null ?
                        new PageResponse<R>(selectStatement.executeSelect()) :
                        new PageResponse<R>(transaction.execute(selectStatement).getResult());

                if (CollectionUtils.isNotEmpty(results) && results.get(0) instanceof ReadTimeAuditable) {
                    for (Object result : results) {
                        ((ReadTimeAuditable) result).setReadTime(new Date(selectStatement.getStopTimeMillis()));
                    }
                }
            }

            Long totalResults = null;

            if (fetchTotalCount)
            {
                totalResults = executeSelectTotalResults();
            }

            results = results == null ? new PageResponse<>() : results;
            if (this.ignoreThreadLocalPagination)
            {
                results.setTotalResults(totalResults);
            }
            return results;

        }
        catch (SQLException ex)
        {
            throw SQLExceptionFactory.buildException(ex);
        }
        catch (RuntimeException ex)
        {
            throw ex;
        }

    }

    /*
     * Will split off non-transactional reads to a read replica. If the DBHelper's connectionPool is
     * set to master it will ignore it. This is to protect against places in the code where we're
     * setting db connection pool to master but not really needing to. Please set the readReplicaOverride
     * in cases where non-transactional reads must go to master (It's likely you shouldn't be doing that).
     */
    private String determineReadConnectionPool(UnorderedTransaction transaction, TransactionMode txMode) {
        String readConnectionPool = connectionPool;

        if (readReplicaEnabled) {
            if (isReadReplicaOverride() || (StringUtils.isNotEmpty(connectionPool)
                    && !connectionPool.equals(DbConnectionPool.MASTER))) {
                readConnectionPool = connectionPool;
            } else if (txMode == TransactionMode.DONT_USE || transaction == null) {
                readConnectionPool = DbConnectionPool.READ_REPLICA.getName();
            }
        }

        return readConnectionPool;
    }

    private void prepareShardParameters(PreparedStatement preparedStatement)
    {
        if (shard != null)
        {
            if (shard.getParameters() != null)
            {
                for (Map.Entry<String, String> entry : shard.getParameters().entrySet())
                {
                    preparedStatement.addNamed(SHARD_PREFIX + entry.getKey(), entry.getValue());
                }
            }

        }
    }

    public R executeSelectFirstRow(Class<R> resultClass) throws SQLRuntimeException
    {
        return setResultClassFactory(new DefaultBeanClassFactory<R>(resultClass)).executeSelectFirstRow();

    }

    public R executeSelectFirstRow() throws SQLRuntimeException
    {
        List<R> result = executeSelect();
        return result == null || result.isEmpty() ? null : result.get(0);
    }

    public void inflateShardableKeyForInsert(Shardable shardable)
    {
        if (sharder != null)
        {
            if (isInsert())
            {
                Key key = shardable.getKey();
                Assert.isNotNull(key, "Shard key cannot be null for shardable entities");

                String id = key.getId();
                if (id == null)
                {
                    String sourceId = key.getSourceId();
                    Assert.isNotNull(sourceId, "source id cannot be null for shardable entities");

                    key = sharder.generateKey(sourceId);
                    shardable.setKey(key);
                }
            }
        }

    }

    private Boolean isInsert()
    {
        Boolean isInsert = Boolean.FALSE;

        if (this.sql != null)
        {
            String upperSql = this.sql.toUpperCase().trim();
            isInsert = upperSql.startsWith("INSERT");
        }

        return isInsert;
    }

    public UpdateStatement[] executeUpdate() throws SQLRuntimeException
    {
        try
        {
            Assert.isNotNull(sql, "sql is null");

            UnorderedTransaction transaction = UnorderedTransaction.getThreadInstance();

            TransactionMode txMode = getWriteTransactionMode();

            if (txMode == TransactionMode.REQUIRED && transaction == null)
            {
                // required and no transaction --> blow up
                throw new RuntimeException("Transaction required for this update or insert");
            }
            else if (txMode == TransactionMode.DONT_USE)
            {
                // force no use --> set transaction to null, will grab new
                // connection below
                transaction = null;
            }

            if (transaction != null &&
                    transaction.getPoolName() != null &&
                    connectionPool != null &&
                    !connectionPool.equals(transaction.getPoolName()))
            {
                LOG.warn("executeUpdate(): CONFLICT!!! configured connectionPool \"{}\" will be overridden by db-transaction's connection pool \"{}\"", connectionPool, transaction.getPoolName());
            }

            DbSchema[] dbSchemas = getDbSchmas();

            UpdateStatement[] updateStatements = new UpdateStatement[dbSchemas.length];

            for (int i = 0; i < dbSchemas.length; i++)
            {

                UpdateStatement updateStatement = transaction == null ? new UpdateStatement(connectionPool)
                        : new UpdateStatement(transaction);

                loadParameters(updateStatement);

                if (updateStatement.getNamed("dbschema") != null)
                {
                    updateStatement.addNamed(DB_SCHEMA, updateStatement.getNamed("dbschema"));
                }
                else
                {
                    updateStatement.addNamed(DB_SCHEMA, dbSchemas[i]);
                }
                updateStatement.addNamed(IfBound.FORCE_APPEND_KEY, forceAccordionAppend);

                updateStatement.setSql(sql);

                if (statementBuilder != null)
                {
                    updateStatement.setStatementBuilder(statementBuilder);
                }

                if (transaction == null)
                {
                    updateStatement.execute();
                }
                else
                {
                    transaction.execute(updateStatement);
                }

                updateStatements[i] = updateStatement;

                if (parameters != null && i == 0)
                {
                    parseGeneratedId(parameters, updateStatement);
                }

            }

            return updateStatements;
        }
        catch (SQLException ex)
        {
            throw SQLExceptionFactory.buildException(ex);
        }
        catch (RuntimeException ex)
        {
            throw ex;
        }

    }

    public Long executeSelectTotalResults() throws SQLRuntimeException
    {
        try
        {
            Long totalResults = null;

            if (shouldComputerTotalResults())
            {
                totalResultsProcessor = Optional.ofNullable(totalResultsProcessor).
                        orElse(new ReplaceSelectTotalResultsProcessor());


                sql = totalResultsProcessor.process(this.sql);

                UnorderedTransaction transaction = UnorderedTransaction.getThreadInstance();

                TransactionMode txMode = getReadTransactionMode();
                String readConnectionPool = determineReadConnectionPool(transaction, txMode);

                if (txMode == TransactionMode.REQUIRED && transaction == null)
                {
                    // required and no transaction --> blow up
                    throw new RuntimeException("Transaction required for this select");
                }
                else if (txMode == TransactionMode.DONT_USE)
                {
                    // force no use --> set transaction to null, will grab new
                    // connection below
                    transaction = null;
                }

                SelectStatement<ResultSet> selectStatement = transaction == null
                        ? new SelectStatement<ResultSet>(readConnectionPool)
                        : new SelectStatement<ResultSet>(transaction);

                selectStatement.setSql(sql);
                loadParameters(selectStatement);
                selectStatement.addNamed(IfBound.FORCE_APPEND_KEY, forceAccordionAppend);

                if (statementBuilder != null)
                {
                    selectStatement.setStatementBuilder(statementBuilder);
                }

                @SuppressWarnings("resource")
                ResultSet rs = transaction == null ?
                        selectStatement.executeSelect() :
                        transaction.execute(selectStatement).getResult();

                totalResults = 0L;
                while (rs.next())
                {
                    totalResults += rs.getLong(1);
                }

                if (isPageContextMatch() && !this.ignoreThreadLocalPagination)
                {
                    ThreadLocalKey.TOTAL_RESULTS.put(totalResults);
                }
            }
            return totalResults;
        }
        catch (SQLException ex)
        {
            throw SQLExceptionFactory.buildException(ex);
        }
        catch (RuntimeException ex)
        {
            throw ex;
        }

    }


    /*
     * For context - pagination has traditionally been done with thread local values. This has lead to a number of issues, and as a result, we have moved to a more explicit means of
     * handling pagination and total results requests. However, for backwards compatibility, we need to support both. This handles the logic for whether to compute total results.
     */
    private boolean shouldComputerTotalResults()
    {
        //the value from thread local (or from query param returnTotalResults=Y|N)
        boolean threadLocalPaginationQueryParamValue = ((YesNo) Optional.ofNullable(ThreadLocalKey.RETURN_TOTAL_RESULTS.get()).orElse(YesNo.N)).booleanValue();

        //if threadLocalPagination is on and total results was requested
        boolean threadLocalEnabled = !this.ignoreThreadLocalPagination && threadLocalPaginationQueryParamValue;

        //ignore thread local && total results was requested
        boolean returnTotalResultsEnabled = this.ignoreThreadLocalPagination && Optional.ofNullable(returnTotalResults).orElse(YesNo.N).booleanValue();

        //if total results was requested in either of the two ways, return true
        return threadLocalEnabled || returnTotalResultsEnabled;
    }

    private DbSchema[] getDbSchmas()
    {

        if (dbSchemaAware == null || dbSchemaAware.length == 0)
        {
            // param null or size 0
            return new DbSchema[1];
        }

        if (dbSchemaAware.length == 1 && dbSchemaAware[0] == null)
        {
            // weird case when using ... 1st null param comes in
            return new DbSchema[1];
        }

        // params passed in
        DbSchema[] out = new DbSchema[dbSchemaAware.length];

        for (int i = 0; i < out.length; i++)
        {
            out[i] = dbSchemaAware[i].getDbSchema();
        }

        return out;
    }

    private Integer parsePageStart()
    {
        if (this.pageStart != null)
        {
            return this.pageStart;
        }

        Integer pageStart = null;
        if (!this.ignoreThreadLocalPagination)
        {
            pageStart = ThreadLocalKey.PAGE_START.get();
        }

        return isPageContextMatch() && pageStart != null ? pageStart : 0;
    }

    private Integer parsePageSize()
    {
        // page size set locally
        if (this.pageSize != null)
        {
            return this.pageSize;
        }

        Integer reqPageSize = null;
        if (!this.ignoreThreadLocalPagination)
        {
            reqPageSize = ThreadLocalKey.PAGE_SIZE.get();
        }

        // if page match return requested size or max size
        if (isPageContextMatch())
        {
            return reqPageSize == null || reqPageSize > maxPageSize ? maxPageSize : reqPageSize;
        }
        else
        {
            return maxPageSize;
        }

    }

    private boolean isPageContextMatch()
    {
        Object reqPageContext = ThreadLocalKey.PAGE_CONTEXT.get();
        return reqPageContext == null || reqPageContext.toString().equals(this.pageContext);
    }

    private Long parseGeneratedId(UpdateStatement updateStatement)
    {
        try
        {
            ResultSet generatedKeys = updateStatement.getGeneratedKeys();
            return generatedKeys != null && generatedKeys.next() ? generatedKeys.getLong(1) : null;
        }
        catch (RuntimeException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    private void parseGeneratedId(List<Object> beans, UpdateStatement updateStatement) throws SQLException
    {
        this.generatedId = parseGeneratedId(updateStatement);

        if (beans != null && !beans.isEmpty() && loadGeneratedId && generatedId != null)
        {
            for (Object bean : beans)
            {
                IdMethodUtil.setId(bean, generatedId);
            }
        }
    }

    public DbHelper<R> setSql(String sql)
    {
        this.sql = sql;
        return this;
    }

    public DbHelper<R> setParameters(final Object... parameters)
    {
        if (parameters != null)
        {
            this.parameters = new ArrayList<>(Arrays.asList(parameters));
        }
        return this;
    }

    public DbHelper<R> addParameter(Object parameter)
    {
        this.parameters = this.parameters == null ? new ArrayList<>() : this.parameters;
        this.parameters.add(parameter);
        return this;
    }

    public DbHelper<R> setConnectionPool(String connectionPool)
    {
        this.connectionPool = connectionPool;
        return this;
    }

    public DbHelper<R> setDbSchemaAware(DbSchemaAware... dbSchemaAware)
    {
        this.dbSchemaAware = dbSchemaAware;
        return this;
    }

    public DbHelper<R> setDbSchemaAware(Vertical... vertical)
    {
        this.dbSchemaAware = new DbSchemaAware[vertical.length];

        for (int i = 0; i < vertical.length; i++)
        {
            this.dbSchemaAware[i] = DbVertical.valueOfVertical(vertical[i]);
        }

        return this;
    }

    public DbHelper<R> setResultSetProcessor(ResultSetProcessor<List<R>> resultSetProcessor)
    {
        this.resultSetProcessor = resultSetProcessor;
        return this;
    }

    public DbHelper<R> setMaxPageSize(int maxPageSize)
    {
        this.maxPageSize = maxPageSize;
        return this;
    }

    public DbHelper<R> setPagination(Integer pageStart, Integer pageSize)
    {
        this.pageStart = pageStart;
        this.pageSize = pageSize;
        return this;
    }

    public DbHelper<R> setPagination(Pageable pageable)
    {
        this.pageStart = pageable.getPageStart();
        this.pageSize = pageable.getPageSize();
        return this;
    }

    public DbHelper<R> setTotalResultsProcessor(TotalResultsProcessor totalResultsProcessor)
    {
        this.totalResultsProcessor = totalResultsProcessor;
        return this;
    }

    public String getSql()
    {
        return sql;
    }

    public Object getParameters()
    {
        return parameters;
    }

    public String getConnectionPool()
    {
        return connectionPool;
    }

    public DbSchemaAware[] getDbSchemaAware()
    {
        return dbSchemaAware;
    }

    public ResultSetProcessor<List<R>> getResultSetProcessor()
    {
        return resultSetProcessor;
    }

    public TotalResultsProcessor getTotalResultsProcessor()
    {
        return totalResultsProcessor;
    }

    public int getMaxPageSize()
    {
        return maxPageSize;
    }

    public Integer getPageStart()
    {
        return pageStart;
    }

    public Integer getPageSize()
    {
        return pageSize;
    }

    public boolean isFetchTotalCount()
    {
        return fetchTotalCount;
    }

    public DbHelper<R> setFetchTotalCount(boolean fetchTotalCount)
    {
        this.fetchTotalCount = fetchTotalCount;
        return this;
    }

    public DbHelper<R> setForceAccordionAppend(boolean forceAccordionAppend)
    {
        this.forceAccordionAppend = forceAccordionAppend;
        return this;
    }

    @SuppressWarnings("unused")
    private static void nullPlaceHolder()
    {

    }

    public boolean isForceAccordionAppend()
    {
        return forceAccordionAppend;
    }

    public String getPageContext()
    {
        return pageContext;
    }

    public DbHelper<R> setPageContext(String pageContext)
    {
        this.pageContext = pageContext;
        return this;
    }

    public StatementBuilder getStatementBuilder()
    {
        return statementBuilder;
    }

    public DbHelper<R> setStatementBuilder(StatementBuilder statementBuilder)
    {
        this.statementBuilder = statementBuilder;
        return this;
    }

    public Long getGeneratedId()
    {
        return generatedId;
    }

    public TransactionMode getTransactionMode()
    {
        return transactionMode;
    }

    public DbHelper<R> setTransactionMode(TransactionMode transactionMode)
    {
        this.transactionMode = transactionMode;
        return this;
    }

    private TransactionMode getReadTransactionMode()
    {
        return transactionMode == null ? TransactionMode.USE_IF_OPEN : transactionMode;
    }

    private TransactionMode getWriteTransactionMode()
    {
        return transactionMode == null ? TransactionMode.REQUIRED : transactionMode;
    }

    private void loadParameters(PreparedStatement preparedStatement)
    {
        if (this.parameters != null)
        {
            for (Object o : parameters)
            {
                if (o instanceof Shardable && this.shard == null)
                {
                    Shardable shardable = (Shardable) o;
                    this.shard = parseShardableParameter(shardable, preparedStatement);
                    if (preparedStatement instanceof UpdateStatement)
                    {
                        inflateShardableKeyForInsert(shardable);
                    }

                }

                preparedStatement.addBean(o);
            }
        }

        // null check inside
        prepareShardParameters(preparedStatement);

        if (protectWhereClause != null)
        {
            preparedStatement.setProtectWhereClause(protectWhereClause);
        }
    }

    private Shard parseShardableParameter(Shardable shardable, PreparedStatement preparedStatement)
    {
        Key key = null;

        if (sharder != null && shardable != null)
        {
            if (shardable.getKey() != null && shardable.getKey().getId() != null)
            {
                // use existing sharded id
                key = sharder.parseKey(shardable.getKey().getId());
            }
            else if (shardable.getKey().getSourceId() != null)
            {
                // generate new key from sourceId
                key = sharder.generateKey(shardable.getKey().getSourceId());
            }
            else
            {
                // generate new key from random value
                key = sharder.generateKey(null);
            }
        }

        return key == null ? null : key.getShard();

    }

    public DbHelper<R> setLoadGeneratedId(boolean loadGeneratedId)
    {
        this.loadGeneratedId = loadGeneratedId;
        return this;
    }

    public BeanFactory<R> getResultClassFactory()
    {
        return resultClassFactory;
    }

    public DbHelper<R> setResultClassFactory(BeanFactory<R> resultClassFactory)
    {
        this.resultClassFactory = resultClassFactory;
        return this;
    }

    public Sharder getSharder()
    {
        return sharder;
    }

    public DbHelper<R> setSharder(Sharder sharder)
    {
        this.sharder = sharder;
        return this;
    }

    public Shard getShard()
    {
        return shard;
    }

    public DbHelper<R> setShard(Shard shard)
    {
        this.shard = shard;
        return this;
    }

    public Boolean getProtectWhereClause()
    {
        return protectWhereClause;
    }

    public DbHelper<R> setProtectWhereClause(Boolean protectWhereClause)
    {
        this.protectWhereClause = protectWhereClause;
        return this;
    }

    public boolean getIgnoreThreadLocalPagination()
    {
        return ignoreThreadLocalPagination;
    }

    public DbHelper<R> setIgnoreThreadLocalPagination(boolean ignoreThreadLocalPagination)
    {
        this.ignoreThreadLocalPagination = ignoreThreadLocalPagination;
        return this;
    }

    protected void populatePageableFields()
    {
        if (CollectionUtils.isNotEmpty(parameters))
        {
            parameters.stream().filter(parameter -> parameter instanceof Pageable).findFirst().
                    ifPresent(parameter ->
                    {
                        Pageable pageable = (Pageable) parameter;
                        this.ignoreThreadLocalPagination = true;
                        if (pageable.getPageSize() != null && pageable.getPageSize() >= 0)
                        {
                            this.pageSize = pageable.getPageSize();
                        }
                        if (pageable.getPageStart() != null && pageable.getPageStart() >= 0)
                        {
                            this.pageStart = pageable.getPageStart();
                        }
                        if (pageable.getReturnTotalResults() != null)
                        {
                            this.setReturnTotalResults(pageable.getReturnTotalResults());
                        }
                    });
        }
    }

    public YesNo getReturnTotalResults()
    {
        return returnTotalResults;
    }

    public void setReturnTotalResults(YesNo returnTotalResults)
    {
        this.returnTotalResults = returnTotalResults;
    }

    public boolean isReadReplicaOverride() {
        return readReplicaOverride || Boolean.TRUE.equals(ThreadLocalKey.BYPASS_READ_REPLICA.get());
    }

    public void setReadReplicaOverride(boolean readReplicaOverride) {
        this.readReplicaOverride = readReplicaOverride;
    }
}
