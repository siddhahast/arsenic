package com.http;

public class UrlBuilder
{

    public static UrlBuilder DEFAULT_BUILDER =

    public String build(String baseUrl, String url, String serviceVersion)
    {
        String expandecUrl = baseUrl + "/" + serviceVersion + "/" + url;
        return expandecUrl;
    }

}
