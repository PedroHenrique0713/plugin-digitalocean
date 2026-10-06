package io.kestra.plugin.digitalocean.billing;

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
    title = "List the DigitalOcean billing history",
    description = "Lists invoices, payments and credits on the account, newest first, following DigitalOcean's page-based pagination automatically."
)
@Plugin(
    examples = {
        @Example(
            title = "List the billing history and log how many entries exist",
            full = true,
            code = """
                id: digitalocean_billing_history
                namespace: company.team

                tasks:
                  - id: list
                    type: io.kestra.plugin.digitalocean.billing.ListBillingHistory
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                  - id: log_total
                    type: io.kestra.plugin.core.log.Log
                    message: "Found {{ outputs.list.total }} billing history entries"
                """
        )
    }
)
public class ListBillingHistory extends AbstractDigitalOceanListTask {

    @Override
    protected String path(RunContext runContext) {
        return "v2/customers/my/billing_history";
    }

    @Override
    protected String arrayKey() {
        return "billing_history";
    }

    @Override
    protected String resourceLabel() {
        return "billing history entries";
    }
}
