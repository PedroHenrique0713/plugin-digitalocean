package io.kestra.plugin.digitalocean.cdn;

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
    title = "Get a DigitalOcean CDN endpoint",
    description = "Reads a CDN endpoint's origin, CDN hostname, cache TTL, and custom domain."
)
@Plugin(
    examples = {
        @Example(
            title = "Get a CDN endpoint and log the hostname it serves from",
            full = true,
            code = """
                id: digitalocean_get_cdn_endpoint
                namespace: company.team

                tasks:
                  - id: get_endpoint
                    type: io.kestra.plugin.digitalocean.cdn.Get
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    endpointId: 19f06b6a-3ace-4315-b086-499a0e521b76
                  - id: log_endpoint
                    type: io.kestra.plugin.core.log.Log
                    message: "{{ outputs.get_endpoint.origin }} is served from {{ outputs.get_endpoint.endpoint }}"
                """
        )
    }
)
public class Get extends AbstractDigitalOceanTask implements RunnableTask<EndpointOutput> {

    @Schema(title = "Endpoint ID", description = "ID of the CDN endpoint to read.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> endpointId;

    @Override
    public EndpointOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rEndpointId = requireRendered(runContext, endpointId, String.class, "endpointId");
        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        logger.info("Fetching DigitalOcean CDN endpoint {}", rEndpointId);

        var url = join(rBaseUrl, "v2/cdn/endpoints/" + encodePathSegment(rEndpointId));
        var requestBuilder = HttpRequest.builder().uri(URI.create(url)).method("GET");
        var body = requestJson(runContext, options, rApiToken, requestBuilder);

        return EndpointOutput.from(unwrap(body, "endpoint"));
    }
}
