package io.kestra.plugin.digitalocean.cdn;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.models.property.Property;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetTest extends AbstractDigitalOceanTest {

    static final String ENDPOINT_JSON = """
        {"endpoint": {"id": "19f06b6a-3ace-4315-b086-499a0e521b76", "origin": "static-images.nyc3.digitaloceanspaces.com",
          "endpoint": "static-images.nyc3.cdn.digitaloceanspaces.com", "ttl": 3600,
          "certificate_id": "892071a0-bb95-49bc-8021-3afd67a210bf", "custom_domain": "static.example.com",
          "created_at": "2026-10-13T12:00:00Z"}}
        """;

    private Get task(WireMockRuntimeInfo wireMockRuntimeInfo, Property<String> endpointId) {
        return Get.builder()
            .id(IdUtils.create())
            .type(Get.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .endpointId(endpointId)
            .build();
    }

    @Test
    void mapsTheEndpointToTheOutput(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson("/v2/cdn/endpoints/19f06b6a-3ace-4315-b086-499a0e521b76", ENDPOINT_JSON);

        var output = task(wireMockRuntimeInfo, Property.ofValue("19f06b6a-3ace-4315-b086-499a0e521b76")).run(runContext());

        assertThat(output.getId(), is("19f06b6a-3ace-4315-b086-499a0e521b76"));
        assertThat(output.getOrigin(), is("static-images.nyc3.digitaloceanspaces.com"));
        assertThat(output.getEndpoint(), is("static-images.nyc3.cdn.digitaloceanspaces.com"));
        assertThat(output.getTtl(), is(3600));
        assertThat(output.getCertificateId(), is("892071a0-bb95-49bc-8021-3afd67a210bf"));
        assertThat(output.getCustomDomain(), is("static.example.com"));
        assertThat(output.getCreatedAt(), is(Instant.parse("2026-10-13T12:00:00Z")));
        verifyBearer(getRequestedFor(urlPathEqualTo("/v2/cdn/endpoints/19f06b6a-3ace-4315-b086-499a0e521b76")), "test-token");
    }

    @Test
    void failsWithClearMessageWhenNotFound(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubStatus("/v2/cdn/endpoints/missing", 404, "{\"id\":\"not_found\",\"message\":\"The resource you were accessing could not be found.\"}");

        var task = task(wireMockRuntimeInfo, Property.ofValue("missing"));

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("HTTP 404"));
    }

    @Test
    void rendersTheEndpointIdFromAnExpression(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson("/v2/cdn/endpoints/19f06b6a-3ace-4315-b086-499a0e521b76", ENDPOINT_JSON);

        task(wireMockRuntimeInfo, Property.ofExpression("{{ inputs.endpointId }}"))
            .run(runContextFactory.of(Map.of("inputs", Map.of("endpointId", "19f06b6a-3ace-4315-b086-499a0e521b76"))));

        verify(getRequestedFor(urlPathEqualTo("/v2/cdn/endpoints/19f06b6a-3ace-4315-b086-499a0e521b76")));
    }
}
