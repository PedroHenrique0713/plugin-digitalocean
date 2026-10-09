package io.kestra.plugin.digitalocean.security.scan;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTask;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URI;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Start a DigitalOcean security scan",
    description = "Starts a security (CSPM) scan of the account and returns it without waiting for it to finish. " +
        "Poll `Get` with the returned `id` to follow the scan until its status leaves IN_PROGRESS."
)
@Plugin(
    examples = {
        @Example(
            title = "Start a scan every Monday",
            full = true,
            code = """
                id: digitalocean_weekly_security_scan
                namespace: company.team

                tasks:
                  - id: start_scan
                    type: io.kestra.plugin.digitalocean.security.scan.Create
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                  - id: log_scan
                    type: io.kestra.plugin.core.log.Log
                    message: "Scan {{ outputs.start_scan.id }} is {{ outputs.start_scan.status }}"

                triggers:
                  - id: every_monday
                    type: io.kestra.plugin.core.trigger.Schedule
                    cron: "0 6 * * 1"
                """
        )
    }
)
public class Create extends AbstractDigitalOceanTask implements RunnableTask<ScanOutput> {

    @Override
    public ScanOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        logger.info("Starting a DigitalOcean security scan");

        var url = join(rBaseUrl, "v2/security/scans");
        var requestBuilder = HttpRequest.builder()
            .uri(URI.create(url))
            .method("POST")
            .body(HttpRequest.JsonRequestBody.of(Map.of()));

        var body = requestJson(runContext, options, rApiToken, requestBuilder);
        var output = ScanOutput.from(unwrap(body, "scan"));

        logger.info("Started security scan {} with status {}", output.getId(), output.getStatus());

        return output;
    }
}
