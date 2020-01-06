package com.cache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BulkGetResponse<K, V> {
    private Set<K> notFoundKeys;
    private Map<K, V> results;

    public BulkGetResponse() {
        notFoundKeys = new HashSet<>();
        results = new HashMap<>();
    }

    public Set<K> getNotFoundKeys() {
        return notFoundKeys;
    }

    public void setNotFoundKeys(Set<K> notFoundKeys) {
        this.notFoundKeys = notFoundKeys;
    }

    public Map<K, V> getResults() {
        return results;
    }

    public void setResults(Map<K, V> results) {
        this.results = results;
    }
}
