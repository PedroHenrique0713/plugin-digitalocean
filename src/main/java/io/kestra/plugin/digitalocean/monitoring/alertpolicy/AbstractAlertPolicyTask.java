package io.kestra.plugin.digitalocean.monitoring.alertpolicy;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTask;
import io.kestra.plugin.digitalocean.monitoring.models.AlertChannels;
import io.kestra.plugin.digitalocean.monitoring.models.Compare;
import io.kestra.plugin.digitalocean.monitoring.models.PolicyType;
import io.kestra.plugin.digitalocean.monitoring.models.Window;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractAlertPolicyTask extends AbstractDigitalOceanTask {

    @Schema(title = "Alert policy metric type")
    @NotNull
    @PluginProperty(group = "main")
    protected Property<PolicyType> policyType;

    @Schema(title = "Alert policy description")
    @NotNull
    @PluginProperty(group = "main")
    protected Property<String> policyDescription;

    @Schema(title = "Comparison operator")
    @NotNull
    @PluginProperty(group = "main")
    protected Property<Compare> compare;

    @Schema(title = "Threshold value")
    @NotNull
    @PluginProperty(group = "main")
    protected Property<Double> value;

    @Schema(title = "Evaluation window")
    @NotNull
    @PluginProperty(group = "main")
    protected Property<Window> window;

    @Schema(title = "Entities monitored by droplet ID")
    @PluginProperty(group = "main")
    protected Property<List<String>> entities;

    @Schema(title = "Tags monitored by the alert policy")
    @PluginProperty(group = "main")
    protected Property<List<String>> tags;

    @Schema(title = "Notification channels")
    @NotNull
    @PluginProperty(group = "main")
    @ToString.Exclude
    protected Property<AlertChannels> alerts;

    @Schema(title = "Whether the alert policy is enabled", description = "Defaults to true")
    @Builder.Default
    @PluginProperty(group = "main")
    protected Property<Boolean> enabled = Property.ofValue(true);

    protected Map<String, Object> buildPayload(RunContext runContext) throws Exception {
        var rType = requireRendered(runContext, policyType, PolicyType.class, "policyType");
        var rDescription = requireRendered(runContext, policyDescription, String.class, "policyDescription");
        var rCompare = requireRendered(runContext, compare, Compare.class, "compare");
        var rValue = requireRendered(runContext, value, Double.class, "value");
        var rWindow = requireRendered(runContext, window, Window.class, "window");
        var rEntities = runContext.render(entities).asList(String.class);
        var rTags = runContext.render(tags).asList(String.class);
        var rAlerts = requireRendered(runContext, alerts, AlertChannels.class, "alerts");
        var rEnabled = runContext.render(enabled).as(Boolean.class).orElse(true);

        var payload = new HashMap<String, Object>();
        payload.put("type", rType.getValue());
        payload.put("description", rDescription);
        payload.put("compare", rCompare.getValue());
        payload.put("value", rValue);
        payload.put("window", rWindow.getValue());
        if (!rEntities.isEmpty()) {
            payload.put("entities", rEntities);
        }
        if (!rTags.isEmpty()) {
            payload.put("tags", rTags);
        }
        payload.put("alerts", rAlerts.toMap());
        payload.put("enabled", rEnabled);
        return payload;
    }
}
