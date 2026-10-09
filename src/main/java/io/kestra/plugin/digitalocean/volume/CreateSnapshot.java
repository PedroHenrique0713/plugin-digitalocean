package io.kestra.plugin.digitalocean.volume;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTask;
import io.kestra.plugin.digitalocean.snapshot.SnapshotOutput;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Create a snapshot of a DigitalOcean volume",
    description = "Takes a point-in-time snapshot of a block storage volume. Use `snapshot.List`, `snapshot.Get` " +
        "and `snapshot.Delete` to manage it afterwards."
)
@Plugin(
    examples = {
        @Example(
            title = "Snapshot a volume and log the snapshot ID",
            full = true,
            code = """
                id: digitalocean_snapshot_volume
                namespace: company.team

                tasks:
                  - id: snapshot_volume
                    type: io.kestra.plugin.digitalocean.volume.CreateSnapshot
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    volumeId: "3fa85f64-5717-4562-b3fc-2c963f66afa6"
                    name: "data-volume-{{ now() | date('yyyyMMddHHmm') }}"
                    tags:
                      - backup
                  - id: log_snapshot
                    type: io.kestra.plugin.core.log.Log
                    message: "Created snapshot {{ outputs.snapshot_volume.id }}"
                """
        )
    }
)
public class CreateSnapshot extends AbstractDigitalOceanTask implements RunnableTask<SnapshotOutput> {

    @Schema(title = "Volume ID", description = "UUID of the volume to snapshot.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> volumeId;

    @Schema(title = "Snapshot name", description = "Human-readable name for the snapshot.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> name;

    @Schema(title = "Tags", description = "Optional tags to apply to the snapshot.")
    @PluginProperty(group = "advanced")
    private Property<List<String>> tags;

    @Override
    public SnapshotOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rVolumeId = requireRendered(runContext, volumeId, String.class, "volumeId");
        var rName = requireRendered(runContext, name, String.class, "name");
        var rTags = runContext.render(tags).asList(String.class);
        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        var payload = new LinkedHashMap<String, Object>();
        payload.put("name", rName);
        if (!rTags.isEmpty()) {
            payload.put("tags", rTags);
        }

        logger.info("Creating snapshot '{}' of DigitalOcean volume {}", rName, rVolumeId);

        var url = join(rBaseUrl, "v2/volumes/" + encodePathSegment(rVolumeId) + "/snapshots");
        var requestBuilder = HttpRequest.builder()
            .uri(URI.create(url))
            .method("POST")
            .body(HttpRequest.JsonRequestBody.of(payload));

        var body = requestJson(runContext, options, rApiToken, requestBuilder);
        return SnapshotOutput.from(unwrap(body, "snapshot"));
    }
}
