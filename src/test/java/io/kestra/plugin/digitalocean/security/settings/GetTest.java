package io.kestra.plugin.digitalocean.security.settings;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.models.property.Property;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetTest extends AbstractDigitalOceanTest {

    private static final String SETTINGS_PATH = "/v2/security/settings";
    private static final String DROPLET_URN = "do:droplet:fe3a2fd7-903d-46e6-ada3-3e4f285fb89d";

    private static final String SETTINGS_JSON = """
        {"settings": {"suppressions": {
            "resources": [{"id": "760e09ef-dc84-11e8-981e-3cfdfeaae000", "rule_uuid": "460e09ef-dc84-11e8-981e-3cfdfeaae000",
              "rule_name": "Droplet Backups Not Enabled", "resource_id": "123", "resource_type": "Droplet"}],
            "meta": {"page": 1, "pages": 25, "total": 25},
            "links": {"pages": {"next": "https://api.digitalocean.com/v2/security/settings/suppressions?page=2"}}}},
         "tier_coverage": {"basic": {"resources": ["do:droplet:fe3a2fd7-903d-46e6-ada3-3e4f285fb89d"], "tags": ["production"]}},
         "plan_downgrades": {"basic": {"resources": ["do:droplet:fe3a2fd7-903d-46e6-ada3-3e4f285fb89d"], "effective_at": "2026-11-01T00:00:00Z"}}}
        """;

    private Get.GetBuilder<?, ?> task(WireMockRuntimeInfo wireMockRuntimeInfo) {
        return Get.builder()
            .id(IdUtils.create())
            .type(Get.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()));
    }

    @Test
    void readsSettingsAndSendsBearerToken(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson(SETTINGS_PATH, SETTINGS_JSON);

        var output = task(wireMockRuntimeInfo).build().run(runContext());

        var basic = (Map<?, ?>) output.getTierCoverage().get("basic");
        assertThat(basic.get("tags"), is(List.of("production")));
        assertThat(basic.get("resources"), is(List.of(DROPLET_URN)));
        assertThat(((Map<?, ?>) output.getPlanDowngrades().get("basic")).get("effective_at"), is("2026-11-01T00:00:00Z"));
        assertThat(output.getSuppressions(), hasSize(1));
        assertThat(output.getSuppressions().getFirst().get("rule_name"), is("Droplet Backups Not Enabled"));
        assertThat(output.getSuppressions().getFirst().get("id"), is("760e09ef-dc84-11e8-981e-3cfdfeaae000"));
        assertThat(output.getSuppressionsTotal(), is(25L));
        verify(1, getRequestedFor(urlPathEqualTo(SETTINGS_PATH))
            .withQueryParam("per_page", equalTo("200"))
            .withQueryParam("page", equalTo("1")));
        verifyBearer(getRequestedFor(urlPathEqualTo(SETTINGS_PATH)), "test-token");
    }

    @Test
    void sendsThePageAndThePageSize(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson(SETTINGS_PATH, SETTINGS_JSON);

        task(wireMockRuntimeInfo).perPage(Property.ofValue(50)).page(Property.ofValue(2)).build().run(runContext());

        verify(getRequestedFor(urlPathEqualTo(SETTINGS_PATH))
            .withQueryParam("per_page", equalTo("50"))
            .withQueryParam("page", equalTo("2")));
    }

    @Test
    void rejectsAPageSizeAboveTheApiMaximumBeforeAnyRequest(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var task = task(wireMockRuntimeInfo).perPage(Property.ofValue(201)).build();

        var runContext = runContext();
        var ex = assertThrows(IllegalArgumentException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("perPage must be between 1 and 200"));
        verify(0, getRequestedFor(urlPathEqualTo(SETTINGS_PATH)));
    }

    @Test
    void returnsEmptySettingsWhenTheApiOmitsThem(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson(SETTINGS_PATH, "{}");

        var output = task(wireMockRuntimeInfo).build().run(runContext());

        assertThat(output.getTierCoverage(), is(anEmptyMap()));
        assertThat(output.getPlanDowngrades(), is(anEmptyMap()));
        assertThat(output.getSuppressions(), is(empty()));
        assertThat(output.getSuppressionsTotal(), is(0L));
    }

    @Test
    void failsWithClearMessageWhenNotFound(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubStatus(SETTINGS_PATH, 404, "{\"id\":\"not_found\",\"message\":\"CSPM is not enabled for this account\"}");

        var task = task(wireMockRuntimeInfo).build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("HTTP 404"));
        assertThat(ex.getMessage(), containsString("CSPM is not enabled for this account"));
    }

    @Test
    void rendersThePageFromAnExpression(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson(SETTINGS_PATH, SETTINGS_JSON);

        var task = task(wireMockRuntimeInfo).page(Property.ofExpression("{{ inputs.page }}")).build();

        task.run(runContextFactory.of(Map.of("inputs", Map.of("page", 3))));

        verify(getRequestedFor(urlPathEqualTo(SETTINGS_PATH)).withQueryParam("page", equalTo("3")));
    }
}
