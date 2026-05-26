package de.tsl.ingester.synchronization;

import de.tsl.ingester.gatewayprovider.GatewayProvider;
import de.tsl.ingester.providermetadata.ProviderMetadata;
import de.tsl.ingester.providermetadata.ProviderMetadataClient;
import de.tsl.ingester.providermetadata.ProviderMetadataFetchException;
import de.tsl.ingester.providermetadata.ProviderMetadataFetchFailure;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enriches gateway providers with OpenID metadata fetched from their well-known configuration
 * endpoints using bounded parallelism and strict all-or-nothing failure handling.
 */
public class ProviderMetadataEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(ProviderMetadataEnrichmentService.class);

    private final ProviderMetadataClient client;
    private final int maxConcurrency;

    /**
     * Creates a metadata enrichment service with bounded parallelism.
     *
     * @param client provider metadata client used for the actual fetches
     * @param maxConcurrency maximum number of concurrent metadata fetches
     */
    public ProviderMetadataEnrichmentService(ProviderMetadataClient client, int maxConcurrency) {
        this.client = client;
        this.maxConcurrency = maxConcurrency;
    }

    /**
     * Fetches provider metadata for the supplied providers and merges the results back into the
     * returned provider list.
     *
     * @param providers providers that require metadata enrichment
     * @return enriched providers in the same logical order as the input
     * @throws GatewayProviderSynchronizationException when any required metadata fetch fails
     */
    public List<GatewayProvider> enrichProviders(List<GatewayProvider> providers) {
        if (providers.isEmpty()) {
            log.info("No provider metadata candidates present for this synchronization run");
            return List.of();
        }

        Map<String, FetchTarget> uniqueTargets = new LinkedHashMap<>();
        List<FetchTarget> orderedTargets = new java.util.ArrayList<>(providers.size());
        for (GatewayProvider provider : providers) {
            FetchTarget target = FetchTarget.from(provider);
            orderedTargets.add(target);
            uniqueTargets.putIfAbsent(target.key(), target);
        }

        log.info(
            "Starting provider metadata enrichment for {} providers with {} unique targets and maxConcurrency={}",
            providers.size(),
            uniqueTargets.size(),
            maxConcurrency
        );
        ExecutorService executorService = Executors.newFixedThreadPool(maxConcurrency);
        try {
            List<CompletableFuture<FetchOutcome>> futures = uniqueTargets.values().stream()
                .map(target -> CompletableFuture.supplyAsync(() -> fetch(target), executorService))
                .toList();

            Map<String, ProviderMetadata> metadataByKey = new LinkedHashMap<>();
            List<FetchOutcome> failures = new java.util.ArrayList<>();
            for (CompletableFuture<FetchOutcome> future : futures) {
                FetchOutcome outcome = future.join();
                if (outcome.failureMessage() == null) {
                    metadataByKey.put(outcome.key(), outcome.metadata());
                } else {
                    failures.add(outcome);
                }
            }
            if (!failures.isEmpty()) {
                throw new GatewayProviderSynchronizationException("One or more provider metadata fetches failed: " + describeFailures(failures));
            }

            log.info(
                "Finished provider metadata enrichment: totalProviders={}, uniqueTargets={}, successful={}",
                providers.size(),
                uniqueTargets.size(),
                metadataByKey.size()
            );
            return orderedTargets.stream()
                .map(target -> target.provider().withProviderMetadata(metadataByKey.get(target.key())))
                .toList();
        } finally {
            executorService.shutdownNow();
        }
    }

    private FetchOutcome fetch(FetchTarget target) {
        try {
            return FetchOutcome.success(target.key(), client.fetchProviderMetadata(target.provider()));
        } catch (ProviderMetadataFetchException exception) {
            return FetchOutcome.failure(target.key(), target.provider().serviceName(), exception.status(), exception.getMessage());
        }
    }

    private String describeFailures(List<FetchOutcome> failures) {
        return failures.stream()
            .map(failure -> failure.status() + " [" + failure.serviceName() + "] " + failure.failureMessage())
            .collect(Collectors.joining("; "));
    }

    private record FetchOutcome(
        String key,
        String serviceName,
        ProviderMetadataFetchFailure status,
        ProviderMetadata metadata,
        String failureMessage
    ) {

        private static FetchOutcome success(String key, ProviderMetadata metadata) {
            return new FetchOutcome(key, null, null, metadata, null);
        }

        private static FetchOutcome failure(String key, String serviceName, ProviderMetadataFetchFailure status, String failureMessage) {
            return new FetchOutcome(key, serviceName, status, null, failureMessage);
        }
    }

    private record FetchTarget(GatewayProvider provider, String key) {

        private static FetchTarget from(GatewayProvider provider) {
            return new FetchTarget(provider, normalizeEndpoint(provider.configEndpoint()) + "#" + fingerprint(provider.certificate()));
        }

        private static String normalizeEndpoint(String endpoint) {
            if (endpoint == null) {
                return "missing-endpoint";
            }
            String trimmed = endpoint.trim();
            if (trimmed.isEmpty()) {
                return "missing-endpoint";
            }
            String normalized = trimmed;
            if (normalized.endsWith("/")) {
                return normalized.substring(0, normalized.length() - 1);
            }
            return normalized;
        }

        private static String fingerprint(byte[] certificate) {
            if (certificate == null || certificate.length == 0) {
                return "missing-certificate";
            }
            try {
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(certificate));
            } catch (NoSuchAlgorithmException exception) {
                throw new IllegalStateException("SHA-256 not available", exception);
            }
        }
    }
}
