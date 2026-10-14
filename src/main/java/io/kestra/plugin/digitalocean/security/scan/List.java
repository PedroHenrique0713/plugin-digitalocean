package io.kestra.plugin.digitalocean.security.scan;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanListTask;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List DigitalOcean security scans",
    description = "Lists the security (CSPM) scans of the account with their ID, status, and creation time, following " +
        "DigitalOcean's page-based pagination automatically. Findings are left out: read them with `Get` or `GetLatest`."
)
@Plugin(
    examples = {
        @Example(
            title = "List the scans and log how many ran",
            full = true,
            code = """
                id: digitalocean_list_security_scans
                namespace: company.team

                tasks:
                  - id: list_scans
                    type: io.kestra.plugin.digitalocean.security.scan.List
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                  - id: log_total
                    type: io.kestra.plugin.core.log.Log
                    message: "{{ outputs.list_scans.total }} scan(s), most recent status: {{ outputs.list_scans.rows[0].status }}"
                """
        )
    }
)
public class List extends AbstractDigitalOceanListTask {

    @Override
    protected String path(RunContext runContext) {
        return "v2/security/scans";
    }

    @Override
    protected String arrayKey() {
        return "scans";
    }

    @Override
    protected String resourceLabel() {
        return "security scan(s)";
    }

    // Each scan can carry up to a page of findings, so a list of scans would otherwise grow with scans x findings.
    @Override
    protected java.util.List<Map<String, Object>> transformRows(RunContext runContext, java.util.List<Map<String, Object>> rows) {
        return rows.stream().map(scan -> {
            var summary = new LinkedHashMap<>(scan);
            summary.remove("findings");
            return (Map<String, Object>) summary;
        }).toList();
    }
}
