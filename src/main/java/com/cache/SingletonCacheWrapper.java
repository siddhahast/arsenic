package com.cache;

import java.util.Optional;

public class SingletonCacheWrapper<V>
{
    private Cache<SingletonCacheWrapper, V> cache;
    private String name;

    public V get()
    {
        return cache.get(this);
    }

    public void put(V value)
    {
        cache.put(this, value);
    }

    public void evictAll()
    {
        cache.evictAll();
    }

    public Cache<SingletonCacheWrapper, V> getCache()
    {
        return cache;
    }

    public void setCache(Cache<SingletonCacheWrapper, V> cache)
    {
        this.cache = cache;
    }

    @Override
    public String toString()
    {
        String string = name;

        if (string == null && cache != null)
        {
            string = cache.getName();
        }

        return Optional.ofNullable(string).orElse(super.toString());
    }

}
