package com.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Weigher;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Weigher;

public class GuavaCache<K, V> extends BaseCache<K, V>
{
    private com.google.common.cache.Cache<K, V> guavaCache;
    private static final Logger LOG = LoggerFactory.getLogger(GuavaCache.class);

    @Override
    protected V _get(K key)
    {
        return guavaCache.getIfPresent(key);
    }

    @Override
    protected void _put(K key, V value)
    {
        guavaCache.put(key, value);
    }

    @Override
    public boolean _evict(K key)
    {
        guavaCache.invalidate(key);
        return true;
    }

    @Override
    protected void _evictAll()
    {
        guavaCache.invalidateAll();
    }

    @Override
    protected Collection<V> _values()
    {
        ConcurrentMap<K, V> cacheMap = guavaCache.asMap();

        Collection<V> values = cacheMap == null ? null : cacheMap.values();

        return values;
    }

    public GuavaCache<K, V> setMaxSize(Long size)
    {
        this.config.setMaxSize(size);
        return this;
    }

    @Override
    protected void _init(ApplicationContext applicationContext)
    {
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();

        Long maxSize = config.getMaxSize();
        Long maxWeight = config.getMaxWeight();
        Weigher weigher = config.getWeigher();
        Long expireMillis = config.getExpireMillis();
        Boolean weakKeys = config.getWeakKeys();
        Boolean weakValues = config.getWeakValues();

        if (maxSize != null)
        {
            cacheBuilder.maximumSize(maxSize);
        }
        else
        {
            LOG.warn("Set a maximum size for the cache");
        }

        if (maxWeight != null)
        {
            cacheBuilder.maximumWeight(maxWeight);
            cacheBuilder.weigher(weigher);
        }

        if (expireMillis != null)
        {
            cacheBuilder.expireAfterWrite(expireMillis, TimeUnit.MILLISECONDS);
        }

        if (weakValues)
        {
            cacheBuilder.weakValues();
        }

        if (weakKeys)
        {
            cacheBuilder.weakKeys();
        }

        guavaCache = cacheBuilder.build();

    }

}
