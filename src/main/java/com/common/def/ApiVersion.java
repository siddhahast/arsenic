package com.common.def;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("rawtypes")
public enum ApiVersion {

    NO_VERSION(null, "no_version", null, null, null, null), //
    V1(ApiConstants.V1_PACKAGE_NAME, ApiConstants.V1_URL_PARAM, com.dibs.service.v1.data.ServiceResponseTypeV1.class, com.dibs.service.v1.data.ServiceListResponse.class, com.dibs.service.v1.data.InternalServiceMapResponse.class, com.dibs.service.v1.data.ServiceSearchResponse.class), //
    V2(ApiConstants.V2_PACKAGE_NAME, ApiConstants.V2_URL_PARAM, com.dibs.service.v2.data.ServiceResponse.class, com.dibs.service.v2.data.ServiceListResponse.class, com.dibs.service.v2.data.InternalServiceMapResponse.class, com.dibs.service.v2.data.ServiceSearchResponse.class), //
    V3(ApiConstants.V3_PACKAGE_NAME, ApiConstants.V3_URL_PARAM, com.dibs.service.v3.data.ServiceResponse.class, com.dibs.service.v3.data.ServiceListResponse.class, com.dibs.service.v3.data.InternalServiceMapResponse.class, com.dibs.service.v3.data.ServiceSearchResponse.class), //
    V4(ApiConstants.V4_PACKAGE_NAME, ApiConstants.V4_URL_PARAM, com.dibs.service.v4.data.ServiceResponse.class, com.dibs.service.v4.data.ServiceListResponse.class, com.dibs.service.v4.data.InternalServiceMapResponse.class, com.dibs.service.v4.data.ServiceSearchResponse.class);

    private static EnumLookerUpper<ApiVersion> HELPER = new EnumLookerUpper<ApiVersion>(ApiVersion.class);

    private static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile(".*\\.(v\\d+)\\.?.*");
    private static final Pattern URL_ARG_PATTERN = Pattern.compile("http[s]?://.*/(\\d+|\\d+\\.\\d+)/.*");

    private static final Map<String, ApiVersion> packageNameToApiVersion;
    private static final Map<String, ApiVersion> urlArgToKeyMap;

    private String packageName;
    private String urlArg;
    private Class<? extends com.common.def.ServiceResponse> responseClass;
    private Class<? extends com.common.def.ServiceResponse> listResponseClass;
    private Class<? extends com.common.def.ServiceResponse> mapResponseClass;
    private Class<? extends com.common.def.ServiceResponse> searchResponseClass;

    static {
        packageNameToApiVersion = new HashMap<>();
        urlArgToKeyMap = new HashMap<>();

        for (ApiVersion key : ApiVersion.values()) {
            packageNameToApiVersion.put(key.getPackageName(), key);
            urlArgToKeyMap.put(key.getUrlArg(), key);
        }
    }

    private ApiVersion(String packageName, String urlArg,
                       Class<? extends com.common.def.ServiceResponse> responseClass,
                       Class<? extends com.common.def.ServiceResponse> listResponseClass,
                       Class<? extends com.common.def.ServiceResponse> mapResponseClass,
                       Class<? extends com.common.def.ServiceResponse> searchResponseClass) {

        this.packageName = packageName;
        this.urlArg = urlArg;
        this.responseClass = responseClass;
        this.listResponseClass = listResponseClass;
        this.mapResponseClass = mapResponseClass;
        this.searchResponseClass = searchResponseClass;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getUrlArg() {
        return urlArg;
    }

    public Class<? extends ServiceResponse> getResponseClass() {
        return responseClass;
    }

    public Class<? extends ServiceResponse> getListResponseClass() {
        return listResponseClass;
    }

    public Class<? extends ServiceResponse> getMapResponseClass() {
        return mapResponseClass;
    }

    public Class<? extends ServiceResponse> getSearchResponseClass() {
        return searchResponseClass;
    }

    public static ApiVersion fromClass(Class<?> clazz) {
        String packageNameArg = getKeyFromInput(PACKAGE_NAME_PATTERN, clazz.getName());
        return packageNameArg == null ? ApiVersion.NO_VERSION : packageNameToApiVersion.get(packageNameArg);
    }

    public static ApiVersion fromPackageName(String packageName) {
        String packageNameArg = getKeyFromInput(PACKAGE_NAME_PATTERN, packageName);
        return packageNameArg == null ? ApiVersion.NO_VERSION : packageNameToApiVersion.get(packageNameArg);
    }

    public static ApiVersion fromUrl(String url) {
        String urlArg = getKeyFromInput(URL_ARG_PATTERN, url);
        return urlArgToKeyMap.get(urlArg);
    }

    public static ApiVersion fromAnyField(String fieldValue) {
        return HELPER.lookup(fieldValue);
    }

    private static String getKeyFromInput(Pattern patternToMatch, String input) {
        Matcher matcher = patternToMatch.matcher(input);

        String version = null;

        if (matcher.matches()) {
            version = matcher.group(1);
        }

        if (version != null) {
            int indexOfPeriod = version.indexOf(".");
            version = indexOfPeriod > -1 ? version.substring(indexOfPeriod + 1) : version;
        }

        return version;
    }
}
