package com.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class InputStreamToProperties implements Transformer<InputStream, Properties>
{

    public static final InputStreamToProperties INSTANCE = new InputStreamToProperties();

    public Properties transform(InputStream input) throws TransformException
    {
        try {
            if (input == null) {
                return null;
            }

            Properties properties = new Properties();
            properties.load(input);
            return properties;
        }
        catch (TransformException tex)
        {

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
