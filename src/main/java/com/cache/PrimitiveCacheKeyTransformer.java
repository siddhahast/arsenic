package com.cache;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PrimitiveCacheKeyTransformer<B> extends CacheKeyTransformer<B>
{

    private Set<Class<?>> supportedTypes = new HashSet<Class<?>>(Arrays.asList(
            String.class,
            Long.class,
            Integer.class,
            Short.class
    ));

    @Override
    public boolean isSupported(Class<?> type)
    {
        return supportedTypes.contains(type);
    }

    @Override
    public String toString(B bean)
    {
        return bean.toString();
    }

    @Override
    public B fromString(String string, Class<B> type)
    {
        try
        {
            return type.getConstructor(String.class).newInstance(string);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

}
