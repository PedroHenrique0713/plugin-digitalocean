package io.kestra.plugin.digitalocean.security.scan;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.models.property.Property;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListAffectedResourcesTest extends AbstractDigitalOceanTest {

    private static final String SCAN_ID = "497dcba3-ecbf-4587-a2dd-5eb0665e6880";
    private static final String FINDING_ID = "50e14f43-dd4e-412f-864d-78943ea28d91";
    private static final String RESOURCES_PATH = "/v2/security/scans/" + SCAN_ID + "/findings/" + FINDING_ID + "/affected_resources";

    private static final String RESOURCES_JSON = """
        {
          "affected_resources": [
            {"urn": "do:droplet:3164444", "name": "web-1", "type": "Droplet"},
            {"urn": "do:droplet:3164445", "name": "web-2", "type": "Droplet"}
          ],
          "links": {"pages": {}},
          "meta": {"total": 2}
        }
        """;

    private ListAffectedResources.ListAffectedResourcesBuilder<?, ?> task(WireMockRuntimeInfo wireMockRuntimeInfo) {
        return ListAffectedResources.builder()
            .id(IdUtils.create())
            .type(ListAffectedResources.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .scanId(Property.ofValue(SCAN_ID))
            .findingId(Property.ofValue(FINDING_ID));
    }

    @Test
    void listsAffectedResourcesAndSendsBearerToken(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson(RESOURCES_PATH, RESOURCES_JSON);

        var output = task(wireMockRuntimeInfo).build().run(runContext());

        assertThat(output.getTotal(), is(2L));
        assertThat(output.getSize(), is(2));
        assertThat(output.getRows().getFirst().get("urn"), is("do:droplet:3164444"));
        assertThat(output.getRows().get(1).get("name"), is("web-2"));
        verifyBearer(getRequestedFor(urlPathEqualTo(RESOURCES_PATH)), "test-token");
    }

    @Test
    void failsWithClearMessageWhenTheFindingIsNotFound(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubStatus(RESOURCES_PATH, 404, "{\"id\":\"not_found\",\"message\":\"finding not found\"}");

        var task = task(wireMockRuntimeInfo).build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("HTTP 404"));
        assertThat(ex.getMessage(), containsString("finding not found"));
    }

    @Test
    void rendersTheScanAndFindingIdsFromExpressions(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson(RESOURCES_PATH, RESOURCES_JSON);

        var task = task(wireMockRuntimeInfo)
            .scanId(Property.ofExpression("{{ inputs.scan_id }}"))
            .findingId(Property.ofExpression("{{ inputs.finding_id }}"))
            .build();

        var output = task.run(runContextFactory.of(Map.of("inputs", Map.of("scan_id", SCAN_ID, "finding_id", FINDING_ID))));

        assertThat(output.getTotal(), is(2L));
    }
}
