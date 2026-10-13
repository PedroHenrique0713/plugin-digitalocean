package io.kestra.plugin.digitalocean.cdn;

import io.kestra.core.models.tasks.Output;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

import static io.kestra.plugin.digitalocean.AbstractDigitalOceanTask.asInteger;
import static io.kestra.plugin.digitalocean.AbstractDigitalOceanTask.asString;

/** Shared output shape for {@link Get}, {@link Create}, and {@link Update}: all three return the same endpoint JSON object. */
@Builder
@Getter
public class EndpointOutput implements Output {

    @Schema(title = "Endpoint ID", description = "UUID assigned by DigitalOcean.")
    private final String id;

    @Schema(title = "Origin", description = "Hostname of the Spaces bucket the CDN serves, e.g. static-images.nyc3.digitaloceanspaces.com.")
    private final String origin;

    @Schema(title = "CDN hostname", description = "Hostname the cached content is served from, e.g. static-images.nyc3.cdn.digitaloceanspaces.com.")
    private final String endpoint;

    @Schema(title = "Cache TTL", description = "How long the edge servers cache the content, in seconds.")
    private final Integer ttl;

    @Schema(title = "Certificate ID", description = "ID of the DigitalOcean managed TLS certificate used for the custom domain.")
    private final String certificateId;

    @Schema(title = "Custom domain", description = "Custom subdomain the content is also served from, e.g. static.example.com.")
    private final String customDomain;

    @Schema(title = "Creation timestamp")
    private final Instant createdAt;

    public static EndpointOutput from(Map<String, Object> endpoint) {
        var createdAt = asString(endpoint.get("created_at"));

        return EndpointOutput.builder()
            .id(asString(endpoint.get("id")))
            .origin(asString(endpoint.get("origin")))
            .endpoint(asString(endpoint.get("endpoint")))
            .ttl(asInteger(endpoint.get("ttl")))
            .certificateId(asString(endpoint.get("certificate_id")))
            .customDomain(asString(endpoint.get("custom_domain")))
            .createdAt(createdAt != null ? Instant.parse(createdAt) : null)
            .build();
    }
}
