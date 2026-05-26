package de.tsl.ingester.gatewayprovider;

import de.tsl.ingester.providermetadata.ProviderMetadata;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Central domain model for a TI gateway provider during synchronization.
 *
 * <p>The record is intentionally independent from XML DTOs and persistence entities so parsing,
 * reconciliation, metadata enrichment, and persistence can share one normalized representation.
 *
 * <p>The record also overrides the default {@code equals}, {@code hashCode}, and {@code toString}
 * implementations because the {@code certificate} component is a byte array. Record defaults are
 * value-based only as far as the component type supports value-based equality itself. For arrays,
 * Java's default behavior is reference-based, which means two providers with identical certificate
 * bytes but different array instances would compare as different, produce inconsistent hash codes,
 * and render the certificate as an object identity string instead of readable contents. This type
 * needs content-based semantics for the certificate field, so the overrides delegate to
 * {@link Arrays#equals(byte[], byte[])}, {@link Arrays#hashCode(byte[])}, and
 * {@link Arrays#toString(byte[])}.
 *
 * @param name display name of the provider
 * @param serviceName business identifier derived from the trusted list
 * @param configEndpoint base endpoint used to derive the OpenID well-known URL
 * @param active whether the provider is currently marked in accord in the trusted list
 * @param certificate trusted-list certificate bytes used as TLS trust anchor
 * @param issuer OpenID issuer value from the provider metadata
 * @param authorizationEndpoint OpenID authorization endpoint
 * @param tokenEndpoint OpenID token endpoint
 * @param jwksUri OpenID JWKS URI
 * @param responseTypesSupported OpenID response types supported by the provider
 * @param version synchronization version assigned to this provider snapshot
 */
public record GatewayProvider(
    String name,
    String serviceName,
    String configEndpoint,
    Boolean active,
    byte[] certificate,
    String issuer,
    String authorizationEndpoint,
    String tokenEndpoint,
    String jwksUri,
    List<String> responseTypesSupported,
    Long version
) {

    /**
     * Creates a normalized gateway provider record.
     */
    public GatewayProvider {
        name = normalize(name);
        serviceName = normalize(serviceName);
        configEndpoint = normalize(configEndpoint);
        issuer = normalize(issuer);
        authorizationEndpoint = normalize(authorizationEndpoint);
        tokenEndpoint = normalize(tokenEndpoint);
        jwksUri = normalize(jwksUri);
        certificate = copy(certificate);
        responseTypesSupported = responseTypesSupported == null ? List.of() : List.copyOf(responseTypesSupported);
    }

    /**
     * Returns a defensive copy of the provider certificate.
     *
     * @return the certificate bytes or {@code null}
     */
    @Override
    public byte[] certificate() {
        return copy(certificate);
    }

    /**
     * Compares this provider to another provider including the certificate contents.
     *
     * @param other object to compare with
     * @return {@code true} when all fields, including certificate bytes, are equal
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GatewayProvider that)) {
            return false;
        }
        return Objects.equals(name, that.name)
            && Objects.equals(serviceName, that.serviceName)
            && Objects.equals(configEndpoint, that.configEndpoint)
            && Objects.equals(active, that.active)
            && Arrays.equals(certificate, that.certificate)
            && Objects.equals(issuer, that.issuer)
            && Objects.equals(authorizationEndpoint, that.authorizationEndpoint)
            && Objects.equals(tokenEndpoint, that.tokenEndpoint)
            && Objects.equals(jwksUri, that.jwksUri)
            && Objects.equals(responseTypesSupported, that.responseTypesSupported)
            && Objects.equals(version, that.version);
    }

    /**
     * Returns a hash code that includes the certificate contents.
     *
     * @return the hash code for this provider
     */
    @Override
    public int hashCode() {
        int result = Objects.hash(
            name,
            serviceName,
            configEndpoint,
            active,
            issuer,
            authorizationEndpoint,
            tokenEndpoint,
            jwksUri,
            responseTypesSupported,
            version
        );
        return 31 * result + Arrays.hashCode(certificate);
    }

    /**
     * Returns a string representation that includes the certificate contents.
     *
     * @return the string representation of this provider
     */
    @Override
    public String toString() {
        return "GatewayProvider[" +
            "name=" + name +
            ", serviceName=" + serviceName +
            ", configEndpoint=" + configEndpoint +
            ", active=" + active +
            ", certificate=" + Arrays.toString(certificate) +
            ", issuer=" + issuer +
            ", authorizationEndpoint=" + authorizationEndpoint +
            ", tokenEndpoint=" + tokenEndpoint +
            ", jwksUri=" + jwksUri +
            ", responseTypesSupported=" + responseTypesSupported +
            ", version=" + version +
            ']';
    }

    /**
     * Returns a copy of this provider with the synchronization version assigned.
     *
     * @param value the TSL or synchronization version for the current run
     * @return a new provider instance carrying the supplied version
     */
    public GatewayProvider withVersion(long value) {
        return new GatewayProvider(
            name,
            serviceName,
            configEndpoint,
            active,
            certificate,
            issuer,
            authorizationEndpoint,
            tokenEndpoint,
            jwksUri,
            responseTypesSupported,
            value
        );
    }

    /**
     * Returns a copy of this provider enriched with OpenID provider metadata.
     *
     * @param metadata metadata fetched from the provider's well-known endpoint
     * @return a new provider instance with metadata fields populated
     */
    public GatewayProvider withProviderMetadata(ProviderMetadata metadata) {
        return new GatewayProvider(
            name,
            serviceName,
            configEndpoint,
            active,
            certificate,
            metadata.issuer(),
            metadata.authorizationEndpoint(),
            metadata.tokenEndpoint(),
            metadata.jwksUri(),
            metadata.responseTypesSupported(),
            version
        );
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static byte[] copy(byte[] value) {
        return value == null ? null : Arrays.copyOf(value, value.length);
    }
}
