package io.kestra.plugin.digitalocean.monitoring.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TrafficDirection {
    INBOUND("inbound"),
    OUTBOUND("outbound");

    private final String value;

    TrafficDirection(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TrafficDirection fromString(String val) {
        if (val == null) return null;
        for (TrafficDirection td : values()) {
            if (td.value.equalsIgnoreCase(val) || td.name().equalsIgnoreCase(val)) {
                return td;
            }
        }
        throw new IllegalArgumentException("Unknown traffic direction: " + val);
    }
}
