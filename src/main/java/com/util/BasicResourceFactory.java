package com.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public class BasicResourceFactory implements ResourceFactory
{

    private static final String WINDOWS_DRIVE_PATTERN = "[A-Z]:/.*";
    private static final String FILE_PROTOCOL = "file://";
    private int connectTimeoutInMillis;
    private int readTimeoutInMillis;
    private String resourceDirectory;

    private Map<Class<?>, Transformer<?,?>> tranformers = new HashMap();

    public BasicResourceFactory()
    {
        this.tranformers.put(InputStream.class, PassthroughTransformer.INSTANCE);
        this.tranformers.put(Properties.class, InputStreamToProperties.INSTANCE);
    }

    public <T> T getResource(String fileName, Class<T> outputClass) {
        T result = null;

        try {
            URL url = loadUrl(fileName);

            if (url == null) {
                throw new FileNotFoundException(fileName + " is not found");
            }

            result = outputClass == URL.class ?
                    (T) url :
                    convert(openStream(url), outputClass);

        }
        catch (FileNotFoundException ex)
        {

        }
        catch (Exception ex)
        {

        }
        return result;
    }

    private URL loadUrl(String fileName) throws IOException
    {
        if (resourceDirectory != null && fileName.startsWith("/")) {
            fileName = resourceDirectory + fileName;
        }

        URL url = BasicResourceFactory.class.getClassLoader().getResource(fileName);
        if (url != null) {
            return url;
        }

        if (fileName.startsWith("/") || Pattern.matches(WINDOWS_DRIVE_PATTERN, fileName)) {
            return new URL("file", null, fileName);
        }

        if (fileName.startsWith(FILE_PROTOCOL)) {
            return new URL("file", null, fileName.substring(FILE_PROTOCOL.length()));
        }

        if (fileName.startsWith("http://") || fileName.startsWith("https://"))
        {
            return new URL(fileName);
        }

        return null;
    }

    private InputStream openStream(URL url) throws IOException
    {
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(connectTimeoutInMillis);
        connection.setReadTimeout(readTimeoutInMillis);
        return connection.getInputStream();
    }

    private <T> T convert(InputStream inputStream, Class<T> outputClass) throws Exception
    {

        if(inputStream == null)
        {
            return null;
        }

        Transformer<InputStream, ?> transformer = (Transformer<InputStream, ?>) tranformers.get(outputClass);

        if(transformer == null)
        {
            throw new RuntimeException();
        }
        return (T) transformer.transform(inputStream);
    }

}
