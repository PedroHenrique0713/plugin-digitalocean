package io.kestra.plugin.digitalocean.security.settings;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A plain bean, not a Property field, so a {@code Property<java.util.Map<String, TierCoverage>>} renders the whole map at once. */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class TierCoverage {

    @Schema(title = "Resources", description = "URNs of the resources the tier scans, e.g. do:droplet:fe3a2fd7-903d-46e6-ada3-3e4f285fb89d.")
    private List<String> resources;

    @Schema(title = "Tags", description = "Resource tags the tier scans.")
    private List<String> tags;

    Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("resources", resources != null ? resources : List.of());
        map.put("tags", tags != null ? tags : List.of());
        return map;
    }
}
