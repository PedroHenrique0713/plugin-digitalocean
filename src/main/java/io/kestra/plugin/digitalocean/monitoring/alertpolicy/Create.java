package io.kestra.plugin.digitalocean.monitoring.alertpolicy;

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
    title = "Create a DigitalOcean monitoring alert policy",
    description = "Creates a new alert policy on DigitalOcean for Droplets or other resources."
)
@Plugin(
    examples = {
        @Example(
            title = "Create a CPU utilization alert policy notifying email and Slack",
            full = true,
            code = """
                id: digitalocean_create_alert_policy
                namespace: company.team

                tasks:
                  - id: create_alert
                    type: io.kestra.plugin.digitalocean.monitoring.alertpolicy.Create
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    policyType: CPU
                    policyDescription: "High CPU usage on production droplets"
                    compare: GREATER_THAN
                    value: 85.0
                    window: FIVE_MINUTES
                    entities:
                      - "12345678"
                    alerts:
                      email:
                        - "ops@company.com"
                      slack:
                        - url: "{{ secret('SLACK_WEBHOOK_URL') }}"
                          channel: "#alerts"
                """
        )
    }
)
public class Create extends AbstractAlertPolicyTask implements RunnableTask<AlertPolicyOutput> {

    @Override
    public AlertPolicyOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var payload = buildPayload(runContext);
        var rDescription = (String) payload.get("description");

        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        logger.info("Creating DigitalOcean alert policy '{}'", rDescription);

        var url = join(rBaseUrl, "v2/monitoring/alerts");
        var requestBuilder = HttpRequest.builder()
            .uri(URI.create(url))
            .method("POST")
            .body(HttpRequest.JsonRequestBody.builder().content(payload).build());

        var body = requestJson(runContext, options, rApiToken, requestBuilder);

        return AlertPolicyOutput.from(unwrap(body, "policy"));
    }
}
