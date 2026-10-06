package io.kestra.plugin.digitalocean.billing;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.digitalocean.AbstractDigitalOceanTask;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URI;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Get the DigitalOcean account balance",
    description = "Reads the account balance, the month-to-date usage and the month-to-date balance of the DigitalOcean account."
)
@Plugin(
    examples = {
        @Example(
            title = "Log the month-to-date usage and alert above a budget",
            full = true,
            code = """
                id: digitalocean_billing_balance
                namespace: company.team

                tasks:
                  - id: get_balance
                    type: io.kestra.plugin.digitalocean.billing.GetBalance
                    apiToken: "{{ secret('DIGITALOCEAN_TOKEN') }}"
                  - id: over_budget
                    type: io.kestra.plugin.core.flow.If
                    condition: "{{ outputs.get_balance.monthToDateUsage > 100 }}"
                    then:
                      - id: log_alert
                        type: io.kestra.plugin.core.log.Log
                        level: WARN
                        message: "DigitalOcean usage is at ${{ outputs.get_balance.monthToDateUsage }} this month"
                """
        )
    }
)
public class GetBalance extends AbstractDigitalOceanTask implements RunnableTask<BalanceOutput> {

    @Override
    public BalanceOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rApiToken = renderApiToken(runContext);
        var rBaseUrl = renderBaseUrl(runContext);

        logger.info("Fetching DigitalOcean account balance");

        var url = join(rBaseUrl, "v2/customers/my/balance");
        var requestBuilder = HttpRequest.builder().uri(URI.create(url)).method("GET");
        var body = requestJson(runContext, options, rApiToken, requestBuilder);

        if (body == null || body.isEmpty()) {
            throw new IllegalStateException("Unexpected response from the DigitalOcean API: the balance response was empty.");
        }
        return BalanceOutput.from(body);
    }
}
