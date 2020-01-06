package com.cache;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;


public class CacheRegistryImpl implements CacheRegistry
{

    private static final Logger LOG = LoggerFactory.getLogger(CacheRegistryImpl.class);

    @Autowired
    private ApplicationContext applicationContext;

    @SuppressWarnings("rawtypes")
    private final Map<String, Cache> cacheRegistry;

    @SuppressWarnings("rawtypes")
    private Map<Class<Cache>, CacheConfig> configRegistry;

    public CacheConfig getConfigByCacheType(Class<Cache> cacheClass)
    {
        CacheConfig cacheConfig = configRegistry.get(cacheClass);
        if (cacheConfig != null)
        {
            cacheConfig.setApplicationContext(applicationContext);
        }
        return cacheConfig;
    }

    public CacheRegistryImpl()
    {
        cacheRegistry = new HashMap<>();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Cache get(String name)
    {
        Cache cache = null;

        cache = cacheRegistry.get(name);

        return cache;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Map<String, Cache> getAll()
    {
        return cacheRegistry;
    }

    @Override
    public void register(String name, Cache cache)
    {
        Cache oldValue = this.cacheRegistry.put(name, cache);

        if (oldValue != null)
        {
            LOG.warn("register() {} is already registered with {}", name, cache);
        }
    }

    @Override
    public CacheConfig getCacheConfig(Class<? extends Cache> cacheClass)
    {
        CacheConfig cacheConfig = configRegistry.get(cacheClass);

        if (cacheConfig == null)
        {
            cacheConfig = new CacheConfig();
        }

        cacheConfig.setApplicationContext(applicationContext);
        return cacheConfig;
    }

    public Map<Class<Cache>, CacheConfig> getConfigRegistry()
    {
        return configRegistry;
    }

    public void setConfigRegistry(Map<Class<Cache>, CacheConfig> configRegistry)
    {
        this.configRegistry = configRegistry;
    }
}
