package io.kestra.plugin.digitalocean.cdn;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.models.property.Property;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurgeCacheTest extends AbstractDigitalOceanTest {

    private static final String PATH = "/v2/cdn/endpoints/19f06b6a-3ace-4315-b086-499a0e521b76/cache";

    private PurgeCache task(WireMockRuntimeInfo wireMockRuntimeInfo, Property<String> endpointId, Property<List<String>> files) {
        return PurgeCache.builder()
            .id(IdUtils.create())
            .type(PurgeCache.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .endpointId(endpointId)
            .files(files)
            .build();
    }

    @Test
    void sendsThePathsInTheRequestBody(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubFor(delete(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(204)));

        task(wireMockRuntimeInfo, Property.ofValue("19f06b6a-3ace-4315-b086-499a0e521b76"), Property.ofValue(List.of("css/*", "index.html")))
            .run(runContext());

        verifyBearer(deleteRequestedFor(urlPathEqualTo(PATH)), "test-token");
        verify(deleteRequestedFor(urlPathEqualTo(PATH)).withRequestBody(equalToJson("""
            {"files": ["css/*", "index.html"]}
            """)));
    }

    @Test
    void rejectsAnEmptyListBeforeCallingTheApi(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var task = task(wireMockRuntimeInfo, Property.ofValue("19f06b6a-3ace-4315-b086-499a0e521b76"), Property.ofValue(List.of()));

        var runContext = runContext();
        var ex = assertThrows(IllegalArgumentException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("files must list at least one path"));
        verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void failsWithClearMessageOnRateLimit(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubFor(delete(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(429).withBody("{\"message\":\"too many requests\"}")));

        var task = task(wireMockRuntimeInfo, Property.ofValue("19f06b6a-3ace-4315-b086-499a0e521b76"), Property.ofValue(List.of("*")));

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("rate limit"));
    }

    @Test
    void rendersTheEndpointIdAndEachPathFromExpressions(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubFor(delete(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(204)));

        task(
            wireMockRuntimeInfo,
            Property.ofExpression("{{ inputs.endpointId }}"),
            Property.ofExpression("[\"{{ inputs.folder }}/*\", \"index.html\"]")
        ).run(runContextFactory.of(Map.of("inputs", Map.of(
            "endpointId", "19f06b6a-3ace-4315-b086-499a0e521b76",
            "folder", "assets"
        ))));

        verify(deleteRequestedFor(urlPathEqualTo(PATH)).withRequestBody(equalToJson("""
            {"files": ["assets/*", "index.html"]}
            """)));
    }
}
