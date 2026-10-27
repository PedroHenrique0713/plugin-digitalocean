package io.kestra.plugin.digitalocean.security.scan;

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
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Exclude a resource from DigitalOcean security scans",
    description = """
        Creates a scan rule that excludes a resource from future security (CSPM) scans, e.g. to mark the \
        findings on it as false positives."""
)
@Plugin(
    examples = {
        @Example(
            title = "Exclude a droplet from future scans",
            full = true,
            code = """
                id: digitalocean_exclude_from_scans
                namespace: company.team

                tasks:
                  - id: exclude_droplet
                    type: io.kestra.plugin.digitalocean.security.scan.CreateRule
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    resource: "do:droplet:fe3a2fd7-903d-46e6-ada3-3e4f285fb89d"
                """
        )
    }
)
public class CreateRule extends AbstractDigitalOceanTask implements RunnableTask<VoidOutput> {

    @Schema(title = "Resource", description = "URN of the resource to exclude from future scans, e.g. do:droplet:fe3a2fd7-903d-46e6-ada3-3e4f285fb89d.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> resource;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rResource = requireRendered(runContext, resource, String.class, "resource");
        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        logger.info("Excluding {} from DigitalOcean security scans", rResource);

        var url = join(rBaseUrl, "v2/security/scans/rules");
        var requestBuilder = HttpRequest.builder()
            .uri(URI.create(url))
            .method("POST")
            .body(HttpRequest.JsonRequestBody.of(Map.of("resource", rResource)));
        request(runContext, options, rApiToken, requestBuilder, String.class);

        return null;
    }
}
