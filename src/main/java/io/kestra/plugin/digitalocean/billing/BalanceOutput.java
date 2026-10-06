package io.kestra.plugin.digitalocean.billing;

import io.kestra.core.models.tasks.Output;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static io.kestra.plugin.digitalocean.AbstractDigitalOceanTask.asString;

/**
 * DigitalOcean returns the amounts as strings such as {@code "23.44"}; they are parsed into {@link BigDecimal} so they
 * keep their exact value and can be compared in expressions.
 */
@Builder
@Getter
public class BalanceOutput implements Output {

    @Schema(title = "Month-to-date balance", description = "Balance as of `generatedAt`: the account balance plus the month-to-date usage, in USD.")
    private final BigDecimal monthToDateBalance;

    @Schema(title = "Account balance", description = "Current balance as of the last invoice, in USD. Negative when the account has credit.")
    private final BigDecimal accountBalance;

    @Schema(title = "Month-to-date usage", description = "Amount used in the current billing period as of `generatedAt`, in USD.")
    private final BigDecimal monthToDateUsage;

    @Schema(title = "Generation timestamp", description = "When DigitalOcean computed these figures.")
    private final Instant generatedAt;

    public static BalanceOutput from(Map<String, Object> balance) {
        var generatedAt = asString(balance.get("generated_at"));

        return BalanceOutput.builder()
            .monthToDateBalance(asAmount(balance.get("month_to_date_balance")))
            .accountBalance(asAmount(balance.get("account_balance")))
            .monthToDateUsage(asAmount(balance.get("month_to_date_usage")))
            .generatedAt(generatedAt != null ? Instant.parse(generatedAt) : null)
            .build();
    }

    private static BigDecimal asAmount(Object value) {
        var text = asString(value);
        return text == null || text.isBlank() ? null : new BigDecimal(text);
    }
}
