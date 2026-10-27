package io.kestra.plugin.digitalocean.security.settings;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTask;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Suppress a DigitalOcean security rule on resources",
    description = "Suppresses the findings of a security (CSPM) rule on the given resources, so later scans stop " +
        "reporting them, and returns the suppressions created."
)
@Plugin(
    examples = {
        @Example(
            title = "Suppress a rule on one droplet",
            full = true,
            code = """
                id: digitalocean_suppress_rule
                namespace: company.team

                inputs:
                  - id: rule_id
                    type: STRING
                  - id: droplet_urn
                    type: STRING

                tasks:
                  - id: suppress
                    type: io.kestra.plugin.digitalocean.security.settings.CreateSuppression
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    ruleId: "{{ inputs.rule_id }}"
                    resources:
                      - "{{ inputs.droplet_urn }}"
                  - id: log_suppression
                    type: io.kestra.plugin.core.log.Log
                    message: "Suppressed {{ outputs.suppress.suppressions[0].rule_name }} as {{ outputs.suppress.suppressions[0].id }}"
                """
        )
    }
)
public class CreateSuppression extends AbstractDigitalOceanTask implements RunnableTask<CreateSuppression.Output> {

    @Schema(title = "Rule ID", description = "UUID of the rule to suppress, e.g. the `rule_uuid` of a finding returned by `security.scan.Get`.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> ruleId;

    @Schema(title = "Resources", description = "URNs of the resources to suppress the rule for, e.g. do:droplet:fe3a2fd7-903d-46e6-ada3-3e4f285fb89d.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<List<String>> resources;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rRuleId = requireRendered(runContext, ruleId, String.class, "ruleId");
        var rResources = runContext.render(resources).asList(String.class);
        if (rResources.isEmpty()) {
            throw new IllegalArgumentException("resources needs at least one resource URN");
        }
        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        var payload = new LinkedHashMap<String, Object>();
        payload.put("rule_uuid", rRuleId);
        payload.put("resources", rResources);

        logger.info("Suppressing DigitalOcean security rule {} on {} resource(s)", rRuleId, rResources.size());

        var url = join(rBaseUrl, "v2/security/settings/suppressions");
        var requestBuilder = HttpRequest.builder()
            .uri(URI.create(url))
            .method("POST")
            .body(HttpRequest.JsonRequestBody.of(payload));

        var suppressions = SuppressionPage.from(requestJson(runContext, options, rApiToken, requestBuilder));

        return Output.builder()
            .suppressions(suppressions.rows())
            .total(suppressions.total())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {

        @Schema(
            title = "Suppressions",
            description = "Suppressions created, each with `id`, `rule_uuid`, `rule_name`, `resource_id`, and `resource_type`."
        )
        private final List<Map<String, Object>> suppressions;

        @Schema(title = "Total suppressions", description = "Number of suppressions reported by the DigitalOcean API (meta.total).")
        private final Long total;
    }
}
