package io.kestra.plugin.digitalocean.billing;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.models.property.Property;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListInvoicesTest extends AbstractDigitalOceanTest {

    private static final String INVOICES_JSON = """
        {
          "invoices": [
            {"invoice_uuid": "22737513-0ea7-4206-8ceb-98a575af7681", "invoice_id": "123456789", "amount": "12.34", "invoice_period": "2026-09", "updated_at": "2026-10-01T00:00:00Z"}
          ],
          "invoice_preview": {"invoice_uuid": "fdabb512-6faf-443c-ba2e-665452332a9e", "amount": "23.45", "invoice_period": "2026-10", "updated_at": "2026-10-06T00:00:00Z"},
          "links": {"pages": {}},
          "meta": {"total": 1}
        }
        """;

    @Test
    void listsInvoicesAndSendsBearerToken(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson("/v2/customers/my/invoices", INVOICES_JSON);

        var task = ListInvoices.builder()
            .id(IdUtils.create())
            .type(ListInvoices.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .build();

        var output = task.run(runContext());

        assertThat(output.getTotal(), is(1L));
        assertThat(output.getRows().getFirst().get("invoice_period"), is("2026-09"));
        verifyBearer(getRequestedFor(urlPathEqualTo("/v2/customers/my/invoices")), "test-token");
    }

    @Test
    void failsWithClearMessageOnRateLimit(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubStatusWithHeader("/v2/customers/my/invoices", 429, "{\"message\":\"too many requests\"}", "retry-after", "20");

        var task = ListInvoices.builder()
            .id(IdUtils.create())
            .type(ListInvoices.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("rate limit"));
    }

    @Test
    void rendersPageSizeAndFetchTypeFromExpressions(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson("/v2/customers/my/invoices", INVOICES_JSON);

        var task = ListInvoices.builder()
            .id(IdUtils.create())
            .type(ListInvoices.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .perPage(Property.ofExpression("{{ inputs.perPage }}"))
            .fetchType(Property.ofExpression("{{ inputs.fetchType }}"))
            .build();

        var output = task.run(runContextFactory.of(Map.of("inputs", Map.of("perPage", "50", "fetchType", "FETCH_ONE"))));

        assertThat(output.getRow().get("invoice_period"), is("2026-09"));
        verify(getRequestedFor(urlPathEqualTo("/v2/customers/my/invoices")).withQueryParam("per_page", equalTo("50")));
    }
}
