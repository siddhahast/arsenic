package com.cache;

public class CachedValue<V>
{
    private V value;
    private long cachedMillis;

    public CachedValue()
    {
    }

    public CachedValue(V value)
    {
        this.value = value;
        this.cachedMillis = System.currentTimeMillis();
    }

    public V getValue()
    {
        return value;
    }

    public void setValue(V value)
    {
        this.value = value;
    }

    public long getCachedMillis()
    {
        return cachedMillis;
    }

    public void setCachedMillis(long cachedMillis)
    {
        this.cachedMillis = cachedMillis;
    }

}
