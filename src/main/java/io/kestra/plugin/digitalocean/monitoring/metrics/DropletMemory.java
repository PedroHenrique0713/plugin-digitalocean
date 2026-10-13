package io.kestra.plugin.digitalocean.monitoring.metrics;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.digitalocean.monitoring.models.MemoryMetricType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URI;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Query DigitalOcean droplet memory metrics",
    description = "Queries memory metrics (available, free, total, or cached) for a specific Droplet over a time window."
)
@Plugin(
    examples = {
        @Example(
            title = "Fetch available memory metrics for a Droplet",
            full = true,
            code = """
                id: digitalocean_get_droplet_memory_metrics
                namespace: company.team

                tasks:
                  - id: memory_metrics
                    type: io.kestra.plugin.digitalocean.monitoring.metrics.DropletMemory
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    hostId: "12345678"
                    metricType: MEMORY_AVAILABLE
                    start: "{{ now() | dateAdd(-1, 'HOURS') }}"
                    end: "{{ now() }}"
                """
        )
    }
)
public class DropletMemory extends AbstractDropletMetricsTask implements RunnableTask<MetricsOutput> {

    @Schema(title = "Memory metric type", description = "MEMORY_AVAILABLE, MEMORY_FREE, MEMORY_TOTAL, MEMORY_CACHED. Defaults to MEMORY_AVAILABLE.")
    @Builder.Default
    @PluginProperty(group = "main")
    private Property<MemoryMetricType> metricType = Property.ofValue(MemoryMetricType.MEMORY_AVAILABLE);

    @Override
    public MetricsOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rHostId = requireRendered(runContext, hostId, String.class, "hostId");
        var rMetricType = runContext.render(metricType).as(MemoryMetricType.class).orElse(MemoryMetricType.MEMORY_AVAILABLE);
        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        logger.info("Fetching {} metrics for Droplet {}", rMetricType, rHostId);

        var query = buildBaseQuery(runContext);
        var url = join(rBaseUrl, "v2/monitoring/metrics/droplet/" + encodePathSegment(rMetricType.getValue()) + "?" + query);
        var requestBuilder = HttpRequest.builder().uri(URI.create(url)).method("GET");
        var body = requestJson(runContext, options, rApiToken, requestBuilder);

        return MetricsOutput.from(body);
    }
}
