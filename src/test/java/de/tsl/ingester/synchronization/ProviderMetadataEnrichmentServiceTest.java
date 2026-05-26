package de.tsl.ingester.synchronization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tsl.ingester.gatewayprovider.GatewayProvider;
import de.tsl.ingester.providermetadata.ProviderMetadata;
import de.tsl.ingester.providermetadata.ProviderMetadataClient;
import de.tsl.ingester.providermetadata.ProviderMetadataFetchException;
import de.tsl.ingester.providermetadata.ProviderMetadataFetchFailure;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ProviderMetadataEnrichmentServiceTest {

    @Test
    void returnsEmptyListWhenThereAreNoProviders() {
        RecordingProviderMetadataClient client = new RecordingProviderMetadataClient();
        ProviderMetadataEnrichmentService service = new ProviderMetadataEnrichmentService(client, 2);

        List<GatewayProvider> enriched = service.enrichProviders(List.of());

        assertThat(enriched).isEmpty();
        assertThat(client.fetchCount()).isZero();
    }

    @Test
    void reusesMetadataFetchForProvidersWithTheSameEndpointAndCertificate() {
        RecordingProviderMetadataClient client = new RecordingProviderMetadataClient();
        ProviderMetadataEnrichmentService service = new ProviderMetadataEnrichmentService(client, 2);
        GatewayProvider first = gatewayProvider("svc-1", "https://shared.example", new byte[] { 0x01, 0x02 });
        GatewayProvider second = gatewayProvider("svc-2", "https://shared.example/", new byte[] { 0x01, 0x02 });

        List<GatewayProvider> enriched = service.enrichProviders(List.of(first, second));

        assertThat(client.fetchCount()).isEqualTo(1);
        assertThat(enriched).extracting(GatewayProvider::issuer).containsExactly("issuer-svc-1", "issuer-svc-1");
        assertThat(enriched).extracting(GatewayProvider::serviceName).containsExactly("svc-1", "svc-2");
    }

    @Test
    void failsWhenAnyMetadataFetchFails() {
        ProviderMetadataEnrichmentService service = new ProviderMetadataEnrichmentService(
            new FailingProviderMetadataClient(),
            2
        );
        GatewayProvider provider = gatewayProvider("svc-failing", "https://failing.example", new byte[] { 0x01 });

        assertThatThrownBy(() -> service.enrichProviders(List.of(provider)))
            .isInstanceOf(GatewayProviderSynchronizationException.class)
            .hasMessageContaining("HTTP_ERROR")
            .hasMessageContaining("svc-failing")
            .hasMessageContaining("boom");
    }

    private GatewayProvider gatewayProvider(String serviceName, String endpoint, byte[] certificate) {
        return new GatewayProvider(
            "Provider " + serviceName,
            serviceName,
            endpoint,
            true,
            certificate,
            null,
            null,
            null,
            null,
            List.of(),
            1L
        );
    }

    private static final class RecordingProviderMetadataClient extends ProviderMetadataClient {

        private final AtomicInteger fetchCount = new AtomicInteger();

        private RecordingProviderMetadataClient() {
            super(Duration.ofSeconds(1), Duration.ofSeconds(1), new ObjectMapper());
        }

        @Override
        public ProviderMetadata fetchProviderMetadata(GatewayProvider provider) {
            fetchCount.incrementAndGet();
            return new ProviderMetadata(
                "issuer-" + provider.serviceName(),
                provider.configEndpoint() + "/auth",
                provider.configEndpoint() + "/token",
                provider.configEndpoint() + "/jwks",
                List.of("code")
            );
        }

        private int fetchCount() {
            return fetchCount.get();
        }
    }

    private static final class FailingProviderMetadataClient extends ProviderMetadataClient {

        private FailingProviderMetadataClient() {
            super(Duration.ofSeconds(1), Duration.ofSeconds(1), new ObjectMapper());
        }

        @Override
        public ProviderMetadata fetchProviderMetadata(GatewayProvider provider) {
            throw new ProviderMetadataFetchException(
                ProviderMetadataFetchFailure.HTTP_ERROR,
                "boom"
            );
        }
    }
}
