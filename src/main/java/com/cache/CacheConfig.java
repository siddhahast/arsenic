package com.cache;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.context.ApplicationContext;

import com.google.common.cache.Weigher;

public class CacheConfig extends LinkedHashMap<String, Object>
{
    private static final String KEY_CLASS = "keyClass";
    private static final String VALUE_CLASS = "valueClass";
    private static final String EXPIRE_MILLIS = "expireMillis";
    private static final String CLONE = "clone";
    private static final String APPLICATION_CONTEXT = "applicationContext";
    private static final String WEIGHT = "weight";
    private static final String WEIGHER = "weigher";
    private static final String MAX_SIZE = "maxSize";
    private static final String WEAK_KEYS = "weakKeys";
    private static final String WEAK_VALUES = "weakValues";
    private static final String UPDATE_BYPASS_ENABLED = "updateBypassEnabled";
    private static final String READ_UPDATE_BYPASS_ENABLED = "readUpdateBypassEnabled";
    private static final String POST_EVICTION_HANDLERS = "postEvictionHandlers";
    private static final String ENABLED = "enabled";
    private static final String KEY_TRANSFORMERS = "keyTransformers";
    private static final String WRITE_TIMEOUT_MILLIS = "writeTimeoutMillis";
    private static final String READ_TIMEOUT_MILLIS = "readTimeoutMillis";
    private static final String FILE_PATH = "filePath";
    private static final String ALLOW_NULL = "allowNull";
    private static final String BULK_READ_TIMEOUT_MILLIS = "bulkReadTimeoutMillis";

    public CacheConfig()
    {
    }

    public CacheConfig(Map<String, Object> sourceMap)
    {
        super(sourceMap);
    }

    public Class<?> getKeyClass()
    {

        Class<?> keyClass = (Class<?>) get(KEY_CLASS);
        return keyClass;
    }

    public CacheConfig setKeyClass(Class<?> keyClass)
    {
        put(KEY_CLASS, keyClass);
        return this;
    }

    public Class<?> getValueClass()
    {
        Class<?> valueClass = (Class<?>) get(VALUE_CLASS);
        return valueClass;
    }

    public CacheConfig setValueClass(Class<?> valueClass)
    {
        put(VALUE_CLASS, valueClass);
        return this;
    }

    public Long getExpireMillis()
    {
        Long expireMillis = (Long) get(EXPIRE_MILLIS);
        return expireMillis;
    }

    public void setExpiration(String expiration)
    {
        Long millis = PeriodTransformer.MILLIS.transform(expiration);
        setExpireMillis(millis);
    }
    public CacheConfig setExpireMillis(Long expireMillis)
    {
        put(EXPIRE_MILLIS, expireMillis);
        return this;
    }

    public CacheConfig setApplicationContext(ApplicationContext context)
    {
        put(APPLICATION_CONTEXT, context);
        return this;
    }

    public ApplicationContext getApplicationContext()
    {
        return (ApplicationContext) get(APPLICATION_CONTEXT);
    }

    public boolean isClone()
    {
        return Boolean.TRUE.equals(get(CLONE));
    }

    public CacheConfig setClone(boolean clone)
    {
        put(CLONE, clone);
        return this;
    }

    public Long getMaxWeight()
    {
        return (Long) get(WEIGHT);
    }

    public CacheConfig setMaxWeight(Long weight)
    {
        put(WEIGHT, weight);
        return this;
    }

    @SuppressWarnings("rawtypes")
    public Weigher getWeigher()
    {
        return (Weigher) get(WEIGHER);
    }

    @SuppressWarnings("rawtypes")
    public CacheConfig setWeigher(Weigher weigher)
    {
        put(WEIGHER, weigher);
        return this;
    }

    public Long getMaxSize()
    {
        return (Long) get(MAX_SIZE);
    }

    public CacheConfig setMaxSize(Long size)
    {
        put(MAX_SIZE, size);
        return this;
    }

    public Boolean getWeakKeys()
    {
        Boolean weakKeys = (Boolean) get(WEAK_KEYS);

        weakKeys = weakKeys == null ? Boolean.FALSE : weakKeys;

        return weakKeys;
    }

    public CacheConfig setWeakKeys(Boolean weakKeys)
    {
        put(WEAK_KEYS, weakKeys);
        return this;
    }

