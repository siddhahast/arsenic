package com.http;

public class MessageConverterFactory
{

    public static SchemaVersionSupportHttpMessageConverter getDefaultConverter()
    {
        return new SchemaVersionSupportHttpMessageConverter();
    }
}
