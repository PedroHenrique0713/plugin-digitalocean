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
    title = "List DigitalOcean invoices",
    description = "Lists the account's invoices, following DigitalOcean's page-based pagination automatically. Each row has the invoice UUID, period and amount; use ListInvoiceItems to read its line items."
)
@Plugin(
    examples = {
        @Example(
            title = "List invoices and log how many exist",
            full = true,
            code = """
                id: digitalocean_list_invoices
                namespace: company.team

                tasks:
                  - id: list
                    type: io.kestra.plugin.digitalocean.billing.ListInvoices
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                  - id: log_total
                    type: io.kestra.plugin.core.log.Log
                    message: "Found {{ outputs.list.total }} invoice(s)"
                """
        )
    }
)
public class ListInvoices extends AbstractDigitalOceanListTask {

    @Override
    protected String path(RunContext runContext) {
        return "v2/customers/my/invoices";
    }

    @Override
    protected String arrayKey() {
        return "invoices";
    }

    @Override
    protected String resourceLabel() {
        return "invoice(s)";
    }
}
