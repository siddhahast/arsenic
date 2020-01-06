package com.cache;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.common.def.ApiConstants;
import com.common.def.ServiceData;
import org.apache.commons.lang.builder.ToStringBuilder;


@XmlRootElement(name = "cacheEvictRequest", namespace = ApiConstants.V2_NAMESPACE)
@XmlType(name = "cacheEvictRequest", namespace = ApiConstants.V2_NAMESPACE)
public class CacheEvictRequest implements ServiceData {
    private String name;
    private List<String> keys;
    private boolean all;
    private String keyClass;
    private boolean postEvict;
    private String uuid;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKeyClass() {
        return keyClass;
    }

    public void setKeyClass(String keyClass) {
        this.keyClass = keyClass;
    }

    public List<String> getKeys() {
        return keys;
    }

    public void setKeys(List<String> keys) {
        this.keys = keys;
    }

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    public boolean isPostEvict() {
        return postEvict;
    }

    public void setPostEvict(boolean postEvict) {
        this.postEvict = postEvict;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}