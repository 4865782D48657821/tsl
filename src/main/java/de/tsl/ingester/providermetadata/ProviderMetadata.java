package de.tsl.ingester.providermetadata;

import java.util.List;

/**
 * OpenID provider metadata loaded from a gateway provider's well-known configuration endpoint.
 *
 * @param issuer issuer identifier of the OpenID provider
 * @param authorizationEndpoint authorization endpoint advertised by the provider
 * @param tokenEndpoint token endpoint advertised by the provider
 * @param jwksUri JSON Web Key Set endpoint advertised by the provider
 * @param responseTypesSupported response types advertised by the provider
 */
public record ProviderMetadata(
    String issuer,
    String authorizationEndpoint,
    String tokenEndpoint,
    String jwksUri,
    List<String> responseTypesSupported
) {

    /**
     * Creates normalized provider metadata with an immutable response type list.
     */
    public ProviderMetadata {
        responseTypesSupported = responseTypesSupported == null ? List.of() : List.copyOf(responseTypesSupported);
    }
}
