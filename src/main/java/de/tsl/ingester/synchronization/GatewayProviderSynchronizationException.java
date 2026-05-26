package de.tsl.ingester.synchronization;

/**
 * Signals a business or orchestration failure during the synchronization process.
 */
public class GatewayProviderSynchronizationException extends RuntimeException {

    /**
     * Creates a synchronization exception without an underlying cause.
     *
     * @param message human-readable error message
     */
    public GatewayProviderSynchronizationException(String message) {
        super(message);
    }

    /**
     * Creates a synchronization exception with an underlying cause.
     *
     * @param message human-readable error message
     * @param cause underlying cause of the failure
     */
    public GatewayProviderSynchronizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
