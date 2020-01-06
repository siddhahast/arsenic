package com.http;

import com.common.def.*;
import com.dibs.service.common.NewRelicTrace;
import com.dibs.service.common.application.Application;
import com.dibs.service.common.def.ProxyHeader;
import com.dibs.service.common.log.ServiceRestTemplateLog4j2Message;
import com.dibs.service.common.message.MessageConverterFactory;
import com.dibs.service.common.message.SchemaVersionSupportHttpMessageConverter;
import com.dibs.service.v2.data.error.ErrorHandlingException;
import com.dibs.service.v2.data.error.ServiceClientException;
import com.exception.RawResponseAwareException;
import org.apache.http.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
public class ServiceRestTemplate extends RestTemplate
{

    public static final String URL_SUFFIX = ".url";
    public static final String SERVICE_VERSION_SUFFIX = ".serviceVersion";
    public static final String URL_BUILDER_SUFFIX = ".urlBuilder";
    private static final Boolean DEFAULT_ERROR_HANDLING_ENABLED = Boolean.FALSE;

    private static final Logger LOG = LogManager.getLogger(ServiceRestTemplate.class);

    private SchemaVersionSupportHttpMessageConverter converter = MessageConverterFactory.getDefaultConverter();

    private MediaType accept = MediaType.APPLICATION_XML;
    private String baseUrl;

    private String serviceVersion;
    private ApiVersion apiVersion;

    private String clientName;
    private Properties properties;

    private UrlBuilder urlBuilder;

    private ServiceClientErrorHandler errorHandler;
    private Boolean errorHandlingEnabled;

    private ApiSerializationUtil apiSerializationUtil;

    public ServiceRestTemplate()
    {
        this.properties = System.getProperties();
        this.errorHandlingEnabled = DEFAULT_ERROR_HANDLING_ENABLED;
        setRequestFactory(ServiceRequestFactory.getInstance());
    }

    /**
     * @param clientName
     */
    public ServiceRestTemplate(String clientName, String serviceVersion)
    {
        this();
        this.clientName = clientName;
        this.serviceVersion = serviceVersion;
        this.apiSerializationUtil = new ApiSerializationUtil();
    }

    private boolean isInitRequired()
    {
        return this.baseUrl == null;
    }

    private void init()
    {
        try
        {
            if (isInitRequired())
            {
                if (getRequestFactory() == null)
                {
                    setRequestFactory(ServiceRequestFactory.getInstance());
                }

                setErrorHandler(new ErrorHandler());

                errorHandler = errorHandler == null ? new DefaultServiceClientErrorHandler() : errorHandler;

                if (this.clientName == null)
                {
                    throw new RuntimeException("service client name is null");
                }

                if (this.properties == null)
                {
                    throw new RuntimeException("properties are null");
                }

                //
                // configure url
                //

                String urlKey = clientName + URL_SUFFIX;

                this.baseUrl = properties.getProperty(urlKey);

                if (this.baseUrl == null)
                {
                    throw new RuntimeException(urlKey + " not defined in properties");
                }

                //
                // configure service version
                //

                if (serviceVersion == null)
                {
                    String serviceVersionKey = clientName + SERVICE_VERSION_SUFFIX;
                    this.serviceVersion = properties.getProperty(serviceVersionKey, "");
                }

                //
                // URL builder
                //

                String urlBuilderClass = properties.getProperty(clientName + URL_BUILDER_SUFFIX);

                this.urlBuilder = urlBuilderClass == null ? UrlBuilder.DEFAULT_BUILDER : (UrlBuilder) Class.forName(urlBuilderClass).newInstance();

                LOG.info("initialize(): baseUrl=[{}] serviceVersion=[{}], urlBuilder=[{}]", this.baseUrl, this.serviceVersion, this.urlBuilder);
            }
        }

        catch (Exception ex)
        {
            throw new RuntimeException("Error loading ServiceRestTemplate", ex);
        }
    }

