package io.kestra.plugin.digitalocean.billing;

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
    title = "List the line items of a DigitalOcean invoice",
    description = "Lists the line items (product, resource, amount, duration) of one invoice, following DigitalOcean's page-based pagination automatically."
)
@Plugin(
    examples = {
        @Example(
            title = "List the line items of the first invoice returned by ListInvoices",
            full = true,
            code = """
                id: digitalocean_list_invoice_items
                namespace: company.team

                tasks:
                  - id: first_invoice
                    type: io.kestra.plugin.digitalocean.billing.ListInvoices
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    fetchType: FETCH_ONE
                  - id: items
                    type: io.kestra.plugin.digitalocean.billing.ListInvoiceItems
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    invoiceUuid: "{{ outputs.first_invoice.row.invoice_uuid }}"
                  - id: log_total
                    type: io.kestra.plugin.core.log.Log
                    message: "Invoice {{ outputs.first_invoice.row.invoice_period }} has {{ outputs.items.total }} line item(s)"
                """
        )
    }
)
public class ListInvoiceItems extends AbstractDigitalOceanListTask {

    @Schema(title = "Invoice UUID", description = "UUID of the invoice to read, as returned by ListInvoices or ListBillingHistory.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> invoiceUuid;

    @Override
    protected String path(RunContext runContext) throws Exception {
        return "v2/customers/my/invoices/" + encodePathSegment(requireRendered(runContext, invoiceUuid, String.class, "invoiceUuid"));
    }

    @Override
    protected String arrayKey() {
        return "invoice_items";
    }

    @Override
    protected String resourceLabel() {
        return "invoice item(s)";
    }
}
