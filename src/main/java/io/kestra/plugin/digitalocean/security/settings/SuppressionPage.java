package io.kestra.plugin.digitalocean.security.settings;

import io.kestra.plugin.digitalocean.AbstractDigitalOceanTask;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.kestra.plugin.digitalocean.AbstractDigitalOceanTask.asMap;

/** DigitalOcean returns suppressions in the same {resources, meta, links} block from the settings and from a create. */
record SuppressionPage(List<Map<String, Object>> rows, long total) {

    static SuppressionPage from(Map<String, Object> block) {
        if (block == null || !(block.get("resources") instanceof List<?> list)) {
            return new SuppressionPage(List.of(), 0);
        }
        var rows = list.stream()
            .map(AbstractDigitalOceanTask::asMap)
            .filter(Objects::nonNull)
            .toList();
        var meta = asMap(block.get("meta"));
        var total = meta != null && meta.get("total") instanceof Number number ? number.longValue() : rows.size();

        return new SuppressionPage(rows, total);
    }
}
