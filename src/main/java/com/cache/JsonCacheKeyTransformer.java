package com.cache;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.common.def.ApiSerializationUtil;

public class JsonCacheKeyTransformer<B> extends CacheKeyTransformer<B>
{

    private Set<Class> annotationClasses =
            new HashSet(Arrays.asList(XmlRootElement.class, XmlType.class));

    private ApiSerializationUtil apiSerializationUtil;

    public JsonCacheKeyTransformer()
    {
        this.apiSerializationUtil = new ApiSerializationUtil();
    }

    @Override
    public boolean isSupported(Class<?> type)
    {

        for (Class annotationClass : annotationClasses)
        {
            if (type.getAnnotation(annotationClass) != null)
            {
                return true;
            }
        }

        return false;

    }
    @Override
    public String toString(B bean)
    {
        return apiSerializationUtil.toJson(bean);
    }

    @Override
    public B fromString(String string, Class<B> type)
    {
        try
        {
            return apiSerializationUtil.fromJson(string, type);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
