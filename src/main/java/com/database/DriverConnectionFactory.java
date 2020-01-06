package com.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.lang.StringUtils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DriverConnectionFactory implements ConnectionFactory, Configurable {
    private static final Logger LOG = LoggerFactory.getLogger(DriverConnectionFactory.class);

    public static final String CONNECTION_PROPERTY_PREFIX = "connection.";

    private DbPoolConfig poolConfig;

    public Properties getConnectionProperties() {

        Properties connectionProperties = new Properties();
        connectionProperties.put("user", poolConfig.getUser());
        connectionProperties.put("password", poolConfig.getPassword());
        connectionProperties.put("useDynamicCharsetInfo", "false"); // mysql default to prevent  SHOW ALL COLUMNS in CachedRowsetImpl

        if (poolConfig.getProperties() != null) {
            for (Entry<Object, Object> entry : poolConfig.getProperties().entrySet()) {
                String key = entry.getKey().toString();
                if (key.startsWith(CONNECTION_PROPERTY_PREFIX)) {
                    connectionProperties.put(
                            StringUtils.substringAfter(key, CONNECTION_PROPERTY_PREFIX),
                            entry.getValue().toString());
                }
            }
        }

        return connectionProperties;
    }

    public Connection createConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(poolConfig.getConnectionString(), getConnectionProperties());

        LOG.debug("createConnection(): Creating jdbc connection {}", connection);

        return connection;
    }

    public DbPoolConfig getPoolConfig() {
        return poolConfig;
    }

    public void setPoolConfig(DbPoolConfig poolConfig) {
        this.poolConfig = poolConfig;
    }

}