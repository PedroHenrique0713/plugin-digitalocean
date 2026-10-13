package io.kestra.plugin.digitalocean.monitoring.alertpolicy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class ListTest extends AbstractDigitalOceanTest {

    private static final String LIST_ALERTS_JSON = """
        {
          "policies": [
            {
              "uuid": "7343e061-e0c1-4b71-a476-c56dfaa3d2ad",
              "type": "v1/insights/droplet/cpu",
              "description": "High CPU alert",
              "compare": "greater_than",
              "value": 85.0,
              "window": "5m",
              "entities": ["123456"],
              "tags": ["prod"],
              "alerts": {
                "email": ["ops@company.com"],
                "slack": [{"url": "https://hooks.slack.com/services/xxx", "channel": "#alerts"}]
              },
              "enabled": true
            }
          ],
          "meta": { "total": 1 }
        }
        """;

    @Test
    void listsAlertPoliciesAndSendsBearerToken(WireMockRuntimeInfo wm) throws Exception {
        stubGetJson("/v2/monitoring/alerts", LIST_ALERTS_JSON);

        var task = List.builder()
            .id(IdUtils.create())
            .type(List.class.getName())
            .apiToken(Property.ofValue("secret-token"))
            .baseUrl(Property.ofValue(wm.getHttpBaseUrl()))
            .build();

        var output = task.run(runContext());

        assertThat(output.getTotal(), is(1L));
        assertThat(output.getSize(), is(1));
        assertThat(output.getRows(), notNullValue());
        var firstRow = output.getRows().getFirst();
        assertThat(firstRow.get("uuid"), is("7343e061-e0c1-4b71-a476-c56dfaa3d2ad"));
        assertThat(firstRow.get("description"), is("High CPU alert"));

        // Verify Slack webhook URL is stripped from rows for security
        @SuppressWarnings("unchecked")
        var alerts = (java.util.Map<String, Object>) firstRow.get("alerts");
        assertThat(alerts, notNullValue());
        @SuppressWarnings("unchecked")
        var slack = (java.util.List<java.util.Map<String, Object>>) alerts.get("slack");
        assertThat(slack, notNullValue());
        assertThat(slack.getFirst(), hasEntry("channel", "#alerts"));
        assertThat(slack.getFirst().containsKey("url"), is(false));

        verifyBearer(getRequestedFor(urlEqualTo("/v2/monitoring/alerts?per_page=200")), "secret-token");
    }

    @Test
    void fetchOneReturnsSingleRow(WireMockRuntimeInfo wm) throws Exception {
        stubGetJson("/v2/monitoring/alerts", LIST_ALERTS_JSON);

        var task = List.builder()
            .id(IdUtils.create())
            .type(List.class.getName())
            .apiToken(Property.ofValue("secret-token"))
            .baseUrl(Property.ofValue(wm.getHttpBaseUrl()))
            .fetchType(Property.ofValue(FetchType.FETCH_ONE))
            .build();

        var output = task.run(runContext());

        assertThat(output.getRow(), notNullValue());
        assertThat(output.getRow().get("uuid"), is("7343e061-e0c1-4b71-a476-c56dfaa3d2ad"));
        assertThat(output.getRows(), nullValue());

        @SuppressWarnings("unchecked")
        var alerts = (java.util.Map<String, Object>) output.getRow().get("alerts");
        assertThat(alerts, notNullValue());
        @SuppressWarnings("unchecked")
        var slack = (java.util.List<java.util.Map<String, Object>>) alerts.get("slack");
        assertThat(slack, notNullValue());
        assertThat(slack.getFirst(), hasEntry("channel", "#alerts"));
        assertThat(slack.getFirst().containsKey("url"), is(false));
    }
}
