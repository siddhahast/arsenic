package com.cache;


public interface EvictionHandler
{
    public void evict(CacheEvictRequest cacheEvictRequest);
}
