package io.kestra.plugin.digitalocean.security.settings;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.models.property.Property;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdatePlanTest extends AbstractDigitalOceanTest {

    private static final String PLAN_PATH = "/v2/security/settings/plan";
    private static final String DROPLET_URN = "do:droplet:fe3a2fd7-903d-46e6-ada3-3e4f285fb89d";

    private static final String PLAN_JSON = """
        {"tier_coverage": {"basic": {"resources": ["do:droplet:fe3a2fd7-903d-46e6-ada3-3e4f285fb89d"], "tags": ["production"]}}}
        """;

    private UpdatePlan.UpdatePlanBuilder<?, ?> task(WireMockRuntimeInfo wireMockRuntimeInfo, Map<String, TierCoverage> tierCoverage) {
        return UpdatePlan.builder()
            .id(IdUtils.create())
            .type(UpdatePlan.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .tierCoverage(Property.ofValue(tierCoverage));
    }

    @Test
    void updatesTheCoverageAndSendsBearerToken(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubFor(put(urlPathEqualTo(PLAN_PATH)).willReturn(okJson(PLAN_JSON)));

        var output = task(wireMockRuntimeInfo, Map.of(
            "basic", TierCoverage.builder().resources(List.of(DROPLET_URN)).tags(List.of("production")).build()
        )).build().run(runContext());

        assertThat(((Map<?, ?>) output.getTierCoverage().get("basic")).get("tags"), is(List.of("production")));
        verify(putRequestedFor(urlPathEqualTo(PLAN_PATH)).withRequestBody(equalToJson(
            "{\"tier_coverage\": {\"basic\": {\"resources\": [\"" + DROPLET_URN + "\"], \"tags\": [\"production\"]}}}"
        )));
        verifyBearer(putRequestedFor(urlPathEqualTo(PLAN_PATH)), "test-token");
    }

    @Test
    void sendsAListLeftOutAsEmpty(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubFor(put(urlPathEqualTo(PLAN_PATH)).willReturn(okJson(PLAN_JSON)));

        task(wireMockRuntimeInfo, Map.of("basic", TierCoverage.builder().tags(List.of("production")).build()))
            .build()
            .run(runContext());

        verify(putRequestedFor(urlPathEqualTo(PLAN_PATH)).withRequestBody(equalToJson(
            "{\"tier_coverage\": {\"basic\": {\"resources\": [], \"tags\": [\"production\"]}}}"
        )));
    }

    @Test
    void rejectsAnEmptyCoverageBeforeAnyRequest(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var task = task(wireMockRuntimeInfo, Map.of()).build();

        var runContext = runContext();
        var ex = assertThrows(IllegalArgumentException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("tierCoverage needs at least one tier"));
        verify(0, putRequestedFor(urlPathEqualTo(PLAN_PATH)));
    }

    @Test
    void failsWithTheApiReasonOnBadRequest(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubFor(put(urlPathEqualTo(PLAN_PATH)).willReturn(aResponse()
            .withStatus(400)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"id\":\"bad_request\",\"message\":\"unknown tier: gold\"}")));

        var task = task(wireMockRuntimeInfo, Map.of("gold", TierCoverage.builder().tags(List.of("production")).build())).build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("HTTP 400"));
        assertThat(ex.getMessage(), containsString("unknown tier: gold"));
    }

    @Test
    void rendersTheCoverageFromAnExpression(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubFor(put(urlPathEqualTo(PLAN_PATH)).willReturn(okJson(PLAN_JSON)));

        var task = UpdatePlan.builder()
            .id(IdUtils.create())
            .type(UpdatePlan.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .tierCoverage(Property.ofExpression("{\"basic\": {\"tags\": [\"{{ inputs.tag }}\"]}}"))
            .build();

        task.run(runContextFactory.of(Map.of("inputs", Map.of("tag", "staging"))));

        verify(putRequestedFor(urlPathEqualTo(PLAN_PATH)).withRequestBody(equalToJson(
            "{\"tier_coverage\": {\"basic\": {\"resources\": [], \"tags\": [\"staging\"]}}}"
        )));
    }
}
