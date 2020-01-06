package com.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheTraceUtil<K, V> {

    private static final Logger LOG = LoggerFactory.getLogger(CacheTraceUtil.class);

    public static <K,V> void record(Cache<K, V> cache, K key, CacheAction cacheAction)
    {
        LOG.debug(cache.getName(), key.toString(), cacheAction.getSegmentName());
    }
}
