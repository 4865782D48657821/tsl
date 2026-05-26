package de.tsl.ingester.trustedlist;

/**
 * Signals invalid trusted-list input or failures while obtaining or decoding trusted-list data.
 */
public class TrustedListFormatException extends RuntimeException {

    /**
     * Creates a trusted-list exception without an underlying cause.
     *
     * @param message human-readable error message
     */
    public TrustedListFormatException(String message) {
        super(message);
    }

    /**
     * Creates a trusted-list exception with an underlying cause.
     *
     * @param message human-readable error message
     * @param cause underlying cause of the failure
     */
    public TrustedListFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
