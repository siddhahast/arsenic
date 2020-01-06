package com.cache;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.ass.BaseAssert;
import com.common.def.HttpOperationType;
import com.common.def.ThreadLocalKey;
import com.common.def.YesNo;
import com.util.SerializationCloneUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.google.common.collect.Sets;

public abstract class BaseCache<K, V> implements Cache<K, V>
{
    private static final Logger LOG = LoggerFactory.getLogger(BaseCache.class);

    @Autowired
    private ApplicationContext applicationContext;

    protected String name;

    protected Date lastEvicted;

    protected CacheConfig config;

    protected EvictionUtil<K, V> evictionUtil;

    protected Function<K, V> singleKeyCacheLoader;

    protected Function<Void, Map<K, V>> multipleKeyCacheLoader;

    protected abstract void _init(ApplicationContext applicationContext);
    protected abstract V _get(K key);

    protected Map<K, V> _get(Set<K> keys) {
        ConcurrentHashMap<K, V> results = new ConcurrentHashMap<>();

        for (K key : keys) {

            V value = _get(key);

            if (value != null) {
                results.put(key, value);
            }

        }

        if (config.isClone()) {
            results = SerializationCloneUtils.clone(results);
        }

        return results;
    }

    protected abstract void _put(K key, V value);
    protected abstract boolean _evict(K key);
    protected abstract void _evictAll();
    protected abstract Collection<V> _values();

    private boolean initCalled = false;

    private CacheStats stats;

    public BaseCache()
    {
        this.config = new CacheConfig();
        this.lastEvicted = new Date();
        this.stats = new CacheStats();
    }

    @Override
    public V get(K key)
    {
        V result = null;
        if (isReadEnabled())
        {
            result = _get(key);
            CacheTraceUtil.record(this, key, result == null ? CacheAction.GET_MISS : CacheAction.GET_HIT);
        }

        return result;

    }

    @Override
    public BulkGetResponse<K, V> getBulk(Set<K> keys)
    {
        BulkGetResponse<K, V> bulkGetResponse = new BulkGetResponse<>();

        if (isReadEnabled()) {
            Map<K, V> results = _get(keys);
            Set<K> notFoundKeys = keys.stream().filter(key -> !results.containsKey(key)).collect(Collectors.toSet());

            bulkGetResponse.setResults(results);
            bulkGetResponse.setNotFoundKeys(notFoundKeys);

            keys.stream().forEach(key -> CacheTraceUtil
                    .record(this,
                            key,
                            notFoundKeys.contains(key) ? CacheAction.GET_MISS : CacheAction.GET_HIT));
        } else {
            bulkGetResponse.setNotFoundKeys(keys);
        }

        return bulkGetResponse;

    }


    private boolean isReadEnabled()
    {
        //if disabled at config level or from runtime lead variable, return false
        if (!isEnabled() || isBypassCache())
        {
            return false;
        }

        if (isCacheEnabledRequest())
        {
            return true;
        }

        // else let the config tell us if it's safe to read from cache during write requests
        return config.isReadUpdateBypassEnabled();
    }

    protected boolean isBypassCache()
    {
        boolean bypassCache = ((YesNo) Optional.ofNullable(ThreadLocalKey.BYPASS_CACHE.get()).orElse(YesNo.N)).booleanValue();

        if (bypassCache)
        {
            Set<String> cacheKeys = ThreadLocalKey.CACHE_KEYS.get() != null ? //
                    Sets.newHashSet((String[]) ThreadLocalKey.CACHE_KEYS.get())
                    : null;

            //if cacheKeys not specified, use default 'bypassCache' value
            if (cacheKeys != null)
            {
                bypassCache = cacheKeys.contains(this.name);
            }
        }

        //if cacheKeys is not null but does not have the cache name in it then use default 'bypassCache' value
        return bypassCache;
    }
    private boolean isUpdateEnabled()
    {
        // we don't want update requests to be updating cache
        if (!isEnabled())
        {
            // disabled for all
            return false;
        }

        if (isCacheEnabledRequest())
        {
            return true;
        }

        return config.isUpdateBypassEnabled();
    }

