package com.http;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;

public class DelegatingHttpInputMessage implements HttpInputMessage
{
    private HttpInputMessage httpInputMessage;
    private Class<?> trueTargetType;

    public DelegatingHttpInputMessage() {}

    public DelegatingHttpInputMessage(HttpInputMessage message)
    {
        this.httpInputMessage = message;
    }

    @Override
    public HttpHeaders getHeaders()
    {
        return httpInputMessage.getHeaders();
    }

    @Override
    public InputStream getBody() throws IOException
    {
        return httpInputMessage.getBody();
    }

    public Class<?> getTrueTargetType()
    {
        return trueTargetType;
    }

    public void setTrueTargetType(Class<?> trueTargetType)
    {
        this.trueTargetType = trueTargetType;
    }
}
