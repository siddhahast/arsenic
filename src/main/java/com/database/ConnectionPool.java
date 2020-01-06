package com.database;

import com.dibs.database.pool.debug.DebugEnabledConnectionFactory;
import com.dibs.service.common.log.JsonLog4j2Message;
import com.dibs.service.common.log.MessageArgs;
import com.util.log.JsonLog4j2Message;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingConnection;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.StackKeyedObjectPoolFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class ConnectionPool
{

    private static final int EXHAUSTED_POOL_BUFFER_WARNING = 2;

    private static final Logger LOG = LogManager.getLogger(ConnectionPool.class);

    private DbPoolConfig poolConfig;
    private GenericObjectPool pool;
    private PoolingDataSource dataSource;

    private static ConcurrentMap<String, ConnectionPool> CONNECTION_POOLS = new ConcurrentHashMap<String, ConnectionPool>();

    public static void clearInstances()
    {
        CONNECTION_POOLS = new ConcurrentHashMap<String, ConnectionPool>();
    }

    public static ConnectionPool getDefaultInstance()
    {
        return getInstance(null);
    }

    public static ConnectionPool getInstance(String poolName)
    {
        if (poolName == null)
        {
            DbPoolConfig poolConfig = DbPoolConfigFactory.Factory.INSTANCE.getDefaultPool();
            if (poolConfig == null)
            {
                return null;
            }
            poolName = poolConfig.getPoolName();
        }

        ConnectionPool pool = CONNECTION_POOLS.get(poolName);

        if (pool == null)
        {
            DbPoolConfig poolConfig = DbPoolConfigFactory.Factory.INSTANCE.getPool(poolName);

            if (poolConfig != null)
            {
                CONNECTION_POOLS.put(poolName, pool = new ConnectionPool(poolConfig));
            }
        }

        return pool;
    }

    private ConnectionPool(DbPoolConfig poolConfig)
    {
        this.poolConfig = poolConfig;
        this.pool = new GenericObjectPool();

        ConnectionFactory connectionFactory = poolConfig.getConnectionFactory();

        KeyedObjectPoolFactory statementPool = poolConfig.getStatementPoolSize() > 0 ? new StackKeyedObjectPoolFactory(poolConfig.getStatementPoolSize()) : null;

        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(
                connectionFactory,
                this.pool,
                statementPool,
                poolConfig.getValidationQuery(),
                poolConfig.getValidationQueryTimeoutSecs(),
                poolConfig.isReadOnly(),
                poolConfig.isAutoCommit());

        this.pool.setMaxActive(poolConfig.getMaxSize());
        this.pool.setMinIdle(poolConfig.getMinSize());
        this.pool.setMaxIdle(poolConfig.getMaxIdle());
        this.pool.setMinIdle(poolConfig.getMinIdle());
        this.pool.setMaxWait(poolConfig.getMaxWaitMillis());
        this.pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
        this.pool.setMinEvictableIdleTimeMillis(poolConfig.getMinEvictableIdleTimeMillis());
        this.pool.setSoftMinEvictableIdleTimeMillis(poolConfig.getSoftMinEvictableIdleTimeMillis());
        this.pool.setTimeBetweenEvictionRunsMillis(poolConfig.getTimeBetweenEvictionRunsMillis());
        this.pool.setNumTestsPerEvictionRun(poolConfig.getTestsPerEvictionRun());
        this.pool.setTestOnBorrow(poolConfig.isTestOnBorrow());
        this.pool.setTestOnReturn(poolConfig.isTestOnReturn());
        this.pool.setTestWhileIdle(poolConfig.isTestWhileIdle());

        LOG.info("getPoolConfig(): initializing new connection pool {}", poolConfig);

        this.dataSource = new PoolingDataSource(this.pool);
        this.dataSource.setAccessToUnderlyingConnectionAllowed(true);

        try
        {
            Class.forName(poolConfig.getDriverClassName());
        }
        catch (ClassNotFoundException ex)
        {
            throw new RuntimeException("Driver not found in classpath. Msg = " + ex.getMessage(), ex);
        }

    }

    public Connection getConnection() throws SQLException
    {
        Connection connection = this.dataSource.getConnection();

        boolean exhaustWarning = pool.getNumActive() + EXHAUSTED_POOL_BUFFER_WARNING >= poolConfig.getMaxSize();

        if (exhaustWarning)
        {
            LOG.warn(new LogMessage("Returning connection: approaching max pool size"));
        }
        else
        {
            LOG.debug(new LogMessage("Returning connection"));
        }


        return connection;
    }

    public DbPoolConfig getPoolConfig()
    {
        return poolConfig;
    }

    public int getNumActive()
    {
        return pool.getNumActive();
    }

    public int getNumIdle()
    {
        return pool.getNumIdle();
    }

    /**
     * @return dataSource
     */
    public PoolingDataSource getDataSource()
    {
        return dataSource;
    }

    public class LogMessage extends JsonLog4j2Message
    {

        public LogMessage(String message)
        {
            super(message);
        }

        @Override
        public void doLoadArgs(MessageArgs args)
        {
            super.doLoadArgs(args);

            int idle = pool.getNumIdle();
            int active = pool.getNumActive();

            putArg(FIELD_DB_POOL, poolConfig.getPoolName());
            putArg(FIELD_DB_TOTAL_CONNECTIONS, active + idle);
            putArg(FIELD_DB_ACTIVE_CONNECTION, active);
            putArg(FIELD_DB_IDLE_CONNECTIONS, idle);


        }
    }
}
