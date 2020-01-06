package com.cache;

import java.util.Map;

public interface CacheRegistry
{
    public Cache get(String name);

    public void register(String name, Cache cache);

    public Map<String, Cache> getAll();

    public CacheConfig getCacheConfig(Class<? extends Cache> cacheClass);
}
