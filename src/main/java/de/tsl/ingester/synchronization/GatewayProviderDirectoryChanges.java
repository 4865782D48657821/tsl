package de.tsl.ingester.synchronization;

import de.tsl.ingester.gatewayprovider.GatewayProvider;
import java.util.ArrayList;
import java.util.List;

record GatewayProviderDirectoryChanges(
    List<GatewayProvider> inserts,
    List<GatewayProvider> updates,
    List<GatewayProvider> deletions
) {

    GatewayProviderDirectoryChanges {
        inserts = List.copyOf(inserts);
        updates = List.copyOf(updates);
        deletions = List.copyOf(deletions);
    }

    List<GatewayProvider> currentTrustedListProviders() {
        List<GatewayProvider> providers = new ArrayList<>(inserts.size() + updates.size());
        providers.addAll(inserts);
        providers.addAll(updates);
        return List.copyOf(providers);
    }
}
