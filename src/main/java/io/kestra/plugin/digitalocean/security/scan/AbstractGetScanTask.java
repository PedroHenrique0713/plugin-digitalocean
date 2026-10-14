package io.kestra.plugin.digitalocean.security.scan;

import io.kestra.core.http.HttpRequest;
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
import java.util.ArrayList;

/**
 * Shared behavior for the two "read one scan" tasks: DigitalOcean serves a scan by ID and the latest scan
 * from different paths, with the same finding filters and the same response shape.
 */
@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractGetScanTask extends AbstractDigitalOceanTask implements RunnableTask<ScanOutput> {

    @Schema(title = "Severity", description = "Only include findings with this severity: LOW, MEDIUM, HIGH, or CRITICAL.")
    @PluginProperty(group = "main")
    protected Property<Severity> severity;

    @Schema(title = "Finding type", description = "Only include findings of this type.")
    @PluginProperty(group = "main")
    protected Property<String> findingType;

    @Schema(title = "Findings per page", description = "Number of findings requested. Defaults to 200, the maximum allowed by the DigitalOcean API.")
    @Builder.Default
    @PluginProperty(group = "processing")
    protected Property<Integer> perPage = Property.ofValue(200);

    @Schema(
        title = "Findings page",
        description = "Page of findings to read, starting at 1. DigitalOcean paginates the findings inside the scan, " +
            "so a scan with more findings than `perPage` needs one call per page."
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    protected Property<Integer> page = Property.ofValue(1);

    @Override
    public final ScanOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rPerPage = requireInRange("perPage", runContext.render(perPage).as(Integer.class).orElse(200), 1, 200);
        var rPage = requireInRange("page", runContext.render(page).as(Integer.class).orElse(1), 1, Integer.MAX_VALUE);
        var rPath = path(runContext);
        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        var query = new ArrayList<String>();
        query.add("per_page=" + rPerPage);
        query.add("page=" + rPage);
        runContext.render(severity).as(Severity.class).ifPresent(v -> query.add("severity=" + v.name()));
        runContext.render(findingType).as(String.class).ifPresent(v -> query.add("type=" + encodePathSegment(v)));

        logger.info("Fetching DigitalOcean security scan from {}", rPath);

        var url = join(rBaseUrl, rPath + "?" + String.join("&", query));
        var requestBuilder = HttpRequest.builder().uri(URI.create(url)).method("GET");
        var body = requestJson(runContext, options, rApiToken, requestBuilder);
        var output = ScanOutput.from(unwrap(body, "scan"));

        logger.info("Scan {} is {} with {} finding(s) on page {}", output.getId(), output.getStatus(), output.getFindings().size(), rPage);

        return output;
    }

    /** Endpoint path relative to the API base, without the query string. */
    protected abstract String path(RunContext runContext) throws Exception;
}
