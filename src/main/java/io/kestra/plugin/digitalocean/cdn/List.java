package io.kestra.plugin.digitalocean.cdn;

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

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List DigitalOcean CDN endpoints",
    description = "Lists the CDN endpoints on the account, following DigitalOcean's page-based pagination automatically."
)
@Plugin(
    examples = {
        @Example(
            title = "List the CDN endpoints and log how many there are",
            full = true,
            code = """
                id: digitalocean_list_cdn_endpoints
                namespace: company.team

                tasks:
                  - id: list_endpoints
                    type: io.kestra.plugin.digitalocean.cdn.List
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                  - id: log_total
                    type: io.kestra.plugin.core.log.Log
                    message: "{{ outputs.list_endpoints.total }} CDN endpoint(s) on the account"
                """
        )
    }
)
public class List extends AbstractDigitalOceanListTask {

    @Override
    protected String path(RunContext runContext) {
        return "v2/cdn/endpoints";
    }

    @Override
    protected String arrayKey() {
        return "endpoints";
    }

    @Override
    protected String resourceLabel() {
        return "CDN endpoints";
    }
}
