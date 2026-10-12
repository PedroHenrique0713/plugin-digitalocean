package io.kestra.plugin.digitalocean.security.settings;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTask;
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
    title = "Remove a DigitalOcean security suppression",
    description = "Removes a rule suppression, so later security (CSPM) scans report the rule's findings on that resource again."
)
@Plugin(
    examples = {
        @Example(
            title = "Remove a suppression",
            full = true,
            code = """
                id: digitalocean_remove_suppression
                namespace: company.team

                tasks:
                  - id: remove_suppression
                    type: io.kestra.plugin.digitalocean.security.settings.DeleteSuppression
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    suppressionId: "5b3b2b2d-5c9c-4a61-9e2f-4d8f80f30a12"
                """
        )
    }
)
public class DeleteSuppression extends AbstractDigitalOceanTask implements RunnableTask<VoidOutput> {

    @Schema(title = "Suppression ID", description = "UUID of the suppression to remove, the `id` of a suppression returned by `Get` or `CreateSuppression`.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> suppressionId;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rSuppressionId = requireRendered(runContext, suppressionId, String.class, "suppressionId");
        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        logger.info("Removing DigitalOcean security suppression {}", rSuppressionId);

        var url = join(rBaseUrl, "v2/security/settings/suppressions/" + encodePathSegment(rSuppressionId));
        var requestBuilder = HttpRequest.builder().uri(URI.create(url)).method("DELETE");
        request(runContext, options, rApiToken, requestBuilder, String.class);

        return null;
    }
}
