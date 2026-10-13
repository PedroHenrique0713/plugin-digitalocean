package io.kestra.plugin.digitalocean.monitoring.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DiskMetricType {
    FILESYSTEM_FREE("filesystem_free"),
    FILESYSTEM_SIZE("filesystem_size"),
    DISK_READ("disk_read"),
    DISK_WRITE("disk_write");

    private final String value;

    DiskMetricType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DiskMetricType fromString(String val) {
        if (val == null) return null;
        for (DiskMetricType d : values()) {
            if (d.value.equalsIgnoreCase(val) || d.name().equalsIgnoreCase(val)) {
                return d;
            }
        }
        // Support common aliases like read/write
        if ("read".equalsIgnoreCase(val)) {
            return DISK_READ;
        }
        if ("write".equalsIgnoreCase(val)) {
            return DISK_WRITE;
        }
        throw new IllegalArgumentException("Unknown disk metric type: " + val);
    }
}
