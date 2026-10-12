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
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeleteSuppressionTest extends AbstractDigitalOceanTest {

    private static final String SUPPRESSION_ID = "5b3b2b2d-5c9c-4a61-9e2f-4d8f80f30a12";
    private static final String SUPPRESSION_PATH = "/v2/security/settings/suppressions/" + SUPPRESSION_ID;

    private DeleteSuppression.DeleteSuppressionBuilder<?, ?> task(WireMockRuntimeInfo wireMockRuntimeInfo) {
        return DeleteSuppression.builder()
            .id(IdUtils.create())
            .type(DeleteSuppression.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .suppressionId(Property.ofValue(SUPPRESSION_ID));
    }

    @Test
    void removesTheSuppressionAndSendsBearerToken(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubFor(delete(urlPathEqualTo(SUPPRESSION_PATH)).willReturn(aResponse().withStatus(204)));

        var output = task(wireMockRuntimeInfo).build().run(runContext());

        assertThat(output, nullValue());
        verifyBearer(deleteRequestedFor(urlPathEqualTo(SUPPRESSION_PATH)), "test-token");
    }

    @Test
    void failsWithClearMessageWhenNotFound(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubFor(delete(urlPathEqualTo(SUPPRESSION_PATH)).willReturn(aResponse()
            .withStatus(404)
            .withBody("{\"id\":\"not_found\",\"message\":\"suppression not found\"}")));

        var task = task(wireMockRuntimeInfo).build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("HTTP 404"));
        assertThat(ex.getMessage(), containsString("suppression not found"));
    }

    @Test
    void rendersTheSuppressionIdFromAnExpression(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubFor(delete(urlPathEqualTo(SUPPRESSION_PATH)).willReturn(aResponse().withStatus(204)));

        var task = task(wireMockRuntimeInfo)
            .suppressionId(Property.ofExpression("{{ outputs.suppress.suppressions[0].id }}"))
            .build();

        task.run(runContextFactory.of(Map.of(
            "outputs", Map.of("suppress", Map.of("suppressions", List.of(Map.of("id", SUPPRESSION_ID))))
        )));

        verify(deleteRequestedFor(urlPathEqualTo(SUPPRESSION_PATH)));
    }
}
