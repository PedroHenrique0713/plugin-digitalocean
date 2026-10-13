package io.kestra.plugin.digitalocean.monitoring.metrics;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.digitalocean.monitoring.models.NetworkInterface;
import io.kestra.plugin.digitalocean.monitoring.models.TrafficDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Query DigitalOcean droplet bandwidth metrics",
    description = "Queries bandwidth transfer rate metrics for a Droplet network interface over a time window."
)
@Plugin(
    examples = {
        @Example(
            title = "Fetch public outbound bandwidth metrics for a Droplet",
            full = true,
            code = """
                id: digitalocean_get_droplet_bandwidth_metrics
                namespace: company.team

                tasks:
                  - id: bandwidth_metrics
                    type: io.kestra.plugin.digitalocean.monitoring.metrics.DropletBandwidth
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    hostId: "12345678"
                    networkInterface: PUBLIC
                    direction: OUTBOUND
                    start: "{{ now() | dateAdd(-1, 'HOURS') }}"
                    end: "{{ now() }}"
                """
        )
    }
)
public class DropletBandwidth extends AbstractDropletMetricsTask implements RunnableTask<MetricsOutput> {

    @Schema(title = "Network interface", description = "PUBLIC or PRIVATE. Defaults to PUBLIC.")
    @Builder.Default
    @PluginProperty(group = "main")
    private Property<NetworkInterface> networkInterface = Property.ofValue(NetworkInterface.PUBLIC);

    @Schema(title = "Traffic direction", description = "INBOUND or OUTBOUND. Defaults to OUTBOUND.")
    @Builder.Default
    @PluginProperty(group = "main")
    private Property<TrafficDirection> direction = Property.ofValue(TrafficDirection.OUTBOUND);

    @Override
    public MetricsOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rHostId = requireRendered(runContext, hostId, String.class, "hostId");
        var rInterface = runContext.render(networkInterface).as(NetworkInterface.class).orElse(NetworkInterface.PUBLIC);
        var rDirection = runContext.render(direction).as(TrafficDirection.class).orElse(TrafficDirection.OUTBOUND);
        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        logger.info("Fetching {} {} bandwidth metrics for Droplet {}", rInterface, rDirection, rHostId);

        var baseQuery = buildBaseQuery(runContext);
        var query = baseQuery
            + "&interface=" + URLEncoder.encode(rInterface.getValue(), StandardCharsets.UTF_8)
            + "&direction=" + URLEncoder.encode(rDirection.getValue(), StandardCharsets.UTF_8);

        var url = join(rBaseUrl, "v2/monitoring/metrics/droplet/bandwidth?" + query);
        var requestBuilder = HttpRequest.builder().uri(URI.create(url)).method("GET");
        var body = requestJson(runContext, options, rApiToken, requestBuilder);

        return MetricsOutput.from(body);
    }
}
