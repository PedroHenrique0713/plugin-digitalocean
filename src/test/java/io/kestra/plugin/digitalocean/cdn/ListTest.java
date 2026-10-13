package io.kestra.plugin.digitalocean.cdn;

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
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListTest extends AbstractDigitalOceanTest {

    private static final String ENDPOINTS_JSON = """
        {
          "endpoints": [
            {"id": "19f06b6a-3ace-4315-b086-499a0e521b76", "origin": "static-images.nyc3.digitaloceanspaces.com",
             "endpoint": "static-images.nyc3.cdn.digitaloceanspaces.com", "ttl": 3600, "created_at": "2026-10-13T12:00:00Z"}
          ],
          "links": {},
          "meta": {"total": 1}
        }
        """;

    @Test
    void listsEndpointsAndSendsBearerToken(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson("/v2/cdn/endpoints", ENDPOINTS_JSON);

        var task = List.builder()
            .id(IdUtils.create())
            .type(List.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .build();

        var output = task.run(runContext());

        assertThat(output.getTotal(), is(1L));
        assertThat(output.getRows().getFirst().get("endpoint"), is("static-images.nyc3.cdn.digitaloceanspaces.com"));
        verify(getRequestedFor(urlPathEqualTo("/v2/cdn/endpoints")).withQueryParam("per_page", equalTo("200")));
        verifyBearer(getRequestedFor(urlPathEqualTo("/v2/cdn/endpoints")), "test-token");
    }

    @Test
    void failsWithClearMessageOnRateLimit(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubStatus("/v2/cdn/endpoints", 429, "{\"message\":\"too many requests\"}");

        var task = List.builder()
            .id(IdUtils.create())
            .type(List.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("rate limit"));
    }

    @Test
    void rendersThePageSizeFromAnExpression(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson("/v2/cdn/endpoints", ENDPOINTS_JSON);

        var task = List.builder()
            .id(IdUtils.create())
            .type(List.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .perPage(Property.ofExpression("{{ inputs.perPage }}"))
            .build();

        task.run(runContextFactory.of(Map.of("inputs", Map.of("perPage", 50))));

        verify(getRequestedFor(urlPathEqualTo("/v2/cdn/endpoints")).withQueryParam("per_page", equalTo("50")));
    }
}
