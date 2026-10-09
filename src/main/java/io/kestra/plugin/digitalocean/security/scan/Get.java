package io.kestra.plugin.digitalocean.security.scan;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
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
    title = "Get a DigitalOcean security scan",
    description = "Reads a security (CSPM) scan by ID, with its status and one page of findings, optionally filtered by severity or type."
)
@Plugin(
    examples = {
        @Example(
            title = "Start a scan and wait until it completes",
            full = true,
            code = """
                id: digitalocean_run_security_scan
                namespace: company.team

                tasks:
                  - id: start_scan
                    type: io.kestra.plugin.digitalocean.security.scan.Create
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                  - id: wait_for_scan
                    type: io.kestra.plugin.core.flow.LoopUntil
                    condition: "{{ outputs.get_scan.status != 'IN_PROGRESS' }}"
                    checkFrequency:
                      interval: PT30S
                      maxDuration: PT30M
                    tasks:
                      - id: get_scan
                        type: io.kestra.plugin.digitalocean.security.scan.Get
                        apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                        scanId: "{{ outputs.start_scan.id }}"
                        severity: CRITICAL
                """
        )
    }
)
public class Get extends AbstractGetScanTask {

    @Schema(title = "Scan ID", description = "UUID of the scan to read.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> scanId;

    @Override
    protected String path(RunContext runContext) throws Exception {
        return "v2/security/scans/" + encodePathSegment(requireRendered(runContext, scanId, String.class, "scanId"));
    }
}
