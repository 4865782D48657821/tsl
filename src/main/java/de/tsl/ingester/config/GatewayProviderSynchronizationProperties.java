package de.tsl.ingester.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * External configuration for the scheduled synchronization process.
 *
 * @param trustedListUrl source URL for the trusted-list XML
 * @param cron cron expression used by the scheduler
 * @param zone timezone identifier used for the scheduler
 * @param connectTimeout connect timeout for outbound HTTP requests
 * @param readTimeout read timeout for outbound HTTP requests
 * @param metadataFetchConcurrency maximum number of parallel provider metadata fetches
 */
@Validated
@ConfigurationProperties(prefix = "gateway-provider-synchronization")
public record GatewayProviderSynchronizationProperties(
    @NotBlank String trustedListUrl,
    @NotBlank String cron,
    @NotBlank String zone,
    @NotNull Duration connectTimeout,
    @NotNull Duration readTimeout,
    @Min(1) int metadataFetchConcurrency
) {
}
