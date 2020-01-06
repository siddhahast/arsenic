package com.common.def;
import java.io.IOException;
import java.util.Set;

import com.http.MessageConverterFactory;
import com.http.OutputMessage;
import com.http.SchemaVersionSupportHttpMessageConverter;
import org.springframework.http.MediaType;

public class ApiSerializationUtil
{
    private SchemaVersionSupportHttpMessageConverter converter;

    public ApiSerializationUtil()
    {
        converter = MessageConverterFactory.getDefaultConverter();
    }

    public ApiSerializationUtil(Set<String> addlContextRoots)
    {
        addlContextRoots.addAll(SchemaVersionSupportHttpMessageConverter.DEFAULT_CONTEXT_ROOTS);
        converter = MessageConverterFactory.newCustomConverter(null, addlContextRoots);
    }

    public void setConverter(SchemaVersionSupportHttpMessageConverter converter)
    {
        this.converter = converter;
    }

    public String toJson(Object data)
    {
        return toMediaType(data, MediaType.APPLICATION_JSON);
    }

    public String toXml(Object data)
    {
        return toMediaType(data, MediaType.APPLICATION_XML);
    }

    private String toMediaType(Object data, MediaType mediaType)
    {
        ApiVersion version = ApiVersion.fromClass(data.getClass());

        OutputMessage outputMessage = new OutputMessage();

        try
        {
            converter.write(version, data, mediaType, outputMessage);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return outputMessage.getBodyAsString().trim();
    }

    public <A> A fromJson(String json, Class<A> objectType)
    {
        return fromMediaType(json, MediaType.APPLICATION_JSON, objectType);
    }

    public <A> A fromXml(String xml, Class<A> objectType)
    {
        return fromMediaType(xml, MediaType.APPLICATION_XML, objectType);
    }

    public <A extends ServiceData> A fromXml(String xml, ApiVersion apiVersion)
    {
        InputMessage inputMessage = new InputMessage(xml.getBytes());
        inputMessage.getHeaders().setContentType(MediaType.APPLICATION_XML);

        try
        {
            return (A) converter.read(apiVersion, inputMessage);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <A> A fromMediaType(String media, MediaType mediaType, Class<A> objectType)
    {
        InputMessage inputMessage = new InputMessage(media.getBytes());
        inputMessage.getHeaders().setContentType(mediaType);

        try
        {
            return (A) converter.read(objectType, inputMessage);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
