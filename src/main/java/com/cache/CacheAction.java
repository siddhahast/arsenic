package com.cache;

public enum CacheAction
{
    GET_HIT("get"), GET_MISS("get"), PUT("put"), EVICT("evict"), EVICT_ALL("evictAll"), VALUES("values");

    private String segmentName;

    private CacheAction(String segmentName)
    {
        this.segmentName = segmentName;
    }

    public String getSegmentName()
    {
        return segmentName;
    }
}
