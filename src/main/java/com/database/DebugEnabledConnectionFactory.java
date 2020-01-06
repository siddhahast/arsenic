package com.database;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

public class DebugEnabledConnectionFactory extends PoolableConnectionFactory
{

    public DebugEnabledConnectionFactory(
        ConnectionFactory connectionFactory,
        GenericObjectPool pool,
        KeyedObjectPoolFactory statementPool,
        String validationQuery,
        int validationQueryInSec,
        boolean isReadOnly,
        boolean isAutoCommit)
    {
        super(connectionFactory, pool, statementPool, validationQuery, validationQueryInSec, isReadOnly, isAutoCommit);
    }
}
