package io.kestra.plugin.digitalocean.snapshot;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanListTask;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Locale;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List DigitalOcean snapshots",
    description = "Lists droplet and volume snapshots on the account, optionally filtered by resource type, " +
        "following DigitalOcean's page-based pagination automatically."
)
@Plugin(
    examples = {
        @Example(
            title = "List volume snapshots and log how many exist",
            full = true,
            code = """
                id: digitalocean_list_snapshots
                namespace: company.team

                tasks:
                  - id: list_snapshots
                    type: io.kestra.plugin.digitalocean.snapshot.List
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                    resourceType: VOLUME
                  - id: log_total
                    type: io.kestra.plugin.core.log.Log
                    message: "Found {{ outputs.list_snapshots.total }} volume snapshot(s)"
                """
        )
    }
)
public class List extends AbstractDigitalOceanListTask {

    @Schema(title = "Resource type", description = "Only list snapshots taken from this type of resource: DROPLET or VOLUME. Leave empty to list both.")
    @PluginProperty(group = "main")
    private Property<SnapshotResourceType> resourceType;

    @Override
    protected String path(RunContext runContext) throws Exception {
        var rResourceType = runContext.render(resourceType).as(SnapshotResourceType.class);
        return rResourceType
            .map(type -> "v2/snapshots?resource_type=" + type.name().toLowerCase(Locale.ROOT))
            .orElse("v2/snapshots");
    }

    @Override
    protected String arrayKey() {
        return "snapshots";
    }

    @Override
    protected String resourceLabel() {
        return "snapshot(s)";
    }
}
