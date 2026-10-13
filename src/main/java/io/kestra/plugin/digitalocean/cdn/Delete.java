package io.kestra.plugin.digitalocean.cdn;

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
    title = "Delete a DigitalOcean CDN endpoint",
    description = "Deletes a CDN endpoint. The Spaces bucket behind it is left untouched and stays reachable at its own hostname."
)
@Plugin(
    examples = {
        @Example(
            title = "Delete a CDN endpoint",
            full = true,
            code = """
                id: digitalocean_delete_cdn_endpoint
                namespace: company.team

                tasks:
                  - id: delete_endpoint
                    type: io.kestra.plugin.digitalocean.cdn.Delete
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    endpointId: 19f06b6a-3ace-4315-b086-499a0e521b76
                """
        )
    }
)
public class Delete extends AbstractDigitalOceanTask implements RunnableTask<VoidOutput> {

    @Schema(title = "Endpoint ID", description = "ID of the CDN endpoint to delete.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> endpointId;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rEndpointId = requireRendered(runContext, endpointId, String.class, "endpointId");
        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        logger.info("Deleting DigitalOcean CDN endpoint {}", rEndpointId);

        var url = join(rBaseUrl, "v2/cdn/endpoints/" + encodePathSegment(rEndpointId));
        var requestBuilder = HttpRequest.builder().uri(URI.create(url)).method("DELETE");
        request(runContext, options, rApiToken, requestBuilder, String.class);
        return null;
    }
}
