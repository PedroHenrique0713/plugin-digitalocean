package io.kestra.plugin.digitalocean.monitoring.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MemoryMetricType {
    MEMORY_AVAILABLE("memory_available"),
    MEMORY_FREE("memory_free"),
    MEMORY_TOTAL("memory_total"),
    MEMORY_CACHED("memory_cached");

    private final String value;

    MemoryMetricType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static MemoryMetricType fromString(String val) {
        if (val == null) return null;
        for (MemoryMetricType m : values()) {
            if (m.value.equalsIgnoreCase(val) || m.name().equalsIgnoreCase(val)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Unknown memory metric type: " + val);
    }
}
