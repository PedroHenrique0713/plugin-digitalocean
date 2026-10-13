package io.kestra.plugin.digitalocean.monitoring.metrics;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.kestra.core.models.property.Property;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTest;
import io.kestra.plugin.digitalocean.monitoring.models.NetworkInterface;
import io.kestra.plugin.digitalocean.monitoring.models.TrafficDirection;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class DropletBandwidthTest extends AbstractDigitalOceanTest {

    @Test
    void fetchesDropletBandwidthMetrics(WireMockRuntimeInfo wm) throws Exception {
        stubGetJson("/v2/monitoring/metrics/droplet/bandwidth", """
            {
              "status": "success",
              "data": {
                "resultType": "matrix",
                "result": [
                  {
                    "metric": {
                      "host_id": "123456",
                      "interface": "public",
                      "direction": "outbound"
                    },
                    "values": [
                      [1728000000, "1024.5"]
                    ]
                  }
                ]
              }
            }
            """);

        var task = DropletBandwidth.builder()
            .id(IdUtils.create())
            .type(DropletBandwidth.class.getName())
            .apiToken(Property.ofValue("secret-token"))
            .baseUrl(Property.ofValue(wm.getHttpBaseUrl()))
            .hostId(Property.ofValue("123456"))
            .networkInterface(Property.ofValue(NetworkInterface.PUBLIC))
            .direction(Property.ofValue(TrafficDirection.OUTBOUND))
            .start(Property.ofValue(Instant.ofEpochSecond(1728000000L)))
            .end(Property.ofValue(Instant.ofEpochSecond(1728003600L)))
            .build();

        var output = task.run(runContext());

        assertThat(output.getStatus(), is("success"));
        assertThat(output.getData(), hasSize(1));
        assertThat(output.getData().getFirst().getMetric().get("direction"), is("outbound"));
        assertThat(output.getData().getFirst().getValues().getFirst().getValue(), is(1024.5));
        verifyBearer(getRequestedFor(urlEqualTo("/v2/monitoring/metrics/droplet/bandwidth?host_id=123456&start=1728000000&end=1728003600&interface=public&direction=outbound")), "secret-token");
    }
}
