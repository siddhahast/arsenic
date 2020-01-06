package com.cache;

public abstract class CacheKeyTransformer<K>
{
    public abstract boolean isSupported(Class<?> type);
    public abstract String toString(K key);
    public abstract K fromString(String string, Class<K> keyClass);
}
