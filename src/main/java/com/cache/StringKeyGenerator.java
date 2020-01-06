package com.cache;



public class StringKeyGenerator implements CacheKeyGenerator<Object> {

    @Override
    public Object generateKey(Object key) {
        return String.valueOf(key);
    }
}
