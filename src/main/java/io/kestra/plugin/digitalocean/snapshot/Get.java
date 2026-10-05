package io.kestra.plugin.digitalocean.snapshot;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTask;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URI;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Get a DigitalOcean snapshot",
    description = "Reads a single droplet or volume snapshot's details from the DigitalOcean API."
)
@Plugin(
    examples = {
        @Example(
            title = "Get a snapshot and log its size",
            full = true,
            code = """
                id: digitalocean_get_snapshot
                namespace: company.team

                tasks:
                  - id: get_snapshot
                    type: io.kestra.plugin.digitalocean.snapshot.Get
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    snapshotId: "fbe805e8-866b-11e6-96bf-000f53315a41"
                  - id: log_size
                    type: io.kestra.plugin.core.log.Log
                    message: "Snapshot {{ outputs.get_snapshot.name }} takes {{ outputs.get_snapshot.sizeGigabytes }} GB"
                """
        )
    }
)
public class Get extends AbstractDigitalOceanTask implements RunnableTask<SnapshotOutput> {

    @Schema(title = "Snapshot ID", description = "ID of the snapshot to read: numeric for a droplet snapshot, a UUID for a volume snapshot.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> snapshotId;

    @Override
    public SnapshotOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rSnapshotId = requireRendered(runContext, snapshotId, String.class, "snapshotId");
        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        logger.info("Fetching DigitalOcean snapshot {}", rSnapshotId);

        var url = join(rBaseUrl, "v2/snapshots/" + encodePathSegment(rSnapshotId));
        var requestBuilder = HttpRequest.builder().uri(URI.create(url)).method("GET");
        var body = requestJson(runContext, options, rApiToken, requestBuilder);

        return SnapshotOutput.from(unwrap(body, "snapshot"));
    }
}
