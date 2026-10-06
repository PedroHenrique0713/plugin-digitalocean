package io.kestra.plugin.digitalocean.billing;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.models.property.Property;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetBalanceTest extends AbstractDigitalOceanTest {

    private static final String BALANCE_JSON = """
        {"month_to_date_balance": "23.44", "account_balance": "-12.10", "month_to_date_usage": "35.54", "generated_at": "2026-10-06T03:01:12Z"}
        """;

    @Test
    void fetchesBalanceAsExactDecimalsAndSendsBearerToken(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson("/v2/customers/my/balance", BALANCE_JSON);

        var task = GetBalance.builder()
            .id(IdUtils.create())
            .type(GetBalance.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .build();

        var output = task.run(runContext());

        assertThat(output.getMonthToDateBalance(), is(new BigDecimal("23.44")));
        assertThat(output.getAccountBalance(), is(new BigDecimal("-12.10")));
        assertThat(output.getMonthToDateUsage(), is(new BigDecimal("35.54")));
        assertThat(output.getGeneratedAt(), is(Instant.parse("2026-10-06T03:01:12Z")));
        verifyBearer(getRequestedFor(urlPathEqualTo("/v2/customers/my/balance")), "test-token");
    }

    @Test
    void failsWithClearMessageOnInvalidToken(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubStatus("/v2/customers/my/balance", 401, "{\"message\":\"Unable to authenticate you\"}");

        var task = GetBalance.builder()
            .id(IdUtils.create())
            .type(GetBalance.class.getName())
            .apiToken(Property.ofValue("bad-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("invalid or missing API token"));
    }

    @Test
    void rendersTheApiTokenFromAnExpression(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson("/v2/customers/my/balance", BALANCE_JSON);

        var task = GetBalance.builder()
            .id(IdUtils.create())
            .type(GetBalance.class.getName())
            .apiToken(Property.ofExpression("{{ inputs.token }}"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .build();

        task.run(runContextFactory.of(Map.of("inputs", Map.of("token", "rendered-token"))));

        verifyBearer(getRequestedFor(urlPathEqualTo("/v2/customers/my/balance")), "rendered-token");
    }
}
