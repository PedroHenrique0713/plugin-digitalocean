package io.kestra.plugin.digitalocean.monitoring.alertpolicy;

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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List DigitalOcean monitoring alert policies",
    description = "Lists all alert policies on the account, following DigitalOcean's page-based pagination automatically. " +
        "Sensitive Slack incoming webhook URLs are masked/stripped from outputs."
)
@Plugin(
    examples = {
        @Example(
            title = "List alert policies and log the total count",
            full = true,
            code = """
                id: digitalocean_list_alert_policies
                namespace: company.team

                tasks:
                  - id: list_policies
                    type: io.kestra.plugin.digitalocean.monitoring.alertpolicy.List
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                  - id: log_count
                    type: io.kestra.plugin.core.log.Log
                    message: "Found {{ outputs.list_policies.total }} alert policy(ies)"
                """
        )
    }
)
public class List extends AbstractDigitalOceanListTask {

    @Override
    protected String path(RunContext runContext) {
        return "v2/monitoring/alerts";
    }

    @Override
    protected String arrayKey() {
        return "policies";
    }

    @Override
    protected String resourceLabel() {
        return "alert policy(ies)";
    }

    @Override
    @SuppressWarnings("unchecked")
    protected java.util.List<Map<String, Object>> transformRows(RunContext runContext, java.util.List<Map<String, Object>> rows) {
        return rows.stream().map(policy -> {
            var sanitized = new LinkedHashMap<>(policy);
            if (sanitized.get("alerts") instanceof Map<?, ?> alertsMap) {
                var copyAlerts = new LinkedHashMap<>((Map<String, Object>) alertsMap);
                if (copyAlerts.get("slack") instanceof java.util.List<?> slackList) {
                    var sanitizedSlack = new ArrayList<Map<String, Object>>();
                    for (Object item : slackList) {
                        if (item instanceof Map<?, ?> itemMap) {
                            var mapCopy = new LinkedHashMap<String, Object>((Map<String, Object>) itemMap);
                            mapCopy.remove("url");
                            sanitizedSlack.add(mapCopy);
                        }
                    }
                    copyAlerts.put("slack", sanitizedSlack);
                }
                sanitized.put("alerts", copyAlerts);
            }
            return (Map<String, Object>) sanitized;
        }).toList();
    }
}
