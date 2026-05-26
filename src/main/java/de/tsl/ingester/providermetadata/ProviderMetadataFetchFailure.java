package de.tsl.ingester.providermetadata;

/**
 * Categorizes the failure mode of a provider metadata fetch attempt.
 */
public enum ProviderMetadataFetchFailure {
    /** The remote endpoint did not respond within the configured timeout. */
    TIMEOUT,
    /** TLS validation against the trusted-list certificate failed. */
    TLS_VALIDATION_FAILED,
    /** The remote endpoint responded with a non-success HTTP status or transport error. */
    HTTP_ERROR,
    /** The response body could not be parsed as valid JSON. */
    INVALID_JSON,
    /** The endpoint or payload violated the expected configuration contract. */
    INVALID_CONFIGURATION,
    /** The provider did not expose a usable configuration endpoint. */
    MISSING_CONFIG_ENDPOINT,
    /** The provider did not expose a usable trust certificate. */
    MISSING_CERTIFICATE
}
