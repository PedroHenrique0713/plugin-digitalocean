package io.kestra.plugin.digitalocean.monitoring.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NetworkInterface {
    PUBLIC("public"),
    PRIVATE("private");

    private final String value;

    NetworkInterface(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static NetworkInterface fromString(String val) {
        if (val == null) return null;
        for (NetworkInterface ni : values()) {
            if (ni.value.equalsIgnoreCase(val) || ni.name().equalsIgnoreCase(val)) {
                return ni;
            }
        }
        throw new IllegalArgumentException("Unknown network interface: " + val);
    }
}
