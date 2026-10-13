package io.kestra.plugin.digitalocean.monitoring.metrics;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTask;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractDropletMetricsTask extends AbstractDigitalOceanTask {

    @Schema(title = "Droplet host ID")
    @NotNull
    @PluginProperty(group = "main")
    protected Property<String> hostId;

    @Schema(title = "Start timestamp (Instant or ISO-8601 string)")
    @NotNull
    @PluginProperty(group = "main")
    protected Property<Instant> start;

    @Schema(title = "End timestamp (Instant or ISO-8601 string)")
    @NotNull
    @PluginProperty(group = "main")
    protected Property<Instant> end;

    protected String buildBaseQuery(RunContext runContext) throws Exception {
        var rHostId = requireRendered(runContext, hostId, String.class, "hostId");
        var rStart = requireRendered(runContext, start, Instant.class, "start");
        var rEnd = requireRendered(runContext, end, Instant.class, "end");

        return "host_id=" + URLEncoder.encode(rHostId, StandardCharsets.UTF_8)
            + "&start=" + rStart.getEpochSecond()
            + "&end=" + rEnd.getEpochSecond();
    }
}
