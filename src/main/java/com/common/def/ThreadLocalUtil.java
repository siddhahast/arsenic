package com.common.def;


import java.util.*;
import java.util.function.Function;

import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadLocalUtil
{
    public static ThreadLocal<Map<Object, Object>> THREAD_LOCAL = new ThreadLocal<Map<Object, Object>>();

    static
    {
        THREAD_LOCAL.set(new HashMap<Object, Object>());
    }

    public static <T> T get(Object key)
    {
        Map<Object, Object> map = getAllOriginal();
        Object value = map.get(key);
        return value == null ? null : (T) value;
    }

    @SuppressWarnings("unchecked")
    public static <T> T put(Object key, T value)
    {
        T out = null;
        if (value == null)
        {
            out = (T) getAllOriginal().remove(key);
        }
        else
        {
            out = (T) getAllOriginal().put(key, value);
        }

        if (key instanceof ThreadLocalKeyDef)
        {
            ThreadLocalKeyDef keyDef = (ThreadLocalKeyDef) key;
            if (keyDef.getMdcKey() != null)
            {
                if (value != null)
                {
                    MDC.put(keyDef.getMdcKey(), value);
                }
                else
                {
                    MDC.remove(keyDef.getMdcKey());
                }
            }
        }

        return out;
    }

    @SuppressWarnings("unchecked")
    public static <T> T remove(Object key)
    {
        return (T) put(key, null);
    }

    public static void clear()
    {
        getAllOriginal().clear();
        MDC.clear();
    }

    /**
     * Returns Original Map. This is private to ensure that original map is modified within ThreadLocalUtil only.
     *
     * @return
     */
    private static Map<Object, Object> getAllOriginal()
    {
        Map<Object, Object> map = THREAD_LOCAL.get();

        if (map == null)
        {
            THREAD_LOCAL.set(map = new HashMap<Object, Object>());
        }

        return map;
    }

    /**
     * Returns copy of the map. This is to ensure that original map is modified within ThreadLocalUtil only.
     *
     * @return
     */
    public static Map<Object, Object> getAll()
    {
        Map<Object, Object> map = THREAD_LOCAL.get();

        if (map == null)
        {
            THREAD_LOCAL.set(map = new HashMap<Object, Object>());
        }

        return new HashMap<Object, Object>(map);
    }


    public static <T> List<T> addToList(Object key, T value)
    {
        return (List<T>) addToCollection(key, value, ArrayList.class);
    }

    public static <T> Set<T> addToSet(Object key, T value)
    {
        return (Set<T>) addToCollection(key, value, LinkedHashSet.class);
    }

    public static <S, T> Map<S, T> addToMap(Object key, S mapKey, T value)
    {
        try
        {
            Map<S, T> collectionOfT = get(key);
            if (collectionOfT == null)
            {
                put(key, collectionOfT = new HashMap<S, T>());
            }
            collectionOfT.put(mapKey, value);
            return collectionOfT;
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    public static <T> Collection<T> addToCollection(Object key, T value, Class collectionClass)
    {
        try
        {
            Collection<T> collectionOfT = get(key);
            if (collectionOfT == null)
            {
                put(key, collectionOfT = (Collection<T>) collectionClass.newInstance());
            }
            collectionOfT.add(value);
            return collectionOfT;
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }


    public static <T> T putIfAbsent(Object key, T value)
    {
        T currentValue = get(key);
        if (currentValue == null)
        {
            put(key, value);
        }
        return currentValue;
    }


    public static <R, T> R invoke(Object key, T value, Function<T, R> function)
    {
        T oldValue = put(key, value);

        R result = function.apply(value);

        put(key, oldValue);

        return result;
    }

    public static void setAll(Map<Object, Object> values)
    {
        THREAD_LOCAL.set(values);
    }
}
