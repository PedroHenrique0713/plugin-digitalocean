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
import java.util.Map;
import java.util.Objects;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Update the DigitalOcean security plan coverage",
    description = "Sets the resources and tags that each security (CSPM) plan tier scans, and returns the coverage DigitalOcean applied."
)
@Plugin(
    examples = {
        @Example(
            title = "Scan production resources and one droplet on the basic tier",
            full = true,
            code = """
                id: digitalocean_security_plan
                namespace: company.team

                tasks:
                  - id: update_plan
                    type: io.kestra.plugin.digitalocean.security.settings.UpdatePlan
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    tierCoverage:
                      basic:
                        tags:
                          - production
                        resources:
                          - do:droplet:fe3a2fd7-903d-46e6-ada3-3e4f285fb89d
                """
        )
    }
)
public class UpdatePlan extends AbstractDigitalOceanTask implements RunnableTask<UpdatePlan.Output> {

    @Schema(
        title = "Tier coverage",
        description = "Coverage per plan tier, keyed by tier name (e.g. basic), each with the `resources` (URNs) and " +
            "`tags` to scan. A list left out is sent empty."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<Map<String, TierCoverage>> tierCoverage;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rTierCoverage = runContext.render(tierCoverage).asMap(String.class, TierCoverage.class);
        if (rTierCoverage == null || rTierCoverage.isEmpty()) {
            throw new IllegalArgumentException("tierCoverage needs at least one tier, e.g. basic");
        }
        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        var coverage = new LinkedHashMap<String, Object>();
        rTierCoverage.forEach((tier, value) -> coverage.put(tier, (value != null ? value : new TierCoverage()).toMap()));

        logger.info("Updating DigitalOcean security plan coverage for tier(s) {}", coverage.keySet());

        var url = join(rBaseUrl, "v2/security/settings/plan");
        var requestBuilder = HttpRequest.builder()
            .uri(URI.create(url))
            .method("PUT")
            .body(HttpRequest.JsonRequestBody.of(Map.of("tier_coverage", coverage)));

        var body = requestJson(runContext, options, rApiToken, requestBuilder);

        return Output.builder()
            .tierCoverage(Objects.requireNonNullElse(asMap(body.get("tier_coverage")), Map.of()))
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {

        @Schema(title = "Tier coverage", description = "Coverage DigitalOcean applied, keyed by tier, each with `resources` (URNs) and `tags`.")
        private final Map<String, Object> tierCoverage;
    }
}
