package io.kestra.plugin.digitalocean.monitoring.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Window {
    FIVE_MINUTES("5m"),
    TEN_MINUTES("10m"),
    THIRTY_MINUTES("30m"),
    ONE_HOUR("1hr");

    private final String value;

    Window(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Window fromString(String val) {
        if (val == null) return null;
        for (Window w : values()) {
            if (w.value.equalsIgnoreCase(val) || w.name().equalsIgnoreCase(val)) {
                return w;
            }
        }
        throw new IllegalArgumentException("Unknown window value: " + val);
    }
}
