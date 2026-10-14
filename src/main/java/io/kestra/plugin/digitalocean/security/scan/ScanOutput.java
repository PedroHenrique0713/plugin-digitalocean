package io.kestra.plugin.digitalocean.security.scan;

import io.kestra.core.models.tasks.Output;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTask;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.kestra.plugin.digitalocean.AbstractDigitalOceanTask.asString;

@Builder
@Getter
public class ScanOutput implements Output {

    @Schema(title = "Scan ID")
    private final String id;

    @Schema(title = "Scan status", description = "One of IN_PROGRESS, COMPLETED, FAILED, CSPM_NOT_ENABLED, or SCAN_NOT_RUN.")
    private final String status;

    @Schema(title = "Creation timestamp")
    private final Instant createdAt;

    @Schema(
        title = "Findings",
        description = "Findings as returned by DigitalOcean, each with `rule_uuid`, `name`, `severity`, `details`, " +
            "`mitigation_steps`, and `affected_resources_count`. Holds at most one page of findings."
    )
    private final List<Map<String, Object>> findings;

    public static ScanOutput from(Map<String, Object> scan) {
        var createdAt = asString(scan.get("created_at"));

        return ScanOutput.builder()
            .id(asString(scan.get("id")))
            .status(asString(scan.get("status")))
            .createdAt(createdAt != null ? Instant.parse(createdAt) : null)
            .findings(findings(scan.get("findings")))
            .build();
    }

    static List<Map<String, Object>> findings(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
            .map(AbstractDigitalOceanTask::asMap)
            .filter(Objects::nonNull)
            .toList();
    }
}
