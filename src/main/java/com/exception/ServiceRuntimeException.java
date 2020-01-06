package com.exception;

import com.dibs.service.common.ServiceError;
import com.dibs.service.common.ServiceResponse;
import com.dibs.service.common.response.ServiceResponseCode;
import com.dibs.service.common.response.StatusCode;

public class ServiceRuntimeException extends RuntimeException implements NoStackTraceException
{
    private StatusCode httpCode;
    private ServiceResponseCode serviceResponseCode;
    private ServiceError error;

    public ServiceRuntimeException(String message)
    {
        super(message);
    }

    public ServiceRuntimeException(String messsage, StatusCode httpCode)
    {
        super(messsage);
        this.httpCode = httpCode;
    }

    public ServiceRuntimeException(String messsage, StatusCode httpCode, ServiceResponseCode serviceResponseCode)
    {
        super(messsage);
        this.httpCode = httpCode;
        this.serviceResponseCode = serviceResponseCode;
    }

    public ServiceRuntimeException(String messsage, StatusCode httpCode, ServiceError serviceError)
    {
        super(messsage);
        this.httpCode = httpCode;
        this.error = serviceError;
    }

    public ServiceRuntimeException(String messsage, StatusCode httpCode, ServiceResponseCode serviceResponseCode, ServiceError serviceError)
    {
        super(messsage);
        this.serviceResponseCode = serviceResponseCode;
        this.httpCode = httpCode;
        this.error = serviceError;
    }

    public ServiceRuntimeException(ServiceResponse<?> serviceResponse)
    {
        this(null, serviceResponse);
    }

    public ServiceRuntimeException(String message, ServiceResponse<?> serviceResponse)
    {
        this(message == null ? serviceResponse.getMessage() : message + " " + serviceResponse.getMessage());
        setServiceResponseCode(serviceResponse.getServiceResponseCode());
        setHttpCode(serviceResponse.getHttpCode());
        setError(serviceResponse.getError());
    }

    public StatusCode getHttpCode()
    {
        return httpCode;
    }
    public void setHttpCode(StatusCode httpCode)
    {
        this.httpCode = httpCode;
    }

    public ServiceResponseCode getServiceResponseCode()
    {
        return serviceResponseCode;
    }

    public void setServiceResponseCode(ServiceResponseCode serviceResponseCode)
    {
        this.serviceResponseCode = serviceResponseCode;
    }

    public ServiceError getError()
    {
        return error;
    }

    public void setError(ServiceError error)
    {
        this.error = error;
    }

