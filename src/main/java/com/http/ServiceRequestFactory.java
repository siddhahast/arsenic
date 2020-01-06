package com.http;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.io.IOException;
import java.net.URI;

public class ServiceRequestFactory implements ClientHttpRequestFactory
{

    public static ServiceRequestFactory INSTANCE = null;

    public static ServiceRequestFactory getInstance()
    {
        if(INSTANCE == null)
        {
            INSTANCE = new ServiceRequestFactory();
        }
        return INSTANCE;
    }

    private ServiceRequestFactory()
    {

    }

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException
    {
        return null;
    }
}
