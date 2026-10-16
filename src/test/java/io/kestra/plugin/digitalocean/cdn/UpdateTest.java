package io.kestra.plugin.digitalocean.cdn;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.models.property.Property;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
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

class UpdateTest extends AbstractDigitalOceanTest {

    private static final String PATH = "/v2/cdn/endpoints/19f06b6a-3ace-4315-b086-499a0e521b76";

    private Update.UpdateBuilder<?, ?> builder(WireMockRuntimeInfo wireMockRuntimeInfo) {
        return Update.builder()
            .id(IdUtils.create())
            .type(Update.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .endpointId(Property.ofValue("19f06b6a-3ace-4315-b086-499a0e521b76"));
    }

    @Test
    void sendsOnlyTheTtlWhenOnlyTheTtlIsSet(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubFor(put(urlPathEqualTo(PATH)).willReturn(okJson(GetTest.ENDPOINT_JSON)));

        var output = builder(wireMockRuntimeInfo).ttl(Property.ofValue(600)).build().run(runContext());

        assertThat(output.getId(), is("19f06b6a-3ace-4315-b086-499a0e521b76"));
        verifyBearer(putRequestedFor(urlPathEqualTo(PATH)), "test-token");
        verify(putRequestedFor(urlPathEqualTo(PATH)).withRequestBody(equalToJson("""
            {"ttl": 600}
            """)));
    }

    @Test
    void sendsTheCustomDomainWithItsCertificate(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubFor(put(urlPathEqualTo(PATH)).willReturn(okJson(GetTest.ENDPOINT_JSON)));

        builder(wireMockRuntimeInfo)
            .customDomain(Property.ofValue("static.example.com"))
            .certificateId(Property.ofValue("892071a0-bb95-49bc-8021-3afd67a210bf"))
            .build()
            .run(runContext());

        verify(putRequestedFor(urlPathEqualTo(PATH)).withRequestBody(equalToJson("""
            {"custom_domain": "static.example.com", "certificate_id": "892071a0-bb95-49bc-8021-3afd67a210bf"}
            """)));
    }

    @Test
    void rejectsAnUpdateWithNothingToChange(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var task = builder(wireMockRuntimeInfo).build();

        var runContext = runContext();
        var ex = assertThrows(IllegalArgumentException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("Nothing to update"));
        verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void rejectsAnUnsupportedTtlBeforeCallingTheApi(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var task = builder(wireMockRuntimeInfo).ttl(Property.ofValue(1800)).build();

        var runContext = runContext();
        var ex = assertThrows(IllegalArgumentException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("got 1800"));
        verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void failsWithClearMessageWhenNotFound(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubFor(put(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(404).withBody("{\"message\":\"not found\"}")));

        var task = builder(wireMockRuntimeInfo).ttl(Property.ofValue(600)).build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("HTTP 404"));
    }

    @Test
    void rendersThePropertiesFromExpressions(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubFor(put(urlPathEqualTo(PATH)).willReturn(okJson(GetTest.ENDPOINT_JSON)));

        builder(wireMockRuntimeInfo)
            .endpointId(Property.ofExpression("{{ inputs.endpointId }}"))
            .ttl(Property.ofExpression("{{ inputs.ttl }}"))
            .build()
            .run(runContextFactory.of(Map.of("inputs", Map.of(
                "endpointId", "19f06b6a-3ace-4315-b086-499a0e521b76",
                "ttl", 604800
            ))));

        verify(putRequestedFor(urlPathEqualTo(PATH)).withRequestBody(equalToJson("""
            {"ttl": 604800}
            """)));
    }
}
