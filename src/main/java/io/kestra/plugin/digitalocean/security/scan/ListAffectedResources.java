package io.kestra.plugin.digitalocean.security.scan;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanListTask;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List the resources affected by a DigitalOcean security finding",
    description = "Lists the resources a finding of a security (CSPM) scan applies to, each with its URN, name, and type, " +
        "following DigitalOcean's page-based pagination automatically."
)
@Plugin(
    examples = {
        @Example(
            title = "List the resources affected by a finding",
            full = true,
            code = """
                id: digitalocean_list_affected_resources
                namespace: company.team

                inputs:
                  - id: scan_id
                    type: STRING
                  - id: finding_id
                    type: STRING

                tasks:
                  - id: affected_resources
                    type: io.kestra.plugin.digitalocean.security.scan.ListAffectedResources
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    scanId: "{{ inputs.scan_id }}"
                    findingId: "{{ inputs.finding_id }}"
                  - id: log_resources
                    type: io.kestra.plugin.core.log.Log
                    message: "{{ outputs.affected_resources.total }} resource(s) affected, first one: {{ outputs.affected_resources.rows[0].urn }}"
                """
        )
    }
)
public class ListAffectedResources extends AbstractDigitalOceanListTask {

    @Schema(title = "Scan ID", description = "UUID of the scan the finding belongs to.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> scanId;

    @Schema(title = "Finding ID", description = "UUID of the finding to list the affected resources of.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> findingId;

    @Override
    protected String path(RunContext runContext) throws Exception {
        var rScanId = requireRendered(runContext, scanId, String.class, "scanId");
        var rFindingId = requireRendered(runContext, findingId, String.class, "findingId");

        return "v2/security/scans/" + encodePathSegment(rScanId) + "/findings/" + encodePathSegment(rFindingId) + "/affected_resources";
    }

    @Override
    protected String arrayKey() {
        return "affected_resources";
    }

    @Override
    protected String resourceLabel() {
        return "affected resource(s)";
    }
}
