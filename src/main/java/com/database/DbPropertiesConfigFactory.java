package com.database;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.dbcp.ConnectionFactory;

import com.dibs.database.builder.StatementBuilder;
import com.dibs.database.config.DefaultConstants;
import com.dibs.database.exception.SQLExceptionHandler;
import com.dibs.database.orm.BeanConverter;
import com.dibs.database.rs.ResultSetProcessor;
import com.dibs.util.resource.PropertyGroupUtil;

public class DbPropertiesConfigFactory implements DbPoolConfigFactory, DefaultConstants {

    public static final String DEFAULT_FILE_CONFIG = "dbconfig.properties";

    private PropertyGroupUtil propertyUtil;

    private ConcurrentMap<String, DbPoolConfig> configMap;

    private Date loadedDate;

    public DbPropertiesConfigFactory() {
        this(null);
    }

    public DbPropertiesConfigFactory(String fileName) {
        propertyUtil = new PropertyGroupUtil(fileName == null ? DEFAULT_FILE_CONFIG : fileName);
        configMap = new ConcurrentHashMap<String, DbPoolConfig>();
        this.loadedDate = new Date();
    }

    @Override
    public DbPoolConfig getDefaultPool() {
        return propertyUtil.getDefaultGroupName() == null ? null : getPool(propertyUtil.getDefaultGroupName());
    }

    @Override
    public DbPoolConfig getPool(String poolName) {
        DbPoolConfig config = configMap.get(poolName);

        if (config == null) {
            Properties properties = propertyUtil.getGroup(poolName);
            if (properties != null) {
                configMap.put(poolName, config = load(poolName, properties));
            }
        }

        return config;

    }

