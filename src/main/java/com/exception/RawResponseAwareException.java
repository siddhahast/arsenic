package com.exception;

import java.io.InputStream;

public class RawResponseAwareException extends RuntimeException
{

    private InputStream rawInputStream;

    public RawResponseAwareException(Exception ex, InputStream message)
    {
        super(ex);
        this.rawInputStream = message;
    }

    public String getRawResponseAsStream()
    {
        String result = null;

        try
        {

        }
        catch(Exception ex)
        {
            result = ex.getMessage();
        }

        return result;
    }

    public void setRawInputStream(InputStream rawInputStream) {
        this.rawInputStream = rawInputStream;
    }
}
