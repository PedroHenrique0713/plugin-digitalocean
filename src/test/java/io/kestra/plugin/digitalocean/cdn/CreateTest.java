package io.kestra.plugin.digitalocean.cdn;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.models.property.Property;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateTest extends AbstractDigitalOceanTest {

    private Create.CreateBuilder<?, ?> builder(WireMockRuntimeInfo wireMockRuntimeInfo) {
        return Create.builder()
            .id(IdUtils.create())
            .type(Create.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .origin(Property.ofValue("static-images.nyc3.digitaloceanspaces.com"));
    }

    @Test
    void createsTheEndpointWithEveryPropertySet(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubPostJson("/v2/cdn/endpoints", 201, GetTest.ENDPOINT_JSON);

        var output = builder(wireMockRuntimeInfo)
            .ttl(Property.ofValue(86400))
            .customDomain(Property.ofValue("static.example.com"))
            .certificateId(Property.ofValue("892071a0-bb95-49bc-8021-3afd67a210bf"))
            .build()
            .run(runContext());

        assertThat(output.getEndpoint(), is("static-images.nyc3.cdn.digitaloceanspaces.com"));
        verifyBearer(postRequestedFor(urlPathEqualTo("/v2/cdn/endpoints")), "test-token");
        verify(postRequestedFor(urlPathEqualTo("/v2/cdn/endpoints")).withRequestBody(equalToJson("""
            {"origin": "static-images.nyc3.digitaloceanspaces.com", "ttl": 86400,
             "custom_domain": "static.example.com", "certificate_id": "892071a0-bb95-49bc-8021-3afd67a210bf"}
            """)));
    }

    @Test
    void sendsOnlyTheOriginWhenNothingElseIsSet(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubPostJson("/v2/cdn/endpoints", 201, GetTest.ENDPOINT_JSON);

        builder(wireMockRuntimeInfo).build().run(runContext());

        verify(postRequestedFor(urlPathEqualTo("/v2/cdn/endpoints")).withRequestBody(equalToJson("""
            {"origin": "static-images.nyc3.digitaloceanspaces.com"}
            """)));
    }

    @Test
    void rejectsAnUnsupportedTtlBeforeCallingTheApi(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var task = builder(wireMockRuntimeInfo).ttl(Property.ofValue(120)).build();

        var runContext = runContext();
        var ex = assertThrows(IllegalArgumentException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("ttl must be one of 60, 600, 3600, 86400, or 604800 seconds, got 120"));
        verify(0, postRequestedFor(urlPathEqualTo("/v2/cdn/endpoints")));
    }

    @Test
    void surfacesTheApiReasonOnBadRequest(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubPostJson("/v2/cdn/endpoints", 400, "{\"id\": \"bad_request\", \"message\": \"origin is not a valid Space\"}");

        var task = builder(wireMockRuntimeInfo).build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("HTTP 400"));
        assertThat(ex.getMessage(), containsString("origin is not a valid Space"));
    }

    @Test
    void rendersThePropertiesFromExpressions(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubPostJson("/v2/cdn/endpoints", 201, GetTest.ENDPOINT_JSON);

        builder(wireMockRuntimeInfo)
            .origin(Property.ofExpression("{{ inputs.bucket }}.nyc3.digitaloceanspaces.com"))
            .ttl(Property.ofExpression("{{ inputs.ttl }}"))
            .customDomain(Property.ofExpression("static.{{ inputs.domain }}"))
            .certificateId(Property.ofExpression("{{ inputs.certificate }}"))
            .build()
            .run(runContextFactory.of(Map.of("inputs", Map.of(
                "bucket", "static-images",
                "ttl", 600,
                "domain", "example.com",
                "certificate", "892071a0-bb95-49bc-8021-3afd67a210bf"
            ))));

        verify(postRequestedFor(urlPathEqualTo("/v2/cdn/endpoints")).withRequestBody(equalToJson("""
            {"origin": "static-images.nyc3.digitaloceanspaces.com", "ttl": 600,
             "custom_domain": "static.example.com", "certificate_id": "892071a0-bb95-49bc-8021-3afd67a210bf"}
            """)));
    }
}
