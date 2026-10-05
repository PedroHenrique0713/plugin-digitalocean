package io.kestra.plugin.digitalocean.snapshot;

import io.kestra.core.models.tasks.Output;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static io.kestra.plugin.digitalocean.AbstractDigitalOceanTask.asInteger;
import static io.kestra.plugin.digitalocean.AbstractDigitalOceanTask.asString;

@Builder
@Getter
public class SnapshotOutput implements Output {

    @Schema(title = "Snapshot ID", description = "Numeric for a droplet snapshot, a UUID for a volume snapshot.")
    private final String id;

    @Schema(title = "Snapshot name")
    private final String name;

    @Schema(title = "Resource type", description = "Type of resource the snapshot was taken from: droplet or volume.")
    private final String resourceType;

    @Schema(title = "Resource ID", description = "ID of the droplet or volume the snapshot was taken from.")
    private final String resourceId;

    @Schema(title = "Region slugs", description = "Regions the snapshot is available in, e.g. nyc3.")
    private final List<String> regions;

    @Schema(title = "Minimum disk size in GB", description = "Smallest disk a droplet or volume needs to use this snapshot.")
    private final Integer minDiskSize;

    @Schema(title = "Billable size in GB")
    private final Double sizeGigabytes;

    @Schema(title = "Tags")
    private final List<String> tags;

    @Schema(title = "Creation timestamp")
    private final Instant createdAt;

    public static SnapshotOutput from(Map<String, Object> snapshot) {
        var createdAt = asString(snapshot.get("created_at"));

        return SnapshotOutput.builder()
            .id(asString(snapshot.get("id")))
            .name(asString(snapshot.get("name")))
            .resourceType(asString(snapshot.get("resource_type")))
            .resourceId(asString(snapshot.get("resource_id")))
            .regions(asStringList(snapshot.get("regions")))
            .minDiskSize(asInteger(snapshot.get("min_disk_size")))
            .sizeGigabytes(snapshot.get("size_gigabytes") instanceof Number number ? number.doubleValue() : null)
            .tags(asStringList(snapshot.get("tags")))
            .createdAt(createdAt != null ? Instant.parse(createdAt) : null)
            .build();
    }

    private static List<String> asStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return null;
        }
        return list.stream().map(String::valueOf).toList();
    }
}
