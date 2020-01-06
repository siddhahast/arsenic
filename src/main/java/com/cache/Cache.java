package com.cache;


import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public interface Cache<K, V>
{

    public V get(K key);

    public BulkGetResponse<K, V> getBulk(Set<K> keys);

    public void put(K key, V value);

    public void putAll(Map<K, V> map);

    public boolean evict(K... keys);

    public void evictAll();

    public Collection<V> values();

    public String getName();

    public Date getLastEvicted();

    public CacheStats getStats();

}