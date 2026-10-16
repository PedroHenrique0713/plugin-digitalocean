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
import java.util.LinkedHashMap;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Update a DigitalOcean CDN endpoint",
    description = "Changes a CDN endpoint's cache TTL, custom domain, or certificate. Only the properties that are set are sent, so a custom domain can be set or changed but not cleared with this task; the origin cannot be changed."
)
@Plugin(
    examples = {
        @Example(
            title = "Shorten the cache TTL of a CDN endpoint to ten minutes",
            full = true,
            code = """
                id: digitalocean_update_cdn_endpoint
                namespace: company.team

                tasks:
                  - id: update_endpoint
                    type: io.kestra.plugin.digitalocean.cdn.Update
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    endpointId: 19f06b6a-3ace-4315-b086-499a0e521b76
                    ttl: 600
                """
        )
    }
)
public class Update extends AbstractDigitalOceanTask implements RunnableTask<EndpointOutput> {

    @Schema(title = "Endpoint ID", description = "ID of the CDN endpoint to update.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> endpointId;

    @Schema(
        title = "Cache TTL",
        description = "How long the edge servers cache the content, in seconds: 60, 600, 3600, 86400, or 604800."
    )
    @PluginProperty(group = "main")
    private Property<Integer> ttl;

    @Schema(title = "Custom domain", description = "Custom subdomain to serve the content from, e.g. static.example.com. Set certificateId with it.")
    @PluginProperty(group = "advanced")
    private Property<String> customDomain;

    @Schema(title = "Certificate ID", description = "ID of the DigitalOcean managed TLS certificate for the custom domain.")
    @PluginProperty(group = "advanced")
    private Property<String> certificateId;

    @Override
    public EndpointOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rEndpointId = requireRendered(runContext, endpointId, String.class, "endpointId");
        var rTtl = Create.validateTtl(runContext.render(ttl).as(Integer.class).orElse(null));
        var rCustomDomain = runContext.render(customDomain).as(String.class).orElse(null);
        var rCertificateId = runContext.render(certificateId).as(String.class).orElse(null);

        var payload = new LinkedHashMap<String, Object>();
        if (rTtl != null) {
            payload.put("ttl", rTtl);
        }
        if (rCustomDomain != null) {
            payload.put("custom_domain", rCustomDomain);
        }
        if (rCertificateId != null) {
            payload.put("certificate_id", rCertificateId);
        }
        if (payload.isEmpty()) {
            throw new IllegalArgumentException("Nothing to update: set ttl, customDomain, or certificateId");
        }

        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        logger.info("Updating DigitalOcean CDN endpoint {}", rEndpointId);

        var url = join(rBaseUrl, "v2/cdn/endpoints/" + encodePathSegment(rEndpointId));
        var requestBuilder = HttpRequest.builder()
            .uri(URI.create(url))
            .method("PUT")
            .body(HttpRequest.JsonRequestBody.of(payload));

        var body = requestJson(runContext, options, rApiToken, requestBuilder);

        return EndpointOutput.from(unwrap(body, "endpoint"));
    }
}
