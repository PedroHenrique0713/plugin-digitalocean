package io.kestra.plugin.digitalocean.monitoring.alertpolicy;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
    title = "Update a DigitalOcean monitoring alert policy",
    description = "Updates an existing alert policy by its UUID."
)
@Plugin(
    examples = {
        @Example(
            title = "Update threshold and description on an existing alert policy",
            full = true,
            code = """
                id: digitalocean_update_alert_policy
                namespace: company.team

                tasks:
                  - id: update_alert
                    type: io.kestra.plugin.digitalocean.monitoring.alertpolicy.Update
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    alertUuid: "7343e061-e0c1-4b71-a476-c56dfaa3d2ad"
                    policyType: CPU
                    policyDescription: "Updated high CPU usage alert"
                    compare: GREATER_THAN
                    value: 90.0
                    window: TEN_MINUTES
                    alerts:
                      email:
                        - "devops@company.com"
                """
        )
    }
)
public class Update extends AbstractAlertPolicyTask implements RunnableTask<AlertPolicyOutput> {

    @Schema(title = "Alert policy UUID")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> alertUuid;

    @Override
    public AlertPolicyOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rAlertUuid = requireRendered(runContext, alertUuid, String.class, "alertUuid");
        var payload = buildPayload(runContext);

        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        logger.info("Updating DigitalOcean alert policy {}", rAlertUuid);

        var url = join(rBaseUrl, "v2/monitoring/alerts/" + encodePathSegment(rAlertUuid));
        var requestBuilder = HttpRequest.builder()
            .uri(URI.create(url))
            .method("PUT")
            .body(HttpRequest.JsonRequestBody.builder().content(payload).build());

        var body = requestJson(runContext, options, rApiToken, requestBuilder);

        return AlertPolicyOutput.from(unwrap(body, "policy"));
    }
}
