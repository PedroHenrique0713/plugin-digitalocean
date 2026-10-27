@PluginSubGroup(
    title = "Security Settings",
    description = "Tasks for DigitalOcean security (CSPM) settings: read the scan settings, set the resources and tags " +
        "each plan tier scans, and suppress a rule's findings on given resources or remove a suppression " +
        "(https://docs.digitalocean.com/products/cspm/).",
    categories = PluginSubGroup.PluginCategory.CLOUD
)
package io.kestra.plugin.digitalocean.security.settings;

import io.kestra.core.models.annotations.PluginSubGroup;
