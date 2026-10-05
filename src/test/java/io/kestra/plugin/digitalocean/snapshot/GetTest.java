package io.kestra.plugin.digitalocean.snapshot;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.models.property.Property;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetTest extends AbstractDigitalOceanTest {

    private static final String VOLUME_SNAPSHOT_JSON = """
        {
          "snapshot": {
            "id": "fbe805e8-866b-11e6-96bf-000f53315a41",
            "name": "big-data-snapshot1475261774",
            "regions": ["nyc1", "ams3"],
            "created_at": "2016-09-30T18:56:12Z",
            "resource_id": "82a48a18-873f-11e6-96bf-000f53315a41",
            "resource_type": "volume",
            "min_disk_size": 10,
            "size_gigabytes": 0.42,
            "tags": ["backup", "env:prod"]
          }
        }
        """;

    private static final String DROPLET_SNAPSHOT_JSON = """
        {
          "snapshot": {
            "id": 6372321,
            "name": "web-01-1595954862243",
            "regions": ["nyc3"],
            "created_at": "2020-07-28T16:47:44Z",
            "resource_id": 200776916,
            "resource_type": "droplet",
            "min_disk_size": 25,
            "size_gigabytes": 2,
            "tags": null
          }
        }
        """;

    @Test
    void fetchesVolumeSnapshotAndSendsBearerToken(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson("/v2/snapshots/fbe805e8-866b-11e6-96bf-000f53315a41", VOLUME_SNAPSHOT_JSON);

        var task = Get.builder()
            .id(IdUtils.create())
            .type(Get.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .snapshotId(Property.ofValue("fbe805e8-866b-11e6-96bf-000f53315a41"))
            .build();

        var output = task.run(runContext());

        assertThat(output.getId(), is("fbe805e8-866b-11e6-96bf-000f53315a41"));
        assertThat(output.getName(), is("big-data-snapshot1475261774"));
        assertThat(output.getResourceType(), is("volume"));
        assertThat(output.getResourceId(), is("82a48a18-873f-11e6-96bf-000f53315a41"));
        assertThat(output.getRegions(), is(List.of("nyc1", "ams3")));
        assertThat(output.getMinDiskSize(), is(10));
        assertThat(output.getSizeGigabytes(), is(0.42));
        assertThat(output.getTags(), is(List.of("backup", "env:prod")));
        assertThat(output.getCreatedAt(), is(Instant.parse("2016-09-30T18:56:12Z")));
        verifyBearer(getRequestedFor(urlPathEqualTo("/v2/snapshots/fbe805e8-866b-11e6-96bf-000f53315a41")), "test-token");
    }

    @Test
    void keepsNumericDropletSnapshotIdsAsStrings(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson("/v2/snapshots/6372321", DROPLET_SNAPSHOT_JSON);

        var task = Get.builder()
            .id(IdUtils.create())
            .type(Get.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .snapshotId(Property.ofValue("6372321"))
            .build();

        var output = task.run(runContext());

        assertThat(output.getId(), is("6372321"));
        assertThat(output.getResourceId(), is("200776916"));
        assertThat(output.getSizeGigabytes(), is(2.0));
        assertThat(output.getTags(), is(nullValue()));
    }

    @Test
    void failsWithClearMessageWhenNotFound(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubStatus("/v2/snapshots/missing", 404, "{\"message\":\"not found\"}");

        var task = Get.builder()
            .id(IdUtils.create())
            .type(Get.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .snapshotId(Property.ofValue("missing"))
            .build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("resource not found"));
    }

    @Test
    void rendersTokenAndSnapshotIdFromExpressions(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubGetJson("/v2/snapshots/6372321", DROPLET_SNAPSHOT_JSON);

        var task = Get.builder()
            .id(IdUtils.create())
            .type(Get.class.getName())
            .apiToken(Property.ofExpression("{{ inputs.token }}"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .snapshotId(Property.ofExpression("{{ inputs.snapshotId }}"))
            .build();

        var output = task.run(runContextFactory.of(Map.of("inputs", Map.of("token", "rendered-token", "snapshotId", "6372321"))));

        assertThat(output.getId(), is("6372321"));
        verifyBearer(getRequestedFor(urlPathEqualTo("/v2/snapshots/6372321")), "rendered-token");
    }
}
