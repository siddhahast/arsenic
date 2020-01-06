package com.cache;


public interface CacheKeyGenerator<K> {
    public Object generateKey(K key);
}