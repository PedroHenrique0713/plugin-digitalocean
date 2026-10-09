@PluginSubGroup(
    title = "Volumes",
    description = "Tasks for managing DigitalOcean block storage volumes: list, read, create, delete, " +
        "attach or detach volumes from droplets, and take volume snapshots (https://docs.digitalocean.com/products/volumes/).",
    categories = PluginSubGroup.PluginCategory.CLOUD
)
package io.kestra.plugin.digitalocean.volume;

import io.kestra.core.models.annotations.PluginSubGroup;