    @Override
    public void put(K key, V value)
    {
        if (isUpdateEnabled())
        {
            _put(key, value);
            CacheTraceUtil.record(this, key, CacheAction.PUT);
        }
    }

    @Override
    public void putAll(Map<K, V> map)
    {
        if (isUpdateEnabled() && map != null)
        {
            Set<Entry<K, V>> entrySet = map.entrySet();
            for (Entry<K, V> entry : entrySet)
            {
                _put(entry.getKey(), entry.getValue());
                CacheTraceUtil.record(this, entry.getKey(), CacheAction.PUT);
            }
        }

    }
    @Override
    public void evictAll()
    {
        processEvictAll();
        CacheTraceUtil.record(this, null, CacheAction.EVICT_ALL);
        lastEvicted = new Date();
        evictionUtil.postEvictAll();

    }

    public void managerEvict(CacheEvictRequest evictRequest)
    {
        try
        {
            if (evictionUtil.isThisMyUUID(evictRequest.getUuid()))
            {
                LOG.debug("managerEvict(): {} is my uuid, not evicting", evictRequest.getUuid());
                return;
            }

            boolean isPostEvict = evictRequest.isPostEvict();

            if (evictRequest.isAll())
            {
                // evict all request
                processEvictAll();
                CacheTraceUtil.record(this, null, CacheAction.EVICT_ALL);
                lastEvicted = new Date();
                if (isPostEvict)
                {
                    evictionUtil.postEvictAll();
                }
            }
            else if (!CollectionUtils.isEmpty(evictRequest.getKeys()))
            {
                // specific key eviction, need to know the keyClass
                Class keyClass = null;

                if (evictRequest.getKeyClass() != null)
                {
                    keyClass = Class.forName(evictRequest.getKeyClass());
                }
                else if (config.getKeyClass() != null)
                {
                    keyClass = config.getKeyClass();
                }
                else
                {
                    throw new RuntimeException("key class is not configured nor provided during the evict");
                }

                List<K> keys = evictionUtil.keysFromString(evictRequest.getKeys(), keyClass);

                if (!CollectionUtils.isEmpty(keys))
                {
                    for (K key : keys)
                    {
                        processEvict(key);
                        CacheTraceUtil.record(this, key, CacheAction.EVICT);
                    }
                }

                if (isPostEvict)
                {
                    evictionUtil.postEvictKeys(keys);
                }

            }

        }
        catch (Exception ex)
        {
            LOG.error("managerEvict(): Error evicting {}", evictRequest, ex);
        }

    }
    @Override
    public boolean evict(K... keys)
    {
        boolean success = true;

        for (K key : keys)
        {
            boolean singleEvictionSuccess = processEvict(key);

            if (!singleEvictionSuccess) {
                success = false;
            }

            CacheTraceUtil.record(this, key, CacheAction.EVICT);
        }
        evictionUtil.postEvictKeys(keys);

        return success;
    }

    @Override
    public Collection<V> values()
    {
        Collection<V> values = null;
        if (isReadEnabled())
        {
            values = _values();
            CacheTraceUtil.record(this, null, CacheAction.VALUES);
        }

        return values;
    }