    public void setConverter(SchemaVersionSupportHttpMessageConverter converter)
    {
        this.converter = converter;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R extends ServiceResponse> R doExecute(String url, HttpMethod httpMethod)
    {
        return (R) doExecute(url, httpMethod, null, null, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R extends ServiceResponse> R doExecute(String url, HttpMethod httpMethod, UrlParams urlParams)
    {
        return (R) doExecute(url, httpMethod, urlParams, null, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R extends ServiceResponse> R doExecute(String url, HttpMethod httpMethod, UrlParams urlParams, ServiceData requestBody)
    {
        return (R) doExecute(url, httpMethod, urlParams, requestBody, null);
    }

    @SuppressWarnings("rawtypes")
    public <R extends ServiceResponse> R doExecute(String url, HttpMethod httpMethod, UrlParams urlParams, ServiceData requestBody, ResponseType responseType)
    {
        R response = null;
        try
        {
            init();

            responseType = Optional.ofNullable(responseType).orElse(ResponseType.DEFAULT);

            urlParams = Optional.ofNullable(urlParams).orElse(new UrlParams());

            addApiToken(urlParams);

            if (urlParams.hasQueryString())
            {
                url = url + (url.contains("?") ? "&" : "?") + urlParams.toQueryString();
            }

            url = urlBuilder.build(baseUrl, url, serviceVersion);

            HttpRequest<?> httpRequest = new HttpRequest<>(this, url, httpMethod);
            httpRequest.setRequestBody(requestBody);
            httpRequest.setUrlParams(urlParams);
            httpRequest.setResponseType(responseType);

            response = internalExecute(httpRequest);

            Boolean inErrorRange = errorHandler.isErrorResponse(response);

            if (inErrorRange)
            {
                /*
                 * Response is an error response. Build ServiceError into an exception and throw.
                 */
                ServiceClientException serviceClientException = //
                        errorHandler.buildServiceError(url, httpMethod, urlParams, response, null);

                throwServiceClientException(serviceClientException);
            }

        }
        catch (ServiceClientException ex)
        {
            throw ex;
        }
        catch (ErrorHandlingException ex)
        {

            /*
             * couldn't even do error handling. Return response anyway just.
             */
            return response;
        }
        catch (Exception ex)
        {
            /*
             * Need to try/catch in case the other service is down or sends us a message that isn't json/xml for some reason. Otherwise we will
             * bomb out trying to parse the response.
             *
             * If we're in this catch, something went horribly wrong with the other service. Build an error, turn it into an exception, and throw it.
             */
            ServiceClientException serviceClientException = errorHandler.buildServiceError(url, httpMethod, urlParams, null, ex);

            throwServiceClientException(serviceClientException);
        }

        return response;
    }

    private void throwServiceClientException(ServiceClientException serviceClientException)
    {
        if (errorHandlingEnabled)
        {
            throw serviceClientException;
        }
        else
        {
            LOG.error("Error calling service. Error handling disabled.", serviceClientException);
        }
    }

    @SuppressWarnings("rawtypes")
    protected <R extends ServiceResponse> R internalExecute(HttpRequest<?> httpRequest)
    {
        long startMillis = System.currentTimeMillis();
        RuntimeException error = null;
        URI expandedUri = null;
        ServiceResponseExtractor<R> responseExtractor = null;
        R result = null;

        try
        {
            // httpRequest must never be null

            ServiceData requestBody = httpRequest.getRequestBody();

            ApiVersion responseVersion = Optional.ofNullable(apiVersion).//
                    orElse(ApiVersion.fromUrl(httpRequest.getUrl()));
            ApiVersion payloadVersion = httpRequest.getRequestBody() == null ? //
                    responseVersion : ApiVersion.fromClass(requestBody.getClass());

            ServiceRequestCallback requestCallback = //
                    new ServiceRequestCallback(requestBody, accept, payloadVersion);

            responseExtractor = //
                    new ServiceResponseExtractor<R>(responseVersion, httpRequest.getResponseType());

            expandedUri = new UriTemplate(httpRequest.getUrl()).//
                    expand(httpRequest.getUrlParams().getTokenParams());

            // call parent spring method
            result = doExecute(expandedUri, httpRequest.getHttpMethod(), requestCallback, responseExtractor);

            return result;
        }
        catch (RuntimeException ex)
        {
            error = ex;
            throw ex;
        }
        finally
        {
            long duration = System.currentTimeMillis() - startMillis;

            String endpoint = responseExtractor == null ? null : responseExtractor.getEndpoint();


            if(error == null)
            {
                LOG.info(new ServiceRestTemplateLog4j2Message(expandedUri, endpoint, httpRequest.getHttpMethod(),
                        result.getHttpCode(), duration));
            }
            else if(error instanceof RawResponseAwareException)
            {
                String rawResponse = ((RawResponseAwareException) error).getRawResponseAsString();

                LOG.error(new ServiceRestTemplateLog4j2Message(expandedUri, endpoint, httpRequest.getHttpMethod(),
                        result.getHttpCode(), duration, null, rawResponse), error);

            }
            else if(error instanceof ResourceAccessException) {
                LOG.error(new ServiceRestTemplateLog4j2Message(expandedUri, endpoint, httpRequest.getHttpMethod(),
                        null, duration), error);
            }
            else
            {
                LOG.error(new ServiceRestTemplateLog4j2Message(expandedUri, endpoint, httpRequest.getHttpMethod(),
                        result.getHttpCode(), duration), error);
            }

        }
    }


    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    public void setAccept(MediaType accept)
    {
        this.accept = accept;
    }

    class ServiceRequestCallback implements RequestCallback
    {
        private final MediaType accept;
        private final ServiceData serviceData;
        private final ApiVersion contextKey;

        public ServiceRequestCallback(ServiceData serviceData, MediaType accept, ApiVersion contextKey)
        {
            this.accept = accept;
            this.serviceData = serviceData;
            this.contextKey = contextKey;
        }

        @Override
        public void doWithRequest(ClientHttpRequest request) throws IOException
        {
            addHeader(request, HttpHeaders.ACCEPT, accept);
            addHeader(request, HttpHeaders.REFERER, ThreadLocalKey.REFERER.get());
            addHeader(request, ProxyHeader.CLIENT_IP.getName(), ThreadLocalKey.CLIENT_IP.get());
            addHeader(request, ProxyHeader.CLIENT_USER_ID.getName(), ThreadLocalKey.CLIENT_USER_ID.get());
            addHeader(request, ProxyHeader.TRACE_GUID.getName(), ThreadLocalKey.TRACE_GUID.get());
            addHeader(request, ProxyHeader.REQUEST_GUID.getName(), ThreadLocalKey.REQUEST_GUID.get());
            addHeader(request, ProxyHeader.MOCK_OBJECTS.getName(), ThreadLocalKey.MOCK_OBJECTS.get());
            addHeader(request, ProxyHeader.X_DIBS_USER_AGENT.getName(), ThreadLocalKey.X_DIBS_USER_AGENT.get());

            if (serviceData != null)
            {
                converter.write(contextKey, serviceData, accept, request);
            }
        }

        private void addHeader(ClientHttpRequest request, String name, Object value)
        {
            if (value != null)
            {
                request.getHeaders().put(name, Arrays.asList(value.toString()));
            }
        }
    }

    class ServiceResponseExtractor<T> implements ResponseExtractor<T>
    {
        private final ApiVersion contextKey;
        private final ResponseType responseType;
        private ClientHttpResponse response;

        private String endpoint;

        public ServiceResponseExtractor(ApiVersion contextKey, ResponseType responseType)
        {
            this.contextKey = contextKey;
            this.responseType = responseType;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T extractData(ClientHttpResponse response) throws IOException
        {
            this.response = response;
            MediaType contentType = response.getHeaders().getContentType();
            this.endpoint = response.getHeaders().getFirst(ProxyHeader.ENDPOINT.getName());
            Class<?> responseClass = determineResponseClass();

            if (!converter.canRead(responseClass, contentType))
            {
                HttpMessageNotReadableException e = new HttpMessageNotReadableException("Unsupported content type " + contentType);
                LOG.error("extractData(): {} ", getResponseBodyAsString(), e);

                throw e;
            }


            return (T) converter.read(contextKey, responseClass, response);
        }

        private Class<?> determineResponseClass()
        {
            switch (responseType)
            {
                case LIST:
                    return contextKey.getListResponseClass();
                case MAP:
                    return contextKey.getMapResponseClass();
                case SERVICE_SEARCH:
                    return contextKey.getSearchResponseClass();
                default:
                    return contextKey.getResponseClass();
            }
        }

        public String getEndpoint()
        {
            return endpoint;
        }

        /**
         * This method can only be called once since the input stream will be flushed
         *
         * @return
         */
        public String getResponseBodyAsString()
        {
            String toString = "";
            try
            {
                if (response != null && response.getBody() != null)
                {
                    InputStream body = response.getBody();

                    if (body != null)
                    {

                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                        int nRead;
                        byte[] data = new byte[16384];

                        while ((nRead = body.read(data, 0, data.length)) != -1)
                        {
                            buffer.write(data, 0, nRead);
                        }

                        buffer.flush();

                        toString = new String(buffer.toByteArray());
                    }

                }
            }
            catch (Exception ex)
            {
                toString = "ERROR: " + ex.toString();
            }

            return toString;
        }
    }

    class PayloadToString
    {
        private final Object payload;

        public PayloadToString(Object payload)
        {
            this.payload = payload;
        }

        @Override
        public String toString()
        {
            String body = "NULL";
            try
            {
                if (payload != null)
                {
                    if (accept == MediaType.APPLICATION_XML)
                    {
                        body = apiSerializationUtil.toXml(payload);
                    }
                    else if (accept == MediaType.APPLICATION_JSON)
                    {
                        body = apiSerializationUtil.toJson(payload);
                    }
                    else
                    {
                        body = "ERROR: Invalid mediaType " + accept;
                    }

                }
            }
            catch (Exception ex)
            {
                body = "ERROR " + ex.toString();
            }

            return body;
        }
    }

    class ErrorHandler implements ResponseErrorHandler
    {

        @Override
        public boolean hasError(ClientHttpResponse response) throws IOException
        {
            return false;
        }

        @Override
        public void handleError(ClientHttpResponse response) throws IOException
        {

        }

    }

    private void addApiToken(UrlParams urlParams)
    {
        if (urlParams.getQueryParams().get(Application.API_TOKEN_KEY) != null)
        {
            return;
        }

        if (Application.getInstance() != null)
        {
            urlParams.addQueryParam(Application.API_TOKEN_KEY, Application.getInstance().getApiToken());
        }
        else
        {
            LOG.warn("addApiToken(): api token not set in the application. Not appending to the request.");
        }

    }

    public String getClientName()
    {
        return clientName;
    }

    public void setClientName(String clientName)
    {
        this.clientName = clientName;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public void setProperties(Properties properties)
    {
        this.properties = properties;
    }

    public SchemaVersionSupportHttpMessageConverter getConverter()
    {
        return converter;
    }

    public MediaType getAccept()
    {
        return accept;
    }

    public ApiVersion getApiVersion()
    {
        return apiVersion;
    }

    public void setApiVersion(ApiVersion apiVersion)
    {
        this.apiVersion = apiVersion;
    }

    public String getServiceVersion()
    {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion)
    {
        this.serviceVersion = serviceVersion;
    }

    public UrlBuilder getUrlBuilder()
    {
        return urlBuilder;
    }

    public void setUrlBuilder(UrlBuilder urlBuilder)
    {
        this.urlBuilder = urlBuilder;
    }

    public void setErrorHandlingEnabled(Boolean errorHandlingEnabled)
    {
        this.errorHandlingEnabled = errorHandlingEnabled;
    }

    public void setErrorHandler(ServiceClientErrorHandler errorHandler)
    {
        this.errorHandler = errorHandler;
    }

}
