package de.tsl.ingester.gatewayprovider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class GatewayProviderTest {

    @Test
    void equalsAndHashCodeUseCertificateContentInsteadOfArrayIdentity() {
        GatewayProvider first = gatewayProvider(new byte[] { 0x01, 0x02, 0x03 });
        GatewayProvider second = gatewayProvider(new byte[] { 0x01, 0x02, 0x03 });

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void toStringShowsCertificateContent() {
        GatewayProvider provider = gatewayProvider(new byte[] { 0x01, 0x02, 0x03 });

        assertThat(provider.toString())
            .contains("certificate=[1, 2, 3]")
            .contains("serviceName=svc-1");
    }

    @Test
    void certificateAccessorReturnsDefensiveCopy() {
        GatewayProvider provider = gatewayProvider(new byte[] { 0x01, 0x02, 0x03 });

        byte[] copy = provider.certificate();
        copy[0] = 0x09;

        assertThat(provider.certificate()).containsExactly(0x01, 0x02, 0x03);
    }

    private GatewayProvider gatewayProvider(byte[] certificate) {
        return new GatewayProvider(
            "Provider",
            "svc-1",
            "https://provider.example",
            true,
            certificate,
            "issuer",
            "https://provider.example/auth",
            "https://provider.example/token",
            "https://provider.example/jwks",
            List.of("code"),
            1L
        );
    }
}
