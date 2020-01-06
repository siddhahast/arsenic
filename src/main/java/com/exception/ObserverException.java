package com.exception;

public class ObserverException extends RuntimeException
{

    public ObserverException(String message, Throwable ex)
    {
        super(message, ex);
    }
}
