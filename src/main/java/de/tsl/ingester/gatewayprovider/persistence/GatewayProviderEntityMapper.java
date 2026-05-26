package de.tsl.ingester.gatewayprovider.persistence;

import de.tsl.ingester.gatewayprovider.GatewayProvider;
import java.util.Arrays;
import java.util.List;

/**
 * Maps between the central domain model and the persistence entity used for database access.
 */
public class GatewayProviderEntityMapper {

    /**
     * Creates a new stateless entity mapper.
     */
    public GatewayProviderEntityMapper() {
    }

    /**
     * Maps a persisted entity into the domain model used by synchronization logic.
     *
     * @param entity the persisted database row representation
     * @return the corresponding domain model
     */
    public GatewayProvider toDomain(GatewayProviderEntity entity) {
        return new GatewayProvider(
            entity.getName(),
            entity.getServiceName(),
            entity.getConfigEndpoint(),
            entity.getActive(),
            entity.getCertificate(),
            entity.getIssuer(),
            entity.getAuthorizationEndpoint(),
            entity.getTokenEndpoint(),
            entity.getJwksUri(),
            parseResponseTypes(entity.getResponseTypesSupported()),
            entity.getProviderVersion()
        );
    }

    /**
     * Maps a domain provider into the persistence entity used by the batch writer.
     *
     * @param provider the domain model to persist
     * @return the mapped entity representation
     */
    public GatewayProviderEntity toEntity(GatewayProvider provider) {
        return GatewayProviderEntity.rehydrate(
            null,
            provider.name(),
            provider.serviceName(),
            provider.configEndpoint(),
            provider.active(),
            provider.certificate(),
            provider.issuer(),
            provider.authorizationEndpoint(),
            provider.tokenEndpoint(),
            provider.jwksUri(),
            provider.responseTypesSupported().isEmpty() ? null : String.join(",", provider.responseTypesSupported()),
            provider.version()
        );
    }

    private List<String> parseResponseTypes(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .toList();
    }
}
