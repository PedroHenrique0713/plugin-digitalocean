package io.kestra.plugin.digitalocean.volume;

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
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateSnapshotTest extends AbstractDigitalOceanTest {

    private static final String SNAPSHOT_JSON = """
        {
          "snapshot": {
            "id": "8fa70202-873f-11e6-8b68-000f533176b1",
            "name": "big-data-snapshot1475261774",
            "regions": ["nyc1"],
            "created_at": "2016-09-28T23:14:30Z",
            "resource_id": "82a48a18-873f-11e6-96bf-000f53315a41",
            "resource_type": "volume",
            "min_disk_size": 10,
            "size_gigabytes": 0,
            "tags": ["backup"]
          }
        }
        """;

    private static final String VOLUME_SNAPSHOTS_PATH = "/v2/volumes/82a48a18-873f-11e6-96bf-000f53315a41/snapshots";

    @Test
    void createsSnapshotWithTagsAndSendsBearerToken(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubPostJson(VOLUME_SNAPSHOTS_PATH, 201, SNAPSHOT_JSON);

        var task = CreateSnapshot.builder()
            .id(IdUtils.create())
            .type(CreateSnapshot.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .volumeId(Property.ofValue("82a48a18-873f-11e6-96bf-000f53315a41"))
            .name(Property.ofValue("big-data-snapshot1475261774"))
            .tags(Property.ofValue(List.of("backup")))
            .build();

        var output = task.run(runContext());

        assertThat(output.getId(), is("8fa70202-873f-11e6-8b68-000f533176b1"));
        assertThat(output.getResourceType(), is("volume"));
        assertThat(output.getTags(), is(List.of("backup")));
        verifyBearer(postRequestedFor(urlPathEqualTo(VOLUME_SNAPSHOTS_PATH)), "test-token");
        verify(postRequestedFor(urlPathEqualTo(VOLUME_SNAPSHOTS_PATH))
            .withRequestBody(equalToJson("{\"name\": \"big-data-snapshot1475261774\", \"tags\": [\"backup\"]}")));
    }

    @Test
    void omitsTagsWhenNoneAreGiven(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubPostJson(VOLUME_SNAPSHOTS_PATH, 201, SNAPSHOT_JSON);

        var task = CreateSnapshot.builder()
            .id(IdUtils.create())
            .type(CreateSnapshot.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .volumeId(Property.ofValue("82a48a18-873f-11e6-96bf-000f53315a41"))
            .name(Property.ofValue("big-data-snapshot1475261774"))
            .build();

        task.run(runContext());

        verify(postRequestedFor(urlPathEqualTo(VOLUME_SNAPSHOTS_PATH))
            .withRequestBody(equalToJson("{\"name\": \"big-data-snapshot1475261774\"}")));
    }

    @Test
    void failsWithClearMessageWhenVolumeNotFound(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubPostJson("/v2/volumes/missing/snapshots", 404, "{\"message\":\"not found\"}");

        var task = CreateSnapshot.builder()
            .id(IdUtils.create())
            .type(CreateSnapshot.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .volumeId(Property.ofValue("missing"))
            .name(Property.ofValue("snapshot"))
            .build();

        var runContext = runContext();
        var ex = assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
        assertThat(ex.getMessage(), containsString("resource not found"));
    }

    @Test
    void rendersVolumeIdAndNameFromExpressions(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        stubPostJson(VOLUME_SNAPSHOTS_PATH, 201, SNAPSHOT_JSON);

        var task = CreateSnapshot.builder()
            .id(IdUtils.create())
            .type(CreateSnapshot.class.getName())
            .apiToken(Property.ofValue("test-token"))
            .baseUrl(Property.ofValue(wireMockRuntimeInfo.getHttpBaseUrl()))
            .volumeId(Property.ofExpression("{{ inputs.volumeId }}"))
            .name(Property.ofExpression("nightly-{{ inputs.day }}"))
            .build();

        task.run(runContextFactory.of(Map.of("inputs", Map.of("volumeId", "82a48a18-873f-11e6-96bf-000f53315a41", "day", "2026-10-06"))));

        verify(postRequestedFor(urlPathEqualTo(VOLUME_SNAPSHOTS_PATH))
            .withRequestBody(equalToJson("{\"name\": \"nightly-2026-10-06\"}")));
    }
}
