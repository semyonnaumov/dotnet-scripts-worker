package com.naumov.dotnetscriptsworker.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple map wrapper, used to mitigate spring {@code Map<String, Object>} injection problems.
 */
public class KafkaPropertyMapWrapper {
    private final Map<String, Object> props = new HashMap<>();

    public void put(String key, Object value) {
        this.props.put(key, value);
    }

    public Map<String, Object> toMap() {
        return Collections.unmodifiableMap(props);
    }
}