package io.kestra.plugin.digitalocean.snapshot;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.models.property.Property;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListTest extends AbstractDigitalOceanTest {

    private static final String SNAPSHOTS_JSON = """
        {
          "snapshots": [
            {"id": "6372321", "name": "web-01-1595954862243", "regions": ["nyc3"], "resource_id": "200776916", "resource_type": "droplet", "min_disk_size": 25, "size_gigabytes": 2.34, "created_at": "2020-07-28T16:47:44Z"},
            {"id": "fbe805e8-866b-11e6-96bf-000f53315a41", "name": "big-data-snapshot", "regions": ["nyc1"], "resource_id": "82a48a18-873f-11e6-96bf-000f53315a41", "resource_type": "volume", "min_disk_size": 10, "size_gigabytes": 0.0, "created_at": "2016-09-28T23:14:30Z"}
          ],
          "links": {"pages": {}},
          "meta": {"total": 2}
        }
        """;

    @Test
    void listsAllSnapshotsWithoutFilter(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson("/v2/snapshots", SNAPSHOTS_JSON);

        var task = List.builder()
            .id(IdUtils.create())
            .type(List.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .build();

        var output = task.run(runContext());

        assertThat(output.getTotal(), is(2L));
        assertThat(output.getRows().getFirst().get("name"), is("web-01-1595954862243"));
        verifyBearer(getRequestedFor(urlPathEqualTo("/v2/snapshots")), "test-token");
        verify(getRequestedFor(urlPathEqualTo("/v2/snapshots")).withQueryParam("resource_type", absent()));
    }

    @Test
    void filtersByResourceType(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson("/v2/snapshots", SNAPSHOTS_JSON);

        var task = List.builder()
            .id(IdUtils.create())
            .type(List.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .resourceType(Property.ofValue(SnapshotResourceType.VOLUME))
            .build();

        task.run(runContext());

        verify(getRequestedFor(urlPathEqualTo("/v2/snapshots"))
            .withQueryParam("resource_type", equalTo("volume"))
            .withQueryParam("per_page", equalTo("200")));
    }

    @Test
    void failsWithClearMessageOnRateLimit(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubStatusWithHeader("/v2/snapshots", 429, "{\"message\":\"too many requests\"}", "retry-after", "20");

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
    void rendersResourceTypeFromAnExpression(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson("/v2/snapshots", SNAPSHOTS_JSON);

        var task = List.builder()
            .id(IdUtils.create())
            .type(List.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .resourceType(Property.ofExpression("{{ inputs.resourceType }}"))
            .build();

        task.run(runContextFactory.of(Map.of("inputs", Map.of("resourceType", "DROPLET"))));

        verify(getRequestedFor(urlPathEqualTo("/v2/snapshots")).withQueryParam("resource_type", equalTo("droplet")));
    }
}
