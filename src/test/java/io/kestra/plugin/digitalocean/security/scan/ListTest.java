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
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListTest extends AbstractDigitalOceanTest {

    private static final String SCANS_PATH = "/v2/security/scans";

    private static final String SCANS_JSON = """
        {
          "scans": [
            {"id": "497dcba3-ecbf-4587-a2dd-5eb0665e6880", "status": "COMPLETED", "created_at": "2026-10-09T07:00:00Z",
             "findings": [{"rule_uuid": "50e14f43-dd4e-412f-864d-78943ea28d91", "name": "Droplet Without Backups", "severity": "HIGH"}]},
            {"id": "8a6f2f6e-0c0e-4d0f-9a43-2d7b4d2b8c55", "status": "FAILED", "created_at": "2026-10-02T07:00:00Z"}
          ],
          "links": {"pages": {}},
          "meta": {"total": 2}
        }
        """;

    private List.ListBuilder<?, ?> task(WireMockRuntimeInfo wireMockRuntimeInfo) {
        return List.builder()
            .id(IdUtils.create())
            .type(List.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()));
    }

    @Test
    void listsScansWithoutTheirFindingsAndSendsBearerToken(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson(SCANS_PATH, SCANS_JSON);

        var output = task(wireMockRuntimeInfo).build().run(runContext());

        assertThat(output.getTotal(), is(2L));
        assertThat(output.getRows().getFirst().get("id"), is("497dcba3-ecbf-4587-a2dd-5eb0665e6880"));
        assertThat(output.getRows().getFirst().get("status"), is("COMPLETED"));
        assertThat(output.getRows().getFirst(), not(hasKey("findings")));
        assertThat(output.getRows().get(1).get("status"), is("FAILED"));
        verifyBearer(getRequestedFor(urlPathEqualTo(SCANS_PATH)), "test-token");
    }

    @Test
    void failsWithClearMessageOnRateLimit(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubStatus(SCANS_PATH, 429, "{\"message\":\"too many requests\"}");

        var task = task(wireMockRuntimeInfo).build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("rate limit"));
    }

    @Test
    void rendersThePageSizeFromAnExpression(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson(SCANS_PATH, SCANS_JSON);

        var task = task(wireMockRuntimeInfo).perPage(Property.ofExpression("{{ inputs.perPage }}")).build();

        task.run(runContextFactory.of(Map.of("inputs", Map.of("perPage", 50))));

        verify(getRequestedFor(urlPathEqualTo(SCANS_PATH)).withQueryParam("per_page", equalTo("50")));
    }
}
