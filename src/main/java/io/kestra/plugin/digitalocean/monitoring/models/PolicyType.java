package io.kestra.plugin.digitalocean.monitoring.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PolicyType {
    CPU("v1/insights/droplet/cpu"),
    MEMORY("v1/insights/droplet/memory"),
    DISK("v1/insights/droplet/disk"),
    BANDWIDTH_INBOUND("v1/insights/droplet/bandwidth_inbound"),
    BANDWIDTH_OUTBOUND("v1/insights/droplet/bandwidth_outbound");

    private final String value;

    PolicyType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PolicyType fromString(String val) {
        if (val == null) return null;
        for (PolicyType p : values()) {
            if (p.value.equalsIgnoreCase(val) || p.name().equalsIgnoreCase(val)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown policy type: " + val);
    }
}
