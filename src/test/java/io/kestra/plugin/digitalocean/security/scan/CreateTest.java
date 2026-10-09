package io.kestra.plugin.digitalocean.security.scan;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.models.property.Property;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateTest extends AbstractDigitalOceanTest {

    private static final String SCANS_PATH = "/v2/security/scans";

    private static final String SCAN_JSON = """
        {"scan": {"id": "497dcba3-ecbf-4587-a2dd-5eb0665e6880", "status": "IN_PROGRESS", "created_at": "2026-10-09T07:00:00Z"}}
        """;

    private Create.CreateBuilder<?, ?> task(WireMockRuntimeInfo wireMockRuntimeInfo) {
        return Create.builder()
            .id(IdUtils.create())
            .type(Create.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()));
    }

    @Test
    void startsAScanAndSendsBearerToken(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubPostJson(SCANS_PATH, 201, SCAN_JSON);

        var output = task(wireMockRuntimeInfo).build().run(runContext());

        assertThat(output.getId(), is("497dcba3-ecbf-4587-a2dd-5eb0665e6880"));
        assertThat(output.getStatus(), is("IN_PROGRESS"));
        assertThat(output.getCreatedAt(), is(Instant.parse("2026-10-09T07:00:00Z")));
        assertThat(output.getFindings(), is(empty()));
        verify(postRequestedFor(urlPathEqualTo(SCANS_PATH)).withRequestBody(equalToJson("{}")));
        verifyBearer(postRequestedFor(urlPathEqualTo(SCANS_PATH)), "test-token");
    }

    @Test
    void failsWithTheApiReasonOnBadRequest(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubPostJson(SCANS_PATH, 400, "{\"id\":\"bad_request\",\"message\":\"a scan is already in progress\"}");

        var task = task(wireMockRuntimeInfo).build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("HTTP 400"));
        assertThat(ex.getMessage(), containsString("a scan is already in progress"));
    }

    @Test
    void rendersTheApiTokenFromAnExpression(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubPostJson(SCANS_PATH, 201, SCAN_JSON);

        var task = task(wireMockRuntimeInfo).apiToken(Property.ofExpression("{{ inputs.token }}")).build();

        task.run(runContextFactory.of(Map.of("inputs", Map.of("token", "rendered-token"))));

        verifyBearer(postRequestedFor(urlPathEqualTo(SCANS_PATH)), "rendered-token");
    }
}
