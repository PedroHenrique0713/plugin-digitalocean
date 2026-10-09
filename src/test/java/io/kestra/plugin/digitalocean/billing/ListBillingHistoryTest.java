package io.kestra.plugin.digitalocean.billing;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.models.property.Property;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListBillingHistoryTest extends AbstractDigitalOceanTest {

    private static final String PATH = "/v2/customers/my/billing_history";

    @Test
    void followsPaginationWhenMetaTotalIsMissing(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        var baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
        stubFor(get(urlPathEqualTo(PATH)).withQueryParam("page", absent()).willReturn(okJson("""
            {
              "billing_history": [
                {"description": "Invoice for September 2026", "amount": "12.34", "invoice_id": "123", "invoice_uuid": "inv-1", "date": "2026-10-01T08:44:38Z", "type": "Invoice"},
                {"description": "Payment (MC 2026)", "amount": "-12.34", "date": "2026-10-02T08:44:38Z", "type": "Payment"}
              ],
              "links": {"pages": {"next": "%s%s?page=2&per_page=200"}}
            }
            """.formatted(baseUrl, PATH))));
        stubFor(get(urlPathEqualTo(PATH)).withQueryParam("page", equalTo("2")).willReturn(okJson("""
            {"billing_history": [{"description": "Credit", "amount": "-5.00", "date": "2026-09-15T00:00:00Z", "type": "Credit"}], "links": {"pages": {}}}
            """)));

        var task = ListBillingHistory.builder()
            .id(IdUtils.create())
            .type(ListBillingHistory.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(baseUrl))
            .build();

        var output = task.run(runContext());

        assertThat(output.getTotal(), is(3L));
        assertThat(output.getRows().size(), is(3));
        assertThat(output.getRows().get(2).get("type"), is("Credit"));
        verifyBearer(getRequestedFor(urlPathEqualTo(PATH)).withQueryParam("page", equalTo("2")), "test-token");
    }

    @Test
    void failsWithClearMessageOnInvalidToken(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubStatus(PATH, 401, "{\"message\":\"Unable to authenticate you\"}");

        var task = ListBillingHistory.builder()
            .id(IdUtils.create())
            .type(ListBillingHistory.class.getName())
            .apiToken(Property.ofValue("bad-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("invalid or missing API token"));
    }

    @Test
    void rendersTheApiTokenFromAnExpression(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson("{\"billing_history\": [], \"links\": {}}")));

        var task = ListBillingHistory.builder()
            .id(IdUtils.create())
            .type(ListBillingHistory.class.getName())
            .apiToken(Property.ofExpression("{{ inputs.token }}"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .build();

        var output = task.run(runContextFactory.of(Map.of("inputs", Map.of("token", "rendered-token"))));

        assertThat(output.getTotal(), is(0L));
        verifyBearer(getRequestedFor(urlPathEqualTo(PATH)), "rendered-token");
    }
}
