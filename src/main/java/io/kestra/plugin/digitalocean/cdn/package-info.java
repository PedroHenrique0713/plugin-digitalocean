@PluginSubGroup(
    title = "CDN",
    description = "Tasks for managing DigitalOcean CDN endpoints in front of Spaces buckets: list, read, create, " +
        "update, and delete endpoints, and purge their cache (https://docs.digitalocean.com/products/spaces/how-to/enable-cdn/).",
    categories = PluginSubGroup.PluginCategory.CLOUD
)
package io.kestra.plugin.digitalocean.cdn;

import io.kestra.core.models.annotations.PluginSubGroup;
