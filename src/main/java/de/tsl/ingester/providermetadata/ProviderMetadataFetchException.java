package de.tsl.ingester.providermetadata;

import java.util.Objects;

/**
 * Exception raised when a provider metadata fetch fails with a classified error status.
 */
public final class ProviderMetadataFetchException extends RuntimeException {

    /** Classified failure category for the rejected metadata fetch. */
    private final ProviderMetadataFetchFailure status;

    /**
     * Creates a fetch exception without an underlying cause.
     *
     * @param status classified failure category
     * @param message human-readable error message
     */
    public ProviderMetadataFetchException(ProviderMetadataFetchFailure status, String message) {
        super(message);
        this.status = Objects.requireNonNull(status, "Status must not be null");
    }

    /**
     * Creates a fetch exception with an underlying cause.
     *
     * @param status classified failure category
     * @param message human-readable error message
     * @param cause underlying cause of the failure
     */
    public ProviderMetadataFetchException(ProviderMetadataFetchFailure status, String message, Throwable cause) {
        super(message, cause);
        this.status = Objects.requireNonNull(status, "Status must not be null");
    }

    /**
     * Returns the classified failure status for the fetch attempt.
     *
     * @return the failure category
     */
    public ProviderMetadataFetchFailure status() {
        return status;
    }

    /**
     * JavaBean-style accessor for frameworks or callers that prefer getter naming.
     *
     * @return the failure category
     */
    public ProviderMetadataFetchFailure getStatus() {
        return status;
    }
}
