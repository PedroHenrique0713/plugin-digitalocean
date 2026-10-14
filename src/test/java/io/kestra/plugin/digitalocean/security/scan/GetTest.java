package io.kestra.plugin.digitalocean.security.scan;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.models.property.Property;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetTest extends AbstractDigitalOceanTest {

    private static final String SCAN_ID = "497dcba3-ecbf-4587-a2dd-5eb0665e6880";
    private static final String SCAN_PATH = "/v2/security/scans/" + SCAN_ID;

    private static final String SCAN_JSON = """
        {"scan": {"id": "497dcba3-ecbf-4587-a2dd-5eb0665e6880", "status": "COMPLETED", "created_at": "2026-10-09T07:00:00Z",
          "findings": [
            {"rule_uuid": "50e14f43-dd4e-412f-864d-78943ea28d91", "name": "All Firewall Ports Exposed to a Source",
             "severity": "CRITICAL", "details": "The firewall allows every port from one source.",
             "mitigation_steps": [{"step": 1, "title": "Restrict the rule", "description": "Allow only the ports in use."}],
             "affected_resources_count": 2}
          ]}}
        """;

    private Get.GetBuilder<?, ?> task(WireMockRuntimeInfo wireMockRuntimeInfo) {
        return Get.builder()
            .id(IdUtils.create())
            .type(Get.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .scanId(Property.ofValue(SCAN_ID));
    }

    @Test
    void readsScanWithFindingsAndSendsBearerToken(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson(SCAN_PATH, SCAN_JSON);

        var output = task(wireMockRuntimeInfo).build().run(runContext());

        assertThat(output.getId(), is(SCAN_ID));
        assertThat(output.getStatus(), is("COMPLETED"));
        assertThat(output.getCreatedAt(), is(Instant.parse("2026-10-09T07:00:00Z")));
        assertThat(output.getFindings(), hasSize(1));
        assertThat(output.getFindings().getFirst().get("severity"), is("CRITICAL"));
        assertThat(output.getFindings().getFirst().get("affected_resources_count"), is(2));
        verify(getRequestedFor(urlPathEqualTo(SCAN_PATH))
            .withQueryParam("per_page", equalTo("200"))
            .withQueryParam("page", equalTo("1")));
        verifyBearer(getRequestedFor(urlPathEqualTo(SCAN_PATH)), "test-token");
    }

    @Test
    void sendsTheFindingFiltersAndThePage(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson(SCAN_PATH, SCAN_JSON);

        var task = task(wireMockRuntimeInfo)
            .severity(Property.ofValue(Severity.HIGH))
            .findingType(Property.ofValue("Droplet firewall"))
            .perPage(Property.ofValue(50))
            .page(Property.ofValue(2))
            .build();

        task.run(runContext());

        verify(getRequestedFor(urlPathEqualTo(SCAN_PATH))
            .withQueryParam("severity", equalTo("HIGH"))
            .withQueryParam("type", equalTo("Droplet firewall"))
            .withQueryParam("per_page", equalTo("50"))
            .withQueryParam("page", equalTo("2")));
    }

    @Test
    void rejectsAPageSizeAboveTheApiMaximumBeforeAnyRequest(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var task = task(wireMockRuntimeInfo).perPage(Property.ofValue(201)).build();

        var runContext = runContext();
        var ex = assertThrows(IllegalArgumentException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("perPage must be between 1 and 200"));
        verify(0, getRequestedFor(urlPathEqualTo(SCAN_PATH)));
    }

    @Test
    void failsWithClearMessageWhenNotFound(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubStatus(SCAN_PATH, 404, "{\"id\":\"not_found\",\"message\":\"scan not found\"}");

        var task = task(wireMockRuntimeInfo).build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("HTTP 404"));
        assertThat(ex.getMessage(), containsString("scan not found"));
    }

    @Test
    void rendersTheScanIdAndSeverityFromExpressions(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson(SCAN_PATH, SCAN_JSON);

        var task = task(wireMockRuntimeInfo)
            .scanId(Property.ofExpression("{{ outputs.start_scan.id }}"))
            .severity(Property.ofExpression("{{ inputs.severity }}"))
            .build();

        var output = task.run(runContextFactory.of(Map.of(
            "outputs", Map.of("start_scan", Map.of("id", SCAN_ID)),
            "inputs", Map.of("severity", "CRITICAL")
        )));

        assertThat(output.getId(), is(SCAN_ID));
        verify(getRequestedFor(urlPathEqualTo(SCAN_PATH)).withQueryParam("severity", equalTo("CRITICAL")));
    }
}
