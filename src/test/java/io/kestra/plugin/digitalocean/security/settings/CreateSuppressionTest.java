package io.kestra.plugin.digitalocean.security.settings;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.models.property.Property;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateSuppressionTest extends AbstractDigitalOceanTest {

    private static final String SUPPRESSIONS_PATH = "/v2/security/settings/suppressions";
    private static final String RULE_ID = "497dcba3-ecbf-4587-a2dd-5eb0665e6880";
    private static final String DROPLET_URN = "do:droplet:fe3a2fd7-903d-46e6-ada3-3e4f285fb89d";

    private static final String CREATED_JSON = """
        {"resources": [{"id": "760e09ef-dc84-11e8-981e-3cfdfeaae000", "rule_uuid": "497dcba3-ecbf-4587-a2dd-5eb0665e6880",
           "rule_name": "Droplet Backups Not Enabled", "resource_id": "123", "resource_type": "Droplet"}],
         "meta": {"page": 1, "pages": 1, "total": 1}, "links": {"pages": {}}}
        """;

    private CreateSuppression.CreateSuppressionBuilder<?, ?> task(WireMockRuntimeInfo wireMockRuntimeInfo, List<String> resources) {
        return CreateSuppression.builder()
            .id(IdUtils.create())
            .type(CreateSuppression.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .ruleId(Property.ofValue(RULE_ID))
            .resources(Property.ofValue(resources));
    }

    @Test
    void suppressesTheRuleAndSendsBearerToken(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubPostJson(SUPPRESSIONS_PATH, 201, CREATED_JSON);

        var output = task(wireMockRuntimeInfo, List.of(DROPLET_URN)).build().run(runContext());

        assertThat(output.getSuppressions(), hasSize(1));
        assertThat(output.getSuppressions().getFirst().get("id"), is("760e09ef-dc84-11e8-981e-3cfdfeaae000"));
        assertThat(output.getSuppressions().getFirst().get("resource_type"), is("Droplet"));
        assertThat(output.getSuppressionsTotal(), is(1L));
        verify(postRequestedFor(urlPathEqualTo(SUPPRESSIONS_PATH)).withRequestBody(equalToJson(
            "{\"rule_uuid\": \"" + RULE_ID + "\", \"resources\": [\"" + DROPLET_URN + "\"]}"
        )));
        verifyBearer(postRequestedFor(urlPathEqualTo(SUPPRESSIONS_PATH)), "test-token");
    }

    @Test
    void rejectsAnEmptyResourceListBeforeAnyRequest(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var task = task(wireMockRuntimeInfo, List.of()).build();

        var runContext = runContext();
        var ex = assertThrows(IllegalArgumentException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("resources needs at least one resource URN"));
        verify(0, postRequestedFor(urlPathEqualTo(SUPPRESSIONS_PATH)));
    }

    @Test
    void failsWithTheApiReasonWhenTheRuleIsUnknown(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubPostJson(SUPPRESSIONS_PATH, 404, "{\"id\":\"not_found\",\"message\":\"rule not found\"}");

        var task = task(wireMockRuntimeInfo, List.of(DROPLET_URN)).build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("HTTP 404"));
        assertThat(ex.getMessage(), containsString("rule not found"));
    }

    @Test
    void rendersTheRuleAndTheResourcesFromExpressions(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubPostJson(SUPPRESSIONS_PATH, 201, CREATED_JSON);

        var task = task(wireMockRuntimeInfo, List.of(DROPLET_URN))
            .ruleId(Property.ofExpression("{{ outputs.scan.findings[0].rule_uuid }}"))
            .resources(Property.ofExpression("[\"{{ inputs.urn }}\"]"))
            .build();

        task.run(runContextFactory.of(Map.of(
            "outputs", Map.of("scan", Map.of("findings", List.of(Map.of("rule_uuid", RULE_ID)))),
            "inputs", Map.of("urn", DROPLET_URN)
        )));

        verify(postRequestedFor(urlPathEqualTo(SUPPRESSIONS_PATH)).withRequestBody(equalToJson(
            "{\"rule_uuid\": \"" + RULE_ID + "\", \"resources\": [\"" + DROPLET_URN + "\"]}"
        )));
    }
}
