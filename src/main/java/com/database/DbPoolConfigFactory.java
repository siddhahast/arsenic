package com.database;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public interface DbPoolConfigFactory
{

    public DbPoolConfig getDefaultPool();
    public DbPoolConfig getPool(String poolName);
    public List<String> getAllPoolNames();

    public static class Factory
    {
        private static final Logger LOG = LoggerFactory.getLogger(DbPoolConfigFactory.class);
        public static DbPoolConfigFactory INSTANCE = getDbPoolConfigFactory();

        public static DbPoolConfigFactory getDbPoolConfigFactory()
        {
            try
            {
                String factoryClassName = System.getProperty("dbconfig.factory", DbPropertiesConfigFactory.class.getName());
                return (DbPoolConfigFactory) Class.forName(factoryClassName).newInstance();
            }
            catch (Exception ex)
            {
                LOG.error("loadInstance(): Error loading DbPoolConfigFactory. Defaulting to {}", DbPropertiesConfigFactory.class.getName(), ex);
                return new DbPropertiesConfigFactory();
            }
        }
    }
}
