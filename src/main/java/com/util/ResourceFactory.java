package com.util;

import org.springframework.stereotype.Component;

@Component
public interface ResourceFactory
{

    public static BasicResourceFactory BASIC = new BasicResourceFactory();

    public <T> T getResource(String fileName, Class<T> outputClass);

}
