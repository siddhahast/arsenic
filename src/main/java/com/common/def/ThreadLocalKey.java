package com.common.def;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum ThreadLocalKey implements ThreadLocalKeyDef
{
	REFRESH_CACHE("refreshCache", null, YesNo.class, YesNo.N), //
	BYPASS_CACHE("bypassCache", "bypasscache", null, YesNo.class, YesNo.N), //
	BYPASS_READ_REPLICA(null, null, Boolean.class, Boolean.FALSE), //
	BYPASS_LEADER_ELECTION("bypassLeaderElection", null, Boolean.class, false), //
	PAGE_START("pageStart", null, Integer.class, null), //
	PAGE_SIZE("pageSize", null, Integer.class, null), //
	PAGE_CONTEXT("pageContext", null, String.class, null), //
	RETURN_TOTAL_RESULTS("returnTotalResults", null, YesNo.class, YesNo.N), //
	SYSTEM_TIME_OFFSET("systemTimeOffset", null, Integer.class, null), //
	BYPASS_REPORTING("bypassReporting", null, Boolean.class, Boolean.FALSE), //
	TOTAL_RESULTS(Long.class), //
	FIELDS("fields", null, String[].class, null), //
	INTERNAL_REQUEST(Boolean.class, Boolean.FALSE), //
	SERVICE_EXCEPTION(Throwable.class), //
	SERVICE_RESPONSE_CODE(ServiceResponseCode.class), //
	CALLER_IP(null, FIELD_CALLER_IP, String.class, null), // ip address of direct caller of this server (excluding varnish and other proxies)
	CLIENT_IP(null, FIELD_CLIENT_IP, String.class, null), // ip address of the end device making the initial call
	CLIENT_USER_ID(null, FIELD_CLIENT_USER_ID, String.class, null), // original user id
	TRACE_GUID(FIELD_TRACE_GUID, FIELD_TRACE_GUID, String.class, null), // traceGuid is the same for all the requests originating from the same GraphQL call
	REQUEST_GUID(null, FIELD_REQUEST_GUID, String.class, null), // requestGuid is unique for the scope of the http/jms/cron request
	CALLER_REQUEST_GUID(null, FIELD_CALLER_REQUEST_GUID, String.class, null), // requestGuid of the parent caller, useful for linking requests
	USER_AGENT(null, FIELD_USER_AGENT, String.class, null), //
	REFERER(null, FIELD_REFERER, String.class, null), //
	ENDPOINT(null, FIELD_ENDPOINT, String.class, null), //
	HTTP_EXECUTION_MODE("executionMode", "executionMode", HttpExecutionMode.class, HttpExecutionMode.FOREGROUND), //
	STATUS_CODE(null, FIELD_STATUS_CODE, StatusCode.class, null), //
	HTTP_REQUEST, //javax.servlet.ServletRequest
	REQUEST_TYPE(null, FIELD_REQUEST_TYPE, RequestType.class, null),//
	HTTP_METHOD(null, FIELD_HTTP_METHOD, String.class, null), //
	HTTP_RESPONSE, //javax.servlet.ServletResponse
	HTTP_REQUEST_HEADERS(null, FIELD_REQUEST_HEADERS, null, null), // com.dibs.util.web.HttpHeaders
	HTTP_RESPONSE_HEADERS(null, FIELD_RESPONSE_HEADERS, null, null), //com.dibs.util.web.HttpHeaders
	HTTP_PROXY_HEADERS, // com.dibs.util.web.HttpHeaders
	HTTP_REQUEST_URL(null, FIELD_URL, String.class, null), //
	HTTP_REQUEST_PAYLOAD(null, FIELD_REQUEST_BODY, String.class, null), //
	REQUEST_START_MILLIS(Long.class), //
	OBSERVABLE_EVENT(List.class), //
	ASYNC_EVENT_HANDLER(List.class), //
	MVC_HANDLER(Object.class), // most likely -> org.springframework.web.method.HandlerMethod
	JMS_REQUEST(Object.class), // javax.jms.Message - don't want shared-service to depend on javax.jms
	WORKFLOW_AUTH_BYPASS(YesNo.class, YesNo.N), //
	BYPASS_VALIDATION("bypassValidation", null, Boolean.class, false), //
	INDEX_MODE("indexMode", null, IndexMode.class, IndexMode.ASYNC_QUEUE), //
	MOCK_OBJECTS("mockObjects", null, String[].class, null), //
	PARTIAL_UPDATE(Boolean.class, Boolean.FALSE), //
	VERBOSE("verbose", null, Boolean.class, Boolean.FALSE), //
	BACKGROUND_PROCESS_ID(null, null, String.class, null), //
	AVAILABLE_ACTIONS("availableActions", null, String[].class, null), //
	CACHE_KEYS("cacheKeys", null, String[].class, null), //
	ROOT_ANCHOR("rootAnchor", FIELD_ROOT_ANCHOR, Anchor.class, null), //
	TEST_MODE("testMode", null, Boolean.class, false), //
	LOCALE("locale", null, Locale.class, Locale.en_US), //
	CURRENCY_RATE_EVENT_TYPE("currencyRateEventType", "currencyRateEventType", CurrencyConversionRateEventType.class, CurrencyConversionRateEventType.DEFAULT), //
	LOCKS("locks", null, Map.class, null), //
	LOG_REDACT(Set.class), // Set of com.dibs.util.log.redact.LogRedactInstruction
	ALL_ACTIONS("allActions", null, Boolean.class, Boolean.FALSE),
	POST_TRANSACTION_CACHE_EVICTIONS(Map.class),
	PAYMENT_GATEWAY_SANBOX_REQUESTED("paymentGatewaySandboxPlease", null, YesNo.class, YesNo.N),
	X_DIBS_USER_AGENT(null, FIELD_CLIENT_USER_AGENT, String.class, null),
	HTTP_REQUEST_OPERATION_TYPE(null, FIELD_REQUEST_OPERATION_TYPE, HttpOperationType.class, null);

	private String queryParameter;
	private String header;
	private String mdcKey;
	private Class<?> valueType;
	private Object defaultValue;

	private ThreadLocalKey()
	{
	}

	private <X> ThreadLocalKey(String queryParameter, String header, String mdcKey, Class<X> valueType, X defaultValue)
	{
		this.queryParameter = queryParameter;
		this.header = header;
		this.mdcKey = mdcKey;
		this.valueType = valueType;
		this.defaultValue = defaultValue;
	}
	private <X> ThreadLocalKey(Class<X> valueType)
	{
		this.valueType = valueType;
	}
	private <X> ThreadLocalKey(Class<X> valueType, X defaultValue)
	{
		this.valueType = valueType;
		this.defaultValue = defaultValue;
	}

	private <X> ThreadLocalKey(String queryParameter, String mdcKey, Class<?> valueType, Object defaultValue) {
		this.queryParameter = queryParameter;
		this.mdcKey = mdcKey;
		this.valueType = valueType;
		this.defaultValue = defaultValue;
	}

	public <T> T get()
	{
		return ThreadLocalUtil.get(this);
	}

	public <T> T put(T value)
	{
		return ThreadLocalUtil.put(this, value);
	}
	public <T> T putIfAbsent(T value)
	{
		return ThreadLocalUtil.putIfAbsent(this, value);
	}
	public <T> List<T> addToList(T value)
	{
		return ThreadLocalUtil.addToList(this, value);
	}
	public <T> Set<T> addToSet(T value)
	{
		return ThreadLocalUtil.addToSet(this, value);
	}
	public <S, T> Map<S, T> addToMap(S mapKey, T value)
	{
		return ThreadLocalUtil.addToMap(this, mapKey, value);
	}
	@Override
	public String getQueryParameter()
	{
		return queryParameter;
	}

	@Override
	public String getHeader() {
		return header;
	}

	@Override
	public String getMdcKey()
	{
		return mdcKey;
	}

	@Override
	public ThreadLocalKeyDef[] getKeys()
	{
		return values();
	}

	@Override
	public Class<?> getValueType()
	{
		return valueType;
	}

	@Override
	public Object getDefaultValue()
	{
		return defaultValue;
	}

	public static Long getRequestDuration()
	{
		Long startMillis = ThreadLocalKey.REQUEST_START_MILLIS.get();
		return startMillis == null ? null : System.currentTimeMillis() - startMillis;
	}

}