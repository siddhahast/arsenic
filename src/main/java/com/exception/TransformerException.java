package com.exception;

public class TransformerException extends RuntimeException
{

    private Object input;

    public TransformerException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public Object getInput() {
        return input;
    }

    public TransformerException setInput(Object input)
    {
        this.input = input;
        return this;
    }
}
