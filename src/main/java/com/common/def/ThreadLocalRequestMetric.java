package com.common.def;

import java.util.List;

public enum ThreadLocalRequestMetric implements ThreadLocalKeyDef{
    OBSERVERS_TRIGGERED(List.class);

    private String queryParameter;
    private String header;
    private String mdcKey;
    private Class<?> valueType;
    private Object defaultValue;

    private <X> ThreadLocalRequestMetric(Class<X> valueType)
    {
        this.valueType = valueType;
    }

    public <T> List<T> add(T value)
    {
        return ThreadLocalUtil.addToList(this, value);
    }

    @Override
    public ThreadLocalKeyDef[] getKeys() {
        return new ThreadLocalKeyDef[0];
    }

    @Override
    public String getQueryParameter() {
        return null;
    }

    @Override
    public String getHeader() {
        return null;
    }

    @Override
    public String getMdcKey() {
        return null;
    }

    @Override
    public Object getDefaultValue() {
        return null;
    }

    @Override
    public Class<?> getValueType() {
        return null;
    }
}
