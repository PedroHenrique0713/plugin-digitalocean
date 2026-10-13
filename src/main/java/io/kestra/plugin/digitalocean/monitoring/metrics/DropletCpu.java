package io.kestra.plugin.digitalocean.monitoring.metrics;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
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
    title = "Query DigitalOcean droplet CPU metrics",
    description = "Queries historical CPU metrics (idle, user, system, iowait, etc.) for a specific Droplet over a time window."
)
@Plugin(
    examples = {
        @Example(
            title = "Fetch CPU metrics for a Droplet over the last hour",
            full = true,
            code = """
                id: digitalocean_get_droplet_cpu_metrics
                namespace: company.team

                tasks:
                  - id: cpu_metrics
                    type: io.kestra.plugin.digitalocean.monitoring.metrics.DropletCpu
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    hostId: "12345678"
                    start: "{{ now() | dateAdd(-1, 'HOURS') }}"
                    end: "{{ now() }}"
                """
        )
    }
)
public class DropletCpu extends AbstractDropletMetricsTask implements RunnableTask<MetricsOutput> {

    @Override
    public MetricsOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rHostId = requireRendered(runContext, hostId, String.class, "hostId");
        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        logger.info("Fetching CPU metrics for Droplet {}", rHostId);

        var query = buildBaseQuery(runContext);
        var url = join(rBaseUrl, "v2/monitoring/metrics/droplet/cpu?" + query);
        var requestBuilder = HttpRequest.builder().uri(URI.create(url)).method("GET");
        var body = requestJson(runContext, options, rApiToken, requestBuilder);

        return MetricsOutput.from(body);
    }
}
