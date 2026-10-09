package io.kestra.plugin.digitalocean.billing;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.models.property.Property;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListInvoiceItemsTest extends AbstractDigitalOceanTest {

    private static final String ITEMS_PATH = "/v2/customers/my/invoices/22737513-0ea7-4206-8ceb-98a575af7681";

    private static final String ITEMS_JSON = """
        {
          "invoice_items": [
            {"product": "Kubernetes Clusters", "resource_uuid": "711157cb-37c8-4817-b371-44fa3504a39c", "resource_id": "2353624", "group_description": "", "description": "my-doks-cluster", "amount": "12.34", "duration": "744", "duration_unit": "Hours", "start_time": "2026-09-01T00:00:00Z", "end_time": "2026-10-01T00:00:00Z", "project_name": "web"}
          ],
          "links": {"pages": {}},
          "meta": {"total": 1}
        }
        """;

    @Test
    void listsItemsOfTheGivenInvoice(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson(ITEMS_PATH, ITEMS_JSON);

        var task = ListInvoiceItems.builder()
            .id(IdUtils.create())
            .type(ListInvoiceItems.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .invoiceUuid(Property.ofValue("22737513-0ea7-4206-8ceb-98a575af7681"))
            .build();

        var output = task.run(runContext());

        assertThat(output.getTotal(), is(1L));
        assertThat(output.getRows().getFirst().get("product"), is("Kubernetes Clusters"));
        verifyBearer(getRequestedFor(urlPathEqualTo(ITEMS_PATH)), "test-token");
    }

    @Test
    void failsWithClearMessageWhenInvoiceNotFound(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubStatus("/v2/customers/my/invoices/missing", 404, "{\"message\":\"not found\"}");

        var task = ListInvoiceItems.builder()
            .id(IdUtils.create())
            .type(ListInvoiceItems.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .invoiceUuid(Property.ofValue("missing"))
            .build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("resource not found"));
    }

    @Test
    void rendersTheInvoiceUuidFromAnExpression(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson(ITEMS_PATH, ITEMS_JSON);

        var task = ListInvoiceItems.builder()
            .id(IdUtils.create())
            .type(ListInvoiceItems.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .invoiceUuid(Property.ofExpression("{{ inputs.invoice.uuid }}"))
            .build();

        task.run(runContextFactory.of(Map.of("inputs", Map.of("invoice", Map.of("uuid", "22737513-0ea7-4206-8ceb-98a575af7681")))));

        verifyBearer(getRequestedFor(urlPathEqualTo(ITEMS_PATH)), "test-token");
    }
}
