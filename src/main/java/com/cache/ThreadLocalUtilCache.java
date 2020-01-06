package com.cache;

import com.common.def.ThreadLocalUtil;
import com.util.SerializationCloneUtils;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ThreadLocalUtilCache<K, V> extends BaseCache<K, V>
{
    private static final CacheKeyGenerator<Object> DEFAULT_KEY_GENERATOR = new StringKeyGenerator();

    private CacheKeyGenerator<Object> keyGenerator = DEFAULT_KEY_GENERATOR;
    private Boolean clone;
    private Boolean allowNull;

    public void setKeyGenerator(CacheKeyGenerator<Object> keyGenerator)
    {
        this.keyGenerator = keyGenerator;
    }

    protected Object generateKey(K key)
    {
        return (keyGenerator != null) ? keyGenerator.generateKey(key) : key;
    }

    @Override
    protected V _get(K key)
    {
        CachedValue<V> value = null;

        Map<Object, CachedValue<V>> cacheMap = ThreadLocalUtil.get(name);

        if (cacheMap != null)
        {
            Object generateKey = generateKey(key);
            value = cacheMap.get(generateKey);

            Long expireMillis = config.getExpireMillis();

            if (value != null && expireMillis != null && System.currentTimeMillis() - value.getCachedMillis() > expireMillis)
            {
                value = null;
                _evict(key);
            }
        }

        if (value != null && value.getValue() != null && clone)
        {
            value = new CachedValue<V>(cloneObject(value.getValue()));
        }

        return value != null ? value.getValue() : null;
    }

    @SuppressWarnings("unchecked")
    private V cloneObject(V value)
    {
        V clone = null;

        if (value instanceof Serializable)
        {
            clone = (V) SerializationCloneUtils.clone((Serializable) value);
        }
        else
        {
            throw new RuntimeException("Clone requested, but cache value is not serializable.");
        }

        return clone;
    }

    @Override
    protected void _put(K key, V value)
    {
        Map<Object, CachedValue<V>> cacheMap = ThreadLocalUtil.get(name);

        if (cacheMap == null)
        {

            cacheMap = new HashMap<Object, CachedValue<V>>();
            ThreadLocalUtil.put(name, cacheMap);
        }

        V valueToCache = value;

        if (clone != null && clone && value != null)
        {
            valueToCache = cloneObject(value);
        }

        if(valueToCache != null || (allowNull != null && allowNull))
        {
            cacheMap.put(generateKey(key), new CachedValue<>(valueToCache));
        }

    }

    @Override
    public boolean _evict(K key)
    {
        Map<Object, CachedValue<V>> cacheMap = ThreadLocalUtil.get(name);

        if (cacheMap != null)
        {
            cacheMap.remove(generateKey(key));
        }

        return true;
    }

    @Override
    protected void _evictAll()
    {
        ThreadLocalUtil.remove(name);
    }

    @Override
    protected Collection<V> _values()
    {
        Collection<V> values = new ArrayList<>();

        Map<Object, V> cacheMap = ThreadLocalUtil.get(name);
        if (cacheMap != null)
        {
            values.addAll(cacheMap.values());
        }
        return values;
    }

    @Override
    protected void _init(ApplicationContext applicationContext)
    {
        if (this.clone == null)
        {
            this.clone = config.isClone();

            if (this.clone == null)
            {
                this.clone = Boolean.FALSE;
            }
        }
        // set default False for allowNull
        if(this.allowNull == null)
        {
            this.allowNull = config.isAllowNull();

            if (this.allowNull == null)
            {
                this.allowNull = Boolean.FALSE;
            }
        }
    }

    public Boolean getClone()
    {
        return clone;
    }

    public void setClone(Boolean clone)
    {
        this.clone = clone;
    }

    public Boolean getAllowNull() {
        return allowNull;
    }

    public void setAllowNull(Boolean allowNull) {
        this.allowNull = allowNull;
    }
}
