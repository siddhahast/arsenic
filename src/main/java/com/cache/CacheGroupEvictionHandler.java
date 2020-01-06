package com.cache;

import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


public class CacheGroupEvictionHandler implements EvictionHandler
{
    private static final Logger LOG = LoggerFactory.getLogger(CacheGroupEvictionHandler.class);

    @Autowired
    private CacheRegistry cacheRegistry;

    private Set<String> cacheNames;

    @Override
    public void evict(CacheEvictRequest evictRequest)
    {
        // currently only works for evict all requests
        if (evictRequest.isAll())
        {
            for (String cacheName : cacheNames)
            {
                if (!evictRequest.getName().equals(cacheName))
                {
                    Cache cache = cacheRegistry.get(cacheName);
                    if (cache == null)
                    {
                        LOG.warn("evicted(): cacheGroupEvictionHandler is configured with a bad cacheName {}. Skipping post eviction.", cacheName);
                    }
                    else if (cache instanceof BaseCache)
                    {
                        BaseCache baseCache = (BaseCache) cache;
                        CacheEvictRequest cacheEvictRequest = new CacheEvictRequest();
                        cacheEvictRequest.setName(evictRequest.getName());
                        cacheEvictRequest.setPostEvict(false);
                        cacheEvictRequest.setAll(true);

                        baseCache.managerEvict(evictRequest);

                    }
                }
            }

        }
    }
    public Set<String> getCacheNames()
    {
        return cacheNames;
    }

    public void setCacheNames(Set<String> cacheNames)
    {
        this.cacheNames = cacheNames;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this).append("cacheNames", cacheNames).toString();
    }
}
