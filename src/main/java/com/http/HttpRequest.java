package com.http;

import com.common.def.ResponseType;
import com.common.def.ServiceData;
import org.springframework.http.HttpMethod;

public class HttpRequest<T>
{

    private ServiceData requestBody;
    private String url;
    private HttpMethod httpMethod;
    private ResponseType responseType;
    private UrlParams urlParams;

    public HttpRequest(ServiceRestTemplate template, String url, HttpMethod httpMethod)
    {
        this.url = url;
        this.httpMethod = httpMethod;
    }

    public ServiceData getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(ServiceData requestBodyS) {
        this.requestBody = requestBodyS;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType;
    }

    public UrlParams getUrlParams() {
        return urlParams;
    }

    public void setUrlParams(UrlParams urlParams) {
        this.urlParams = urlParams;
    }
}
