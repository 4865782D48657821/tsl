package de.tsl.ingester.providermetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tsl.ingester.gatewayprovider.GatewayProvider;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProviderMetadataClientTest {

    private final ProviderMetadataClient client = new ProviderMetadataClient(
        Duration.ofSeconds(2),
        Duration.ofSeconds(2),
        new ObjectMapper()
    );

    @Test
    void rejectsMissingConfigEndpoint() {
        assertFailure(
            gatewayProvider(null, new byte[] { 0x01 }),
            ProviderMetadataFetchFailure.MISSING_CONFIG_ENDPOINT,
            "Missing config endpoint"
        );
    }

    @Test
    void rejectsBlankConfigEndpoint() {
        assertFailure(
            gatewayProvider("   ", new byte[] { 0x01 }),
            ProviderMetadataFetchFailure.MISSING_CONFIG_ENDPOINT,
            "Missing config endpoint"
        );
    }

    @Test
    void rejectsMissingCertificate() {
        assertFailure(
            gatewayProvider("https://localhost", null),
            ProviderMetadataFetchFailure.MISSING_CERTIFICATE,
            "Missing certificate"
        );
    }

    @Test
    void rejectsNonHttpsConfigEndpoint() {
        assertFailure(
            gatewayProvider("http://localhost", new byte[] { 0x01 }),
            ProviderMetadataFetchFailure.INVALID_CONFIGURATION,
            "Config endpoint must use HTTPS"
        );
    }

    @Test
    void rejectsMalformedConfigEndpoint() {
        assertFailure(
            gatewayProvider("https:// local host", new byte[] { 0x01 }),
            ProviderMetadataFetchFailure.INVALID_CONFIGURATION,
            "Invalid config endpoint"
        );
    }

    @Test
    void rejectsInvalidCertificateBytesBeforeSendingRequest() {
        assertFailure(
            gatewayProvider("https://localhost", new byte[] { 0x01, 0x02, 0x03 }),
            ProviderMetadataFetchFailure.INVALID_CONFIGURATION,
            "Invalid certificate"
        );
    }

    private void assertFailure(
        GatewayProvider provider,
        ProviderMetadataFetchFailure expectedStatus,
        String expectedMessage
    ) {
        assertThatThrownBy(() -> client.fetchProviderMetadata(provider))
            .isInstanceOfSatisfying(ProviderMetadataFetchException.class, exception -> {
                assertThat(exception.status()).isEqualTo(expectedStatus);
                assertThat(exception).hasMessageContaining(expectedMessage);
            });
    }

    private GatewayProvider gatewayProvider(String configEndpoint, byte[] certificate) {
        return new GatewayProvider(
            "Provider",
            "svc-1",
            configEndpoint,
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
}
