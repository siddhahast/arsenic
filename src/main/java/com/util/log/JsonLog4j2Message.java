package com.util.log;

import com.common.def.LoggingConstants;
import org.apache.logging.log4j.message.Message;

public class JsonLog4j2Message extends LoggingConstants implements Message {

    public JsonLog4j2Message(String message)
    {

    }

    public void doLoadArgs()
    {}


    @Override
    public String getFormattedMessage()
    {
        return null;
    }

    @Override
    public String getFormat()
    {
        return null;
    }

    @Override
    public Object[] getParameters()
    {
        return new Object[0];
    }

    @Override
    public Throwable getThrowable()
    {
        return null;
    }
}
