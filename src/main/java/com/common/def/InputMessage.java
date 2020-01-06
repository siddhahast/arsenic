package com.common.def;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMessage;

import java.io.IOException;
import java.io.InputStream;

public class InputMessage implements HttpInputMessage
{

    public InputMessage(byte[] input)
    {

    }

    @Override
    public HttpHeaders getHeaders() {
        return null;
    }

    @Override
    public InputStream getBody() throws IOException
    {
        return null;
    }
}
