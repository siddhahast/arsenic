package com.util;

public class PassthroughTransformer implements Transformer<Object, Object>
{
    public static PassthroughTransformer INSTANCE = new PassthroughTransformer();

    public Object transform(Object input)
    {
        return input;
    }
}