    private DbPoolConfig load(String poolName, Properties properties) {
        try {
            DbPoolConfig config = new DbPoolConfig();
            config.setPoolName(poolName);

            /*
             * required properties
             */
            config.setUser(
                    properties.getProperty("user"));
            config.setPassword(
                    properties.getProperty("password"));
            config.setConnectionString(
                    properties.getProperty("connectionString"));

            config.setAutoCommit(
                    Boolean.parseBoolean(properties.getProperty("autoCommit", AUTO_COMMIT)));
            config.setInitialSize(
                    Integer.parseInt(properties.getProperty("initialSize", INITIAL_SIZE)));
            config.setMaxIdle(
                    Integer.parseInt(properties.getProperty("maxIdle", MAX_IDLE)));
            config.setMinIdle(
                    Integer.parseInt(properties.getProperty("minIdle", MIN_IDLE)));
            config.setMaxSize(
                    Integer.parseInt(properties.getProperty("maxSize", MAX_SIZE)));
            config.setBatchSize(
                    Integer.parseInt(properties.getProperty("batchSize", BATCH_SIZE)));

            // wait 10 secs to get a connection from pool
            config.setMaxWaitMillis(Integer.parseInt(properties.getProperty("maxWaitMillis", MAX_WAIT_MILLIS)));

            // kill connections which have been idle for 1 min
            config.setMinEvictableIdleTimeMillis(
                    Integer.parseInt(properties.getProperty("minEvictableIdleTimeMillis", MIN_EVICTABLE_IDLE_TIME_MILLIS)));

            config.setMinSize(
                    Integer.parseInt(properties.getProperty("minSize", MIN_SIZE)));

            config.setQueryTimeoutSecs(
                    Integer.parseInt(properties.getProperty("queryTimeoutSecs", QUERY_TIMEOUT_SECS)));

            config.setReadOnly(
                    Boolean.parseBoolean(properties.getProperty("readOnly", READ_ONLY)));

            config.setSoftMinEvictableIdleTimeMillis(
                    Integer.parseInt(properties.getProperty("softMinEvictableIdleTimeMillis", SOFT_MIN_EVICTABLE_IDLE_TIME_MILLIS)));

            config.setStatementPoolSize(
                    Integer.parseInt(properties.getProperty("statementPoolSize", STATEMENT_POOL_SIZE)));

            config.setTestOnBorrow(
                    Boolean.parseBoolean(properties.getProperty("testOnBorrow", TEST_ON_BORROW)));

            config.setTestOnReturn(
                    Boolean.parseBoolean(properties.getProperty("testOnReturn", TEST_ON_RETURN)));

            config.setTestWhileIdle(
                    Boolean.parseBoolean(properties.getProperty("testWhileIdle", TEST_WHILE_IDLE)));

            // test max N connections during each eviction run
            String defaultTestPerEviciton = TEST_PER_EVICTION_RUN;
            if (config.getMaxSize() != 0) {
                Double floor = Math.floor(config.getMaxSize() * TEST_PER_EVICTION_RUN_MULTIPLIER);
                defaultTestPerEviciton = String.valueOf(floor.intValue());
            }
            config.setTestsPerEvictionRun(
                    Integer.parseInt(properties.getProperty("testsPerEvictionRun", defaultTestPerEviciton)));

            // run evictor thread every 20 seconds and remove connections which have been idle for 1 min
            config.setTimeBetweenEvictionRunsMillis(
                    Integer.parseInt(properties.getProperty("timeBetweenEvictionRunsMillis", TIME_BETWEEN_EVICTION_RUNS_MILLIS)));

            config.setValidationQuery(
                    properties.getProperty("validationQuery", VALIDATION_QUERY));

            config.setQueryTimeoutSecs(
                    Integer.parseInt(properties.getProperty("validationQueryTimeoutSecs", VALIDATION_QUERY_TIMEOUT_SECS)));

            config.setAutoGeneratedKeys(
                    Integer.parseInt(properties.getProperty("autoGeneratedKeys", String.valueOf(Statement.RETURN_GENERATED_KEYS))));

            config.setProperties(properties);

            config.setDriverClassName(
                    properties.getProperty("driverClassName", DRIVER_CLASS_NAME));

            config.setFetchSize(
                    new Integer(properties.getProperty("fetchSize", FETCH_SIZE)));

            /*
             * ConnectionFactory
             */
            ConnectionFactory connectionFactory = (ConnectionFactory) Class.forName(properties.getProperty("connectionFactoryClass", CONNECTION_FACTORY)).newInstance();
            config.setConnectionFactory(connectionFactory);

            /*
             * StatementBuilder
             */
            StatementBuilder statementBuilder = (StatementBuilder) Class.forName(properties.getProperty("statementBuilder", STATEMENT_BUILDER)).newInstance();
            config.setStatementBuilder(statementBuilder);

            /*
             * ResultSetProcessor
             */
            ResultSetProcessor resultSetProcessor = (ResultSetProcessor) Class.forName(properties.getProperty("resultSetProcessor", RESULT_SET_PROCESSOR)).newInstance();
            config.setResultSetProcessor(resultSetProcessor);

            /*
             * SQLExceptionHandler
             */
            SQLExceptionHandler sqlExceptionHandler = (SQLExceptionHandler) Class.forName(properties.getProperty("sqlExceptionHandler", EXCEPTION_HANDLER)).newInstance();
            config.setSqlExceptionHandler(sqlExceptionHandler);

            /*
             * BeanConverter
             */
            BeanConverter beanConverter = (BeanConverter) Class.forName(properties.getProperty("beanConverter", BEAN_CONVERTER)).newInstance();
            config.setBeanConverter(beanConverter);

            /*
             * Call configure each Configurable element
             */
            configure(connectionFactory, config);
            configure(statementBuilder, config);
            configure(sqlExceptionHandler, config);
            configure(beanConverter, config);
            configure(resultSetProcessor, config);

            return config;
        } catch (Exception ex) {
            throw new RuntimeException("Error loading db config", ex);
        }
    }

    private void configure(Object configurable, DbPoolConfig config) {
        if (configurable instanceof Configurable) {
            ((Configurable) configurable).setPoolConfig(config);
        }
    }

    @Override
    public List<String> getAllPoolNames() {
        if (propertyUtil != null && propertyUtil.getGroups() != null) {
            return new ArrayList<>(propertyUtil.getGroups());
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public PropertyGroupUtil getPropertyUtil() {
        return propertyUtil;
    }

    public Date getLoadedDate() {
        return loadedDate;
    }
}

