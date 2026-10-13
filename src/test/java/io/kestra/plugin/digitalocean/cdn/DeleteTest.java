package io.kestra.plugin.digitalocean.cdn;

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

    private static final String PATH = "/v2/cdn/endpoints/19f06b6a-3ace-4315-b086-499a0e521b76";

    private Delete task(WireMockRuntimeInfo wireMockRuntimeInfo, Property<String> endpointId) {
        return Delete.builder()
            .id(IdUtils.create())
            .type(Delete.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .endpointId(endpointId)
            .build();
    }

    @Test
    void deletesTheEndpointAndSendsBearerToken(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubFor(delete(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(204)));

        task(wireMockRuntimeInfo, Property.ofValue("19f06b6a-3ace-4315-b086-499a0e521b76")).run(runContext());

        verifyBearer(deleteRequestedFor(urlPathEqualTo(PATH)), "test-token");
    }

    @Test
    void failsWithClearMessageWhenNotFound(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubFor(delete(urlPathEqualTo("/v2/cdn/endpoints/missing")).willReturn(aResponse().withStatus(404).withBody("{\"message\":\"not found\"}")));

        var task = task(wireMockRuntimeInfo, Property.ofValue("missing"));

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("HTTP 404"));
    }

    @Test
    void rendersTheEndpointIdFromAnExpression(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubFor(delete(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(204)));

        task(wireMockRuntimeInfo, Property.ofExpression("{{ inputs.endpointId }}"))
            .run(runContextFactory.of(Map.of("inputs", Map.of("endpointId", "19f06b6a-3ace-4315-b086-499a0e521b76"))));

        verify(deleteRequestedFor(urlPathEqualTo(PATH)));
    }
}
