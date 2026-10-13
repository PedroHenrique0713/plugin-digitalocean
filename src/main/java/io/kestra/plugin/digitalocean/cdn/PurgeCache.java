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
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Purge the cache of a DigitalOcean CDN endpoint",
    description = """
        Removes cached files from a CDN endpoint so the next request fetches them again from the Spaces bucket, \
        e.g. right after uploading a new version. A path can name one file or end with a wildcard (`css/*`) to \
        purge everything under it; `*` alone purges the whole cache. DigitalOcean purges at most 50 files per \
        20 seconds, and a wildcard path counts as one."""
)
@Plugin(
    examples = {
        @Example(
            title = "Purge the stylesheets and the home page after a deploy",
            full = true,
            code = """
                id: digitalocean_purge_cdn_cache
                namespace: company.team

                tasks:
                  - id: purge_cache
                    type: io.kestra.plugin.digitalocean.cdn.PurgeCache
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    endpointId: 19f06b6a-3ace-4315-b086-499a0e521b76
                    files:
                      - "css/*"
                      - index.html
                """
        )
    }
)
public class PurgeCache extends AbstractDigitalOceanTask implements RunnableTask<VoidOutput> {

    @Schema(title = "Endpoint ID", description = "ID of the CDN endpoint whose cache to purge.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> endpointId;

    @Schema(
        title = "Paths to purge",
        description = "Paths of the cached files, relative to the bucket, e.g. index.html or css/*. Use * to purge the whole cache."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<List<String>> files;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rEndpointId = requireRendered(runContext, endpointId, String.class, "endpointId");
        @SuppressWarnings("unchecked")
        var rFiles = (List<String>) runContext.render(files).asList(String.class);
        if (rFiles.isEmpty()) {
            throw new IllegalArgumentException("files must list at least one path; use * to purge the whole cache");
        }
        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        logger.info("Purging {} path(s) from DigitalOcean CDN endpoint {}", rFiles.size(), rEndpointId);

        var url = join(rBaseUrl, "v2/cdn/endpoints/" + encodePathSegment(rEndpointId) + "/cache");
        var requestBuilder = HttpRequest.builder()
            .uri(URI.create(url))
            .method("DELETE")
            .body(HttpRequest.JsonRequestBody.of(Map.of("files", rFiles)));
        request(runContext, options, rApiToken, requestBuilder, String.class);
        return null;
    }
}
