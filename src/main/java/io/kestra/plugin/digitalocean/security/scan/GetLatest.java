package io.kestra.plugin.digitalocean.security.scan;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
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
    title = "Get the latest DigitalOcean security scan",
    description = "Reads the most recent security (CSPM) scan of the account, with its status and one page of findings, " +
        "optionally filtered by severity or type. The status is CSPM_NOT_ENABLED or SCAN_NOT_RUN when there is no scan to read."
)
@Plugin(
    examples = {
        @Example(
            title = "Fail a daily check when the latest scan has critical findings",
            full = true,
            code = """
                id: digitalocean_check_critical_findings
                namespace: company.team

                tasks:
                  - id: latest_scan
                    type: io.kestra.plugin.digitalocean.security.scan.GetLatest
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    severity: CRITICAL
                  - id: fail_on_critical
                    type: io.kestra.plugin.core.execution.Fail
                    condition: "{{ outputs.latest_scan.findings | length > 0 }}"
                    errorMessage: "{{ outputs.latest_scan.findings | length }} critical finding(s), first one: {{ outputs.latest_scan.findings[0].name }}"

                triggers:
                  - id: every_morning
                    type: io.kestra.plugin.core.trigger.Schedule
                    cron: "0 7 * * *"
                """
        )
    }
)
public class GetLatest extends AbstractGetScanTask {

    @Override
    protected String path(RunContext runContext) {
        return "v2/security/scans/latest";
    }
}
