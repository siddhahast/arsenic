package com.util;

import org.springframework.stereotype.Component;

@Component
public interface ResourceFactory
{

    public static BasicResourceFactory BASIC = new BasicResourceFactory();

    public static SystemPropertiesLoader BASIC_SYSTEM_PROPERTIES_LOADER = new BasicSystemPropetiesLoader();

    public <T> T getResource(String fileName, Class<T> outputClass);

}
