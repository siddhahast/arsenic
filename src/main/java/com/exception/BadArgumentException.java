package com.exception;

public class BadArgumentException extends RuntimeException implements NoStackTraceException
{
    public BadArgumentException()
    {
        super();
    }

    public BadArgumentException(String message)
    {
        super(message);
    }

}
