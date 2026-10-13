package io.kestra.plugin.digitalocean.monitoring.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Compare {
    GREATER_THAN("greater_than"),
    LESS_THAN("less_than");

    private final String value;

    Compare(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Compare fromString(String val) {
        if (val == null) return null;
        for (Compare c : values()) {
            if (c.value.equalsIgnoreCase(val) || c.name().equalsIgnoreCase(val)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown compare operator: " + val);
    }
}
