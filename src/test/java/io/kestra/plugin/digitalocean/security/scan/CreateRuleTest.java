package io.kestra.plugin.digitalocean.security.scan;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.models.property.Property;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateRuleTest extends AbstractDigitalOceanTest {

    private static final String RULES_PATH = "/v2/security/scans/rules";
    private static final String DROPLET_URN = "do:droplet:fe3a2fd7-903d-46e6-ada3-3e4f285fb89d";

    private CreateRule.CreateRuleBuilder<?, ?> task(WireMockRuntimeInfo wireMockRuntimeInfo) {
        return CreateRule.builder()
            .id(IdUtils.create())
            .type(CreateRule.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .resource(Property.ofValue(DROPLET_URN));
    }

    @Test
    void excludesTheResourceAndSendsBearerToken(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubFor(post(urlPathEqualTo(RULES_PATH)).willReturn(aResponse().withStatus(201)));

        var output = task(wireMockRuntimeInfo).build().run(runContext());

        assertThat(output, nullValue());
        verify(postRequestedFor(urlPathEqualTo(RULES_PATH)).withRequestBody(equalToJson("{\"resource\": \"" + DROPLET_URN + "\"}")));
        verifyBearer(postRequestedFor(urlPathEqualTo(RULES_PATH)), "test-token");
    }

    @Test
    void failsWithTheApiReasonOnBadRequest(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubPostJson(RULES_PATH, 400, "{\"id\":\"bad_request\",\"message\":\"invalid resource urn\"}");

        var task = task(wireMockRuntimeInfo).resource(Property.ofValue("droplet-123")).build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("HTTP 400"));
        assertThat(ex.getMessage(), containsString("invalid resource urn"));
    }

    @Test
    void rendersTheResourceFromAnExpression(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubFor(post(urlPathEqualTo(RULES_PATH)).willReturn(aResponse().withStatus(201)));

        var task = task(wireMockRuntimeInfo).resource(Property.ofExpression("do:droplet:{{ inputs.droplet_id }}")).build();

        task.run(runContextFactory.of(Map.of("inputs", Map.of("droplet_id", "fe3a2fd7-903d-46e6-ada3-3e4f285fb89d"))));

        verify(postRequestedFor(urlPathEqualTo(RULES_PATH)).withRequestBody(equalToJson("{\"resource\": \"" + DROPLET_URN + "\"}")));
    }
}