    public Boolean getWeakValues()
    {
        Boolean weakValues = (Boolean) get(WEAK_VALUES);

        weakValues = weakValues == null ? Boolean.FALSE : weakValues;

        return weakValues;
    }

    public CacheConfig setWeakValues(Boolean weakValues)
    {
        put(WEAK_VALUES, weakValues);
        return this;
    }

    public CacheConfig setEnabled(boolean enabled)
    {
        put(ENABLED, enabled);
        return this;
    }

    public boolean isEnabled()
    {
        Boolean enabled = (Boolean) get(ENABLED);
        return enabled == null ? true : enabled.booleanValue();
    }

    public List<EvictionHandler> getPostEvictionHandlers()
    {
        return (List<EvictionHandler>) get(POST_EVICTION_HANDLERS);
    }

    public CacheConfig setPostEvictionHandlers(List<EvictionHandler> evictionHandlers)
    {
        put(POST_EVICTION_HANDLERS, evictionHandlers);
        return this;
    }

    public List<CacheKeyTransformer> getKeyTransformers()
    {
        return (List<CacheKeyTransformer>) get(KEY_TRANSFORMERS);
    }

    public CacheConfig setKeyTransformers(List<CacheKeyTransformer> keyTransformers)
    {
        put(KEY_TRANSFORMERS, keyTransformers);
        return this;
    }

    public CacheConfig put(String key, String stringValue, Class valueClass)
    {
        try
        {
            Constructor constructor = valueClass.getConstructor(String.class);
            Object value = constructor.newInstance(stringValue);
            put(key, value);
            return this;
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }

    }

    public CacheConfig setUpdateBypassEnabled(boolean updateBypassEnabled)
    {
        put(UPDATE_BYPASS_ENABLED, updateBypassEnabled);

        return this;
    }

    public CacheConfig setReadUpdateBypassEnabled(boolean readUpdateBypassEnabled)
    {
        put(READ_UPDATE_BYPASS_ENABLED, readUpdateBypassEnabled);

        return this;
    }

    public CacheConfig setReadTimeoutMillis(Long readTimeoutMillis)
    {
        put(READ_TIMEOUT_MILLIS, readTimeoutMillis);
        return this;
    }

    public Long getReadTimeoutMillis()
    {
        return (Long) get(READ_TIMEOUT_MILLIS);
    }

    public CacheConfig setWriteTimeoutMillis(Long writeTimeoutMillis)
    {
        put(WRITE_TIMEOUT_MILLIS, writeTimeoutMillis);
        return this;
    }

    public CacheConfig setBulkReadTimeoutMillis(Long bulkReadTimeoutMillis)
    {
        put(BULK_READ_TIMEOUT_MILLIS, bulkReadTimeoutMillis);
        return this;
    }

    public Long getBulkReadTimeoutMillis()
    {
        return (Long) get(BULK_READ_TIMEOUT_MILLIS);
    }

    public Long getWriteTimeoutMillis()
    {
        return (Long) get(WRITE_TIMEOUT_MILLIS);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m)
    {

        Set<java.util.Map.Entry<String, Object>> entries = (Set) m.entrySet();
        for (java.util.Map.Entry<String, Object> entry : entries)
        {
            Object newValue = entry.getValue();
            if (newValue instanceof List)
            {
                newValue = merge((List) newValue, (List) get(entry.getKey()));
            }

            put(entry.getKey(), newValue);
        }
    }
    private List<Object> merge(List<Object> n, List<Object> o)
    {
        List<Object> result = new ArrayList<>(n);

        if (CollectionUtils.isNotEmpty(o))
        {
            result.addAll(o);
        }

        return result;

    }
    public boolean isUpdateBypassEnabled()
    {
        return (boolean) Optional.ofNullable(get(UPDATE_BYPASS_ENABLED)).orElse(false);
    }

    public boolean isReadUpdateBypassEnabled()
    {
        return (boolean) Optional.ofNullable(get(READ_UPDATE_BYPASS_ENABLED)).orElse(true);
    }

    public String getFilePath()
    {
        return (String) get(FILE_PATH);
    }

    public CacheConfig setFilePath(String filePath)
    {
        put(FILE_PATH, filePath);
        return this;
    }

    public boolean isAllowNull()
    {
        return Boolean.TRUE.equals(get(ALLOW_NULL));
    }

    public CacheConfig setAllowNull(boolean allowNull)
    {
        put(ALLOW_NULL, allowNull);
        return this;
    }
}
