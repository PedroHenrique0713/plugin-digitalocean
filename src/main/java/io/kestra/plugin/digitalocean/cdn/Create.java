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
import java.util.Set;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Create a DigitalOcean CDN endpoint",
    description = """
        Puts a CDN in front of a Spaces bucket. The origin is the bucket's hostname, e.g. \
        static-images.nyc3.digitaloceanspaces.com. Optionally sets the cache TTL and a custom subdomain, \
        which goes with the ID of a DigitalOcean managed certificate for it."""
)
@Plugin(
    examples = {
        @Example(
            title = "Enable the CDN on a Spaces bucket with a one-day cache",
            full = true,
            code = """
                id: digitalocean_create_cdn_endpoint
                namespace: company.team

                tasks:
                  - id: create_endpoint
                    type: io.kestra.plugin.digitalocean.cdn.Create
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    origin: static-images.nyc3.digitaloceanspaces.com
                    ttl: 86400
                  - id: log_endpoint
                    type: io.kestra.plugin.core.log.Log
                    message: "Content is now served from {{ outputs.create_endpoint.endpoint }}"
                """
        )
    }
)
public class Create extends AbstractDigitalOceanTask implements RunnableTask<EndpointOutput> {

    private static final Set<Integer> ALLOWED_TTLS = Set.of(60, 600, 3600, 86400, 604800);

    @Schema(title = "Origin", description = "Hostname of the Spaces bucket to serve, e.g. static-images.nyc3.digitaloceanspaces.com.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> origin;

    @Schema(
        title = "Cache TTL",
        description = "How long the edge servers cache the content, in seconds: 60, 600, 3600, 86400, or 604800. DigitalOcean defaults to 3600."
    )
    @PluginProperty(group = "main")
    private Property<Integer> ttl;

    @Schema(title = "Custom domain", description = "Custom subdomain to also serve the content from, e.g. static.example.com. Set certificateId with it.")
    @PluginProperty(group = "advanced")
    private Property<String> customDomain;

    @Schema(title = "Certificate ID", description = "ID of the DigitalOcean managed TLS certificate for the custom domain.")
    @PluginProperty(group = "advanced")
    private Property<String> certificateId;

    @Override
    public EndpointOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rOrigin = requireRendered(runContext, origin, String.class, "origin");
        var rTtl = validateTtl(runContext.render(ttl).as(Integer.class).orElse(null));
        var rCustomDomain = runContext.render(customDomain).as(String.class).orElse(null);
        var rCertificateId = runContext.render(certificateId).as(String.class).orElse(null);
        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        var payload = new LinkedHashMap<String, Object>();
        payload.put("origin", rOrigin);
        if (rTtl != null) {
            payload.put("ttl", rTtl);
        }
        if (rCustomDomain != null) {
            payload.put("custom_domain", rCustomDomain);
        }
        if (rCertificateId != null) {
            payload.put("certificate_id", rCertificateId);
        }

        logger.info("Creating DigitalOcean CDN endpoint for {}", rOrigin);

        var url = join(rBaseUrl, "v2/cdn/endpoints");
        var requestBuilder = HttpRequest.builder()
            .uri(URI.create(url))
            .method("POST")
            .body(HttpRequest.JsonRequestBody.of(payload));

        var body = requestJson(runContext, options, rApiToken, requestBuilder);

        return EndpointOutput.from(unwrap(body, "endpoint"));
    }

    static Integer validateTtl(Integer ttl) {
        if (ttl != null && !ALLOWED_TTLS.contains(ttl)) {
            throw new IllegalArgumentException("ttl must be one of 60, 600, 3600, 86400, or 604800 seconds, got " + ttl);
        }
        return ttl;
    }
}
