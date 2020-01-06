package com.common.def;

import java.util.Date;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.dibs.service.common.ServiceError;
import com.dibs.service.common.response.StatusCode;
import com.dibs.service.common.response.HttpCodeAdapter;
import com.dibs.service.common.response.ServiceResponseCode;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@XmlRootElement(name = "response", namespace = "v1")
@XmlType(name = "response", namespace = "V1")
@XmlSeeAlso({ServiceError.class})
public class ServiceResponse<X extends ServiceData>
{
    private StatusCode httpCode;
    private ServiceResponseCode serviceResponseCode;
    private String message;
    private X result;
    private Date verifiedDate;
    private Long totalResults;
    private Integer pageSize;
    private Integer pageStart;
    private ServiceError error;
    private String requestId;

    public ServiceResponse()
    {
    }

    @XmlJavaTypeAdapter(HttpCodeAdapter.class)
    @XmlElement
    public StatusCode getHttpCode()
    {
        return httpCode;
    }

    public void setHttpCode(StatusCode httpCode)
    {
        this.httpCode = httpCode;
    }

    @XmlElement
    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    @XmlElement
    @JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = ApiConstants.V1_JSON_TYPE_PROPERTY_NAME)
    public X getResult()
    {
        return result;
    }

    public void setResult(X result)
    {
        this.result = result;
    }

    @XmlElement
    public ServiceResponseCode getServiceResponseCode()
    {
        return serviceResponseCode;
    }

    public void setServiceResponseCode(ServiceResponseCode code)
    {
        this.serviceResponseCode = code;
    }

    public Date getVerifiedDate()
    {
        return verifiedDate;
    }

    public void setVerifiedDate(Date verifiedDate)
    {
        this.verifiedDate = verifiedDate;
    }

    public Long getTotalResults()
    {
        return totalResults;
    }

    public void setTotalResults(Long totalResults)
    {
        this.totalResults = totalResults;
    }

    public Integer getPageSize()
    {
        return pageSize;
    }

    public void setPageSize(Integer pageSize)
    {
        this.pageSize = pageSize;
    }

    public Integer getPageStart()
    {
        return pageStart;
    }

    public void setPageStart(Integer pageStart)
    {
        this.pageStart = pageStart;
    }

    public ServiceError getError()
    {
        return error;
    }

    @XmlElement
    @JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = ApiConstants.V1_JSON_TYPE_PROPERTY_NAME)
    public void setError(ServiceError error)
    {
        this.error = error;
    }

    public String getRequestId()
    {
        return requestId;
    }

    public void setRequestId(String requestId)
    {
        this.requestId = requestId;
    }
}
