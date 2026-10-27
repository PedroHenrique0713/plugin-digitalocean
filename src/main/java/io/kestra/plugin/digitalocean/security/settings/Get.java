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
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Get the DigitalOcean security settings",
    description = "Reads the security (CSPM) settings of the account: the resources and tags each plan tier scans, " +
        "the coverage downgrades scheduled, and one page of the rule suppressions."
)
@Plugin(
    examples = {
        @Example(
            title = "Log the active rule suppressions",
            full = true,
            code = """
                id: digitalocean_security_settings
                namespace: company.team

                tasks:
                  - id: settings
                    type: io.kestra.plugin.digitalocean.security.settings.Get
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                  - id: log_suppressions
                    type: io.kestra.plugin.core.log.Log
                    message: "{{ outputs.settings.suppressionsTotal }} suppression(s), first rule: {{ outputs.settings.suppressions[0].rule_name }}"
                """
        )
    }
)
public class Get extends AbstractDigitalOceanTask implements RunnableTask<Get.Output> {

    @Schema(title = "Suppressions per page", description = "Number of suppressions requested. Defaults to 200, the maximum allowed by the DigitalOcean API.")
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<Integer> perPage = Property.ofValue(200);

    @Schema(
        title = "Suppressions page",
        description = "Page of suppressions to read, starting at 1. DigitalOcean paginates the suppressions inside the " +
            "settings, so an account with more suppressions than `perPage` needs one call per page."
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<Integer> page = Property.ofValue(1);

    @Override
    public Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rPerPage = requireInRange("perPage", runContext.render(perPage).as(Integer.class).orElse(200), 1, 200);
        var rPage = requireInRange("page", runContext.render(page).as(Integer.class).orElse(1), 1, Integer.MAX_VALUE);
        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        logger.info("Fetching DigitalOcean security settings");

        var url = join(rBaseUrl, "v2/security/settings?per_page=" + rPerPage + "&page=" + rPage);
        var requestBuilder = HttpRequest.builder().uri(URI.create(url)).method("GET");
        var body = requestJson(runContext, options, rApiToken, requestBuilder);

        var settings = asMap(body.get("settings"));
        var suppressions = SuppressionPage.from(settings != null ? asMap(settings.get("suppressions")) : null);

        logger.info("Read {} of {} suppression(s) on page {}", suppressions.rows().size(), suppressions.total(), rPage);

        return Output.builder()
            .tierCoverage(Objects.requireNonNullElse(asMap(body.get("tier_coverage")), Map.of()))
            .planDowngrades(Objects.requireNonNullElse(asMap(body.get("plan_downgrades")), Map.of()))
            .suppressions(suppressions.rows())
            .suppressionsTotal(suppressions.total())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {

        @Schema(title = "Tier coverage", description = "Resources and tags scanned by each plan tier, keyed by tier, each with `resources` (URNs) and `tags`.")
        private final Map<String, Object> tierCoverage;

        @Schema(title = "Plan downgrades", description = "Coverage downgrades scheduled, keyed by tier, each with `resources` (URNs) and `effective_at`.")
        private final Map<String, Object> planDowngrades;

        @Schema(
            title = "Suppressions",
            description = "One page of rule suppressions, each with `id`, `rule_uuid`, `rule_name`, `resource_id`, and `resource_type`."
        )
        private final List<Map<String, Object>> suppressions;

        @Schema(title = "Total suppressions", description = "Total number of suppressions reported by the DigitalOcean API (meta.total), across all pages.")
        private final Long suppressionsTotal;
    }
}
