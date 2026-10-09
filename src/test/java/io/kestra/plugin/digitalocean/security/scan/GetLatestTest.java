package io.kestra.plugin.digitalocean.security.scan;

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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetLatestTest extends AbstractDigitalOceanTest {

    private static final String LATEST_PATH = "/v2/security/scans/latest";

    private static final String SCAN_JSON = """
        {"scan": {"id": "497dcba3-ecbf-4587-a2dd-5eb0665e6880", "status": "COMPLETED", "created_at": "2026-10-09T07:00:00Z",
          "findings": [
            {"rule_uuid": "50e14f43-dd4e-412f-864d-78943ea28d91", "name": "Droplet Without Backups", "severity": "HIGH", "affected_resources_count": 3},
            {"rule_uuid": "6f1a1d0c-2b1e-4a3c-9d53-0c1b3f2a7e11", "name": "Database Open to the Internet", "severity": "HIGH", "affected_resources_count": 1}
          ]}}
        """;

    private GetLatest.GetLatestBuilder<?, ?> task(WireMockRuntimeInfo wireMockRuntimeInfo) {
        return GetLatest.builder()
            .id(IdUtils.create())
            .type(GetLatest.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()));
    }

    @Test
    void readsTheLatestScanAndSendsBearerToken(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson(LATEST_PATH, SCAN_JSON);

        var output = task(wireMockRuntimeInfo).build().run(runContext());

        assertThat(output.getId(), is("497dcba3-ecbf-4587-a2dd-5eb0665e6880"));
        assertThat(output.getFindings(), hasSize(2));
        assertThat(output.getFindings().get(1).get("name"), is("Database Open to the Internet"));
        verifyBearer(getRequestedFor(urlPathEqualTo(LATEST_PATH)), "test-token");
    }

    @Test
    void returnsNoFindingsWhenCspmIsNotEnabled(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson(LATEST_PATH, "{\"scan\": {\"status\": \"CSPM_NOT_ENABLED\"}}");

        var output = task(wireMockRuntimeInfo).build().run(runContext());

        assertThat(output.getStatus(), is("CSPM_NOT_ENABLED"));
        assertThat(output.getId(), is(nullValue()));
        assertThat(output.getCreatedAt(), is(nullValue()));
        assertThat(output.getFindings(), is(empty()));
    }

    @Test
    void failsWithClearMessageOnRateLimit(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubStatus(LATEST_PATH, 429, "{\"message\":\"too many requests\"}");

        var task = task(wireMockRuntimeInfo).build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("rate limit"));
    }

    @Test
    void rendersTheFiltersFromExpressions(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson(LATEST_PATH, SCAN_JSON);

        var task = task(wireMockRuntimeInfo)
            .severity(Property.ofExpression("{{ inputs.severity }}"))
            .findingType(Property.ofExpression("{{ inputs.type }}"))
            .build();

        task.run(runContextFactory.of(Map.of("inputs", Map.of("severity", "HIGH", "type", "droplet"))));

        verify(getRequestedFor(urlPathEqualTo(LATEST_PATH))
            .withQueryParam("severity", equalTo("HIGH"))
            .withQueryParam("type", equalTo("droplet")));
    }
}
