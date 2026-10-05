package io.kestra.plugin.digitalocean.snapshot;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
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
    title = "Delete a DigitalOcean snapshot",
    description = "Permanently destroys a droplet or volume snapshot."
)
@Plugin(
    examples = {
        @Example(
            title = "Delete a volume snapshot",
            full = true,
            code = """
                id: digitalocean_delete_snapshot
                namespace: company.team

                tasks:
                  - id: delete_snapshot
                    type: io.kestra.plugin.digitalocean.snapshot.Delete
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    snapshotId: "fbe805e8-866b-11e6-96bf-000f53315a41"
                """
        )
    }
)
public class Delete extends AbstractDigitalOceanTask implements RunnableTask<VoidOutput> {

    @Schema(title = "Snapshot ID", description = "ID of the snapshot to delete: numeric for a droplet snapshot, a UUID for a volume snapshot.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> snapshotId;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rSnapshotId = requireRendered(runContext, snapshotId, String.class, "snapshotId");
        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        logger.info("Deleting DigitalOcean snapshot {}", rSnapshotId);

        var url = join(rBaseUrl, "v2/snapshots/" + encodePathSegment(rSnapshotId));
        var requestBuilder = HttpRequest.builder().uri(URI.create(url)).method("DELETE");
        request(runContext, options, rApiToken, requestBuilder, String.class);

        return null;
    }
}