    @Override
    public Date getLastEvicted()
    {
        return lastEvicted;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public BaseCache<K, V> setName(String name)
    {
        this.name = name;
        return this;
    }

    public BaseCache<K, V> init(ApplicationContext applicationContext)
    {
        CacheRegistry cacheRegistry = applicationContext.getBean(CacheRegistry.class);

        // init config
        // local config takes priority over default config
        CacheConfig tempConfig = new CacheConfig();

        CacheConfig defaultConfig = null;

        if (cacheRegistry != null)
        {
            // cacheRegistry is optional
            defaultConfig = cacheRegistry.getCacheConfig(getClass());
        }

        if (defaultConfig != null)
        {
            tempConfig.putAll(defaultConfig);
        }

        if (config != null)
        {
            tempConfig.putAll(config);
        }

        this.config = tempConfig;

        // figure out the name for the cache
        // default name will be generated if one is not provided
        String registerName = null;
        if (this.name != null)
        {
            registerName = this.name;
        }
        else
        {
            Set<Entry<String, Cache>> cacheEntrySet = config.getApplicationContext().getBeansOfType(Cache.class).entrySet();
            for (Entry<String, Cache> cacheEntry : cacheEntrySet)
            {
                if (cacheEntry.getValue().equals(this))
                {
                    this.name = cacheEntry.getKey();
                    registerName = cacheEntry.getKey();
                    break;
                }
            }
        }

        if (registerName == null)
        {
            this.name = registerName = getClass().getName() + "@" + Integer.toHexString(hashCode()) + "-DYNAMIC";
            LOG.warn("init(): name not configured, generated a random name {}", this);
        }

        evictionUtil = new EvictionUtil<>(name, config);

        if (cacheRegistry != null)
        {
            cacheRegistry.register(registerName, this);
        }

        _init(applicationContext);

        if(multipleKeyCacheLoader != null)
        {
            loadCache(multipleKeyCacheLoader.apply(null));
        }

        initCalled = true;
        return this;

    }

    private void loadCache(Map<K, V> cacheValues)
    {
        if(MapUtils.isNotEmpty(cacheValues))
        {
            for(Entry<K, V> entry : cacheValues.entrySet())
            {
                this._put(entry.getKey(), entry.getValue());
            }
        }
    }

    @PostConstruct
    private BaseCache<K, V> init()
    {
        return init(applicationContext);
    }

    public CacheConfig getConfig()
    {
        return config;
    }

    public BaseCache<K, V> setConfig(CacheConfig config)
    {
        this.config = config;
        return this;
    }

    public boolean isEnabled()
    {
        boolean enabled = config.isEnabled();

        if (enabled && !initCalled)
        {
            throw new RuntimeException("init() has not been called for [" + name + "] cache");
        }

        return enabled;

    }
    public BaseCache<K, V> setExpiration(String expiration)
    {
        config.setExpiration(expiration);
        return this;
    }

    public BaseCache<K, V> setClone(boolean clone)
    {
        config.setClone(clone);
        return this;
    }

    @Override
    public CacheStats getStats()
    {
        return stats;
    }

    private void processEvictAll()
    {
        Map<K, V> cacheValues = null;
        if(multipleKeyCacheLoader != null)
        {
            cacheValues = multipleKeyCacheLoader.apply(null);

            BaseAssert.isNotNull(cacheValues, "CacheLoader returned null cache values");
            BaseAssert.isNotEmpty(cacheValues.values(), "CacheLoader returned empty cache values");
        }
        if (MapUtils.isNotEmpty(cacheValues))
        {
            _evictAll();
            loadCache(cacheValues);
        }
        else
        {
            _evictAll();
        }
    }

    private boolean processEvict(K key)
    {
        V value = null;
        boolean singleEvictionSuccess = false;
        if(singleKeyCacheLoader != null)
        {
            value = singleKeyCacheLoader.apply(key);
        }
        if(value != null)
        {
            singleEvictionSuccess = _evict(key);
            this._put(key, value);
        }
        else
        {
            singleEvictionSuccess = _evict(key);
        }

        return singleEvictionSuccess;
    }

    public Function<K, V> getSingleKeyCacheLoader()
    {
        return singleKeyCacheLoader;
    }
    public void setSingleKeyCacheLoader(Function<K, V> singleKeyCacheLoader)
    {
        this.singleKeyCacheLoader = singleKeyCacheLoader;
    }
    public Function<Void, Map<K, V>> getMultipleKeyCacheLoader()
    {
        return multipleKeyCacheLoader;
    }
    public void setMultipleKeyCacheLoader(Function<Void, Map<K, V>> multipleKeyCacheLoader)
    {
        this.multipleKeyCacheLoader = multipleKeyCacheLoader;
    }

    private boolean isCacheEnabledRequest() {
        return ThreadLocalKey.HTTP_REQUEST_OPERATION_TYPE.get() == null ||
                ThreadLocalKey.HTTP_REQUEST_OPERATION_TYPE.get() == HttpOperationType.READ;
    }
}
