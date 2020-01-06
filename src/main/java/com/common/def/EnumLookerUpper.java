package com.common.def;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EnumLookerUpper<E extends Enum<?>>
{
    private Map<String, E> lookup;
    private boolean allowNull = false;
    private Set<String> fieldNames;
    private Class<E> enumClass;

    public EnumLookerUpper(Class<E> enumClass)
    {
        init(enumClass, (String[]) null);
    }

    public EnumLookerUpper(Class<E> enumClass, String... fieldNames)
    {
        init(enumClass, fieldNames);
    }

    public void init(Class<E> enumClass, String... fieldNames)
    {

        if (!enumClass.isEnum())
        {
            throw new RuntimeException("class " + enumClass + " is not an enum");
        }

        this.enumClass = enumClass;
        this.lookup = new HashMap<String, E>();

        if (fieldNames != null)
        {
            this.fieldNames = new HashSet<String>(Arrays.asList(fieldNames));
        }

        E[] enumConstants = (E[]) enumClass.getEnumConstants();

        for (E e : enumConstants)
        {
            lookup.put(e.name(), e);
        }

        Method[] methods = enumClass.getDeclaredMethods();

        for (Method method : methods)
        {
            if (isValidReadMethod(method))
            {
                for (E e : enumConstants)
                {
                    try
                    {
                        Object key = method.invoke(e, (Object[]) null);
                        if (key != null)
                        {
                            lookup.put(key.toString(), e);
                        }

                    }
                    catch (Exception ex)
                    {

                    }
                }
            }
        }
    }
    private boolean isValidReadMethod(Method method)
    {
        if (Modifier.isStatic(method.getModifiers()))
        {
            return false;
        }

        if (method.getReturnType() == null)
        {
            return false;
        }

        String fieldName = toFieldName(method);

        if (fieldName == null)
        {
            return false;
        }
        else if (fieldNames != null)
        {
            return fieldNames.contains(fieldName);
        }
        else
        {
            return true;
        }

    }

    private static String toFieldName(Method method)
    {
        String methodName = method.getName();
        if (methodName.startsWith("get"))
        {
            char[] charArray = methodName.substring("get".length()).toCharArray();
            charArray[0] = Character.toLowerCase(charArray[0]);
            return new String(charArray);
        }
        else if (methodName.startsWith("is"))
        {
            char[] charArray = methodName.substring("is".length()).toCharArray();
            charArray[0] = Character.toLowerCase(charArray[0]);
            return new String(charArray);
        }

        return null;
    }

    public E lookup(String key)
    {
        E e = lookup.get(key);
        if (e != null)
        {
            return e;
        }
        else if (allowNull)
        {
            return null;
        }
        else
        {
            throw new IllegalArgumentException("invalid key " + key + " on " + enumClass);
        }

    }
    public boolean isAllowNull()
    {
        return allowNull;
    }

    public void setAllowNull(boolean allowNull)
    {
        this.allowNull = allowNull;
    }

    public Set<String> getFieldNames()
    {
        return fieldNames;
    }

    public void setFieldNames(Set<String> fieldNames)
    {
        this.fieldNames = fieldNames;
    }

    public void setLookup(Map<String, E> lookup)
    {
        this.lookup = lookup;
    }

    public void put(String key, E value)
    {
        this.lookup.put(key, value);
    }
}
