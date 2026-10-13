package io.kestra.plugin.digitalocean.monitoring.metrics;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.digitalocean.monitoring.models.DiskMetricType;
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
    title = "Query DigitalOcean droplet disk metrics",
    description = "Queries disk and filesystem metrics (FILESYSTEM_FREE, FILESYSTEM_SIZE, DISK_READ, DISK_WRITE) for a specific Droplet over a time window."
)
@Plugin(
    examples = {
        @Example(
            title = "Fetch free filesystem space metrics for a Droplet",
            full = true,
            code = """
                id: digitalocean_get_droplet_disk_metrics
                namespace: company.team

                tasks:
                  - id: disk_metrics
                    type: io.kestra.plugin.digitalocean.monitoring.metrics.DropletDisk
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    hostId: "12345678"
                    metricType: FILESYSTEM_FREE
                    start: "{{ now() | dateAdd(-1, 'HOURS') }}"
                    end: "{{ now() }}"
                """
        )
    }
)
public class DropletDisk extends AbstractDropletMetricsTask implements RunnableTask<MetricsOutput> {

    @Schema(title = "Disk metric type", description = "FILESYSTEM_FREE, FILESYSTEM_SIZE, DISK_READ, DISK_WRITE. Defaults to FILESYSTEM_FREE.")
    @Builder.Default
    @PluginProperty(group = "main")
    private Property<DiskMetricType> metricType = Property.ofValue(DiskMetricType.FILESYSTEM_FREE);

    @Override
    public MetricsOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rHostId = requireRendered(runContext, hostId, String.class, "hostId");
        var rMetricType = runContext.render(metricType).as(DiskMetricType.class).orElse(DiskMetricType.FILESYSTEM_FREE);
        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        logger.info("Fetching {} disk metrics for Droplet {}", rMetricType, rHostId);

        var query = buildBaseQuery(runContext);
        var url = join(rBaseUrl, "v2/monitoring/metrics/droplet/" + encodePathSegment(rMetricType.getValue()) + "?" + query);
        var requestBuilder = HttpRequest.builder().uri(URI.create(url)).method("GET");
        var body = requestJson(runContext, options, rApiToken, requestBuilder);

        return MetricsOutput.from(body);
    }
}
