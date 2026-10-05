package io.kestra.plugin.digitalocean.snapshot;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.models.property.Property;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeleteTest extends AbstractDigitalOceanTest {

    @Test
    void deletesSnapshotAndSendsBearerToken(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubFor(delete(urlPathEqualTo("/v2/snapshots/6372321")).willReturn(aResponse().withStatus(204)));

        var task = Delete.builder()
            .id(IdUtils.create())
            .type(Delete.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .snapshotId(Property.ofValue("6372321"))
            .build();

        task.run(runContext());

        verifyBearer(deleteRequestedFor(urlPathEqualTo("/v2/snapshots/6372321")), "test-token");
    }

    @Test
    void failsWithClearMessageWhenNotFound(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubFor(delete(urlPathEqualTo("/v2/snapshots/missing")).willReturn(aResponse().withStatus(404).withBody("{\"message\":\"not found\"}")));

        var task = Delete.builder()
            .id(IdUtils.create())
            .type(Delete.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .snapshotId(Property.ofValue("missing"))
            .build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("resource not found"));
    }

    @Test
    void rendersSnapshotIdFromAnExpression(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubFor(delete(urlPathEqualTo("/v2/snapshots/6372321")).willReturn(aResponse().withStatus(204)));

        var task = Delete.builder()
            .id(IdUtils.create())
            .type(Delete.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .snapshotId(Property.ofExpression("{{ inputs.snapshotId }}"))
            .build();

        task.run(runContextFactory.of(Map.of("inputs", Map.of("snapshotId", "6372321"))));

        verify(deleteRequestedFor(urlPathEqualTo("/v2/snapshots/6372321")));
    }
}
