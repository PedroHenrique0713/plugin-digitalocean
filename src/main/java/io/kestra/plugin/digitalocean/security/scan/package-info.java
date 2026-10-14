@PluginSubGroup(
    title = "Security Scans",
    description = "Tasks for DigitalOcean security (CSPM) scans: start a scan, list scans, read a scan and its " +
        "findings, and list the resources affected by a finding (https://docs.digitalocean.com/products/cspm/).",
    categories = PluginSubGroup.PluginCategory.CLOUD
)
package io.kestra.plugin.digitalocean.security.scan;

import io.kestra.core.models.annotations.PluginSubGroup;
