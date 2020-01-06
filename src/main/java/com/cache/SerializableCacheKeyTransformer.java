package com.cache;

import com.util.SerializationCloneUtils;

import java.io.Serializable;


public class SerializableCacheKeyTransformer<B extends Serializable> extends CacheKeyTransformer<B>
{

    @Override
    public boolean isSupported(Class<?> type)
    {
        return Serializable.class.isAssignableFrom(type);
    }

    @Override
    public String toString(B bean)
    {
        try
        {
            return SerializationCloneUtils.serializeBase64(bean);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public B fromString(String string, Class<B> type)
    {
        return SerializationCloneUtils.deserializeBase64(string);
    }
}
