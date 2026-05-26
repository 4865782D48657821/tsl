package de.tsl.ingester.synchronization;

import de.tsl.ingester.gatewayprovider.GatewayProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes inserts, updates, and deletions by comparing current trusted-list providers with the
 * currently persisted directory state.
 */
public class GatewayProviderDirectoryReconciler {

    /**
     * Creates a new stateless directory reconciler.
     */
    public GatewayProviderDirectoryReconciler() {
    }

    /**
     * Reconciles the current trusted-list view against the persisted provider directory.
     *
     * @param currentTrustedListProviders current providers extracted from the trusted list
     * @param persistedProviders providers currently stored in the database
     * @param version synchronization version to assign to new and updated entries
     * @return the directory changes to persist
     */
    public GatewayProviderDirectoryChanges reconcile(
        List<GatewayProvider> currentTrustedListProviders,
        List<GatewayProvider> persistedProviders,
        long version
    ) {
        Map<String, GatewayProvider> currentByServiceName = new LinkedHashMap<>();
        for (GatewayProvider provider : currentTrustedListProviders) {
            String identity = identityOf(provider);
            GatewayProvider previous = currentByServiceName.putIfAbsent(identity, provider.withVersion(version));
            if (previous != null) {
                throw new IllegalArgumentException("duplicate serviceName in XML entries: " + identity);
            }
        }

        Map<String, GatewayProvider> persistedByServiceName = new LinkedHashMap<>();
        for (GatewayProvider provider : persistedProviders) {
            persistedByServiceName.put(identityOf(provider), provider);
        }

        List<GatewayProvider> inserts = new ArrayList<>();
        List<GatewayProvider> updates = new ArrayList<>();
        for (Map.Entry<String, GatewayProvider> entry : currentByServiceName.entrySet()) {
            GatewayProvider existing = persistedByServiceName.remove(entry.getKey());
            if (existing == null) {
                inserts.add(entry.getValue());
            } else {
                updates.add(entry.getValue());
            }
        }

        List<GatewayProvider> deletions = List.copyOf(persistedByServiceName.values());
        return new GatewayProviderDirectoryChanges(inserts, updates, deletions);
    }

    private String identityOf(GatewayProvider provider) {
        if (provider == null || provider.serviceName() == null) {
            throw new IllegalArgumentException("serviceName must not be blank");
        }
        return provider.serviceName();
    }
}
