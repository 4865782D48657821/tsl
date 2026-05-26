package de.tsl.ingester.synchronization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tsl.ingester.gatewayprovider.GatewayProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class GatewayProviderDirectoryReconcilerTest {

    private final GatewayProviderDirectoryReconciler gatewayProviderDirectoryReconciler =
        new GatewayProviderDirectoryReconciler();

    @Test
    void createsInsertUpdateAndDeleteActionsByServiceName() {
        GatewayProvider trustedListExisting = gatewayProvider("svc-existing", "Existing Provider", true, null);
        GatewayProvider trustedListNew = gatewayProvider("svc-new", "New Provider", false, null);

        GatewayProvider persistedExisting = gatewayProvider("svc-existing", "Old Existing Provider", true, 4L);
        GatewayProvider persistedRemoved = gatewayProvider("svc-removed", "Old Existing Provider", true, 4L);

        GatewayProviderDirectoryChanges changes = gatewayProviderDirectoryReconciler.reconcile(
            List.of(trustedListExisting, trustedListNew),
            List.of(persistedExisting, persistedRemoved),
            12L
        );

        assertThat(changes.inserts())
            .singleElement()
            .extracting(GatewayProvider::serviceName)
            .isEqualTo("svc-new");
        assertThat(changes.updates())
            .singleElement()
            .extracting(GatewayProvider::serviceName)
            .isEqualTo("svc-existing");
        assertThat(changes.deletions())
            .singleElement()
            .extracting(GatewayProvider::serviceName)
            .isEqualTo("svc-removed");
        assertThat(changes.deletions()).singleElement().extracting(GatewayProvider::active).isEqualTo(true);
        assertThat(changes.inserts()).allSatisfy(provider -> assertThat(provider.version()).isEqualTo(12L));
        assertThat(changes.updates()).allSatisfy(provider -> assertThat(provider.version()).isEqualTo(12L));
    }

    @Test
    void rejectsDuplicateServiceNamesWithinTrustedListProviders() {
        assertThatThrownBy(() -> gatewayProviderDirectoryReconciler.reconcile(
            List.of(
                gatewayProvider("svc-1", "One", true, null),
                gatewayProvider("svc-1", "Duplicate", false, null)
            ),
            List.of(),
            5L
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("duplicate serviceName");
    }

    private GatewayProvider gatewayProvider(String serviceName, String name, boolean active, Long version) {
        return new GatewayProvider(
            name,
            serviceName,
            "https://" + serviceName + ".example",
            active,
            new byte[] { 0x01, 0x02, 0x03 },
            null,
            null,
            null,
            null,
            List.of(),
            version
        );
    }
}
