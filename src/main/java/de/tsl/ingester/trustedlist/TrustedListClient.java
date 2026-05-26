package de.tsl.ingester.trustedlist;

/**
 * Adapter for obtaining the raw trusted list XML document from an external source.
 */
public interface TrustedListClient {

    /**
     * Fetches the trusted list document as XML text.
     *
     * @return the trusted list XML payload
     * @throws TrustedListFormatException when the document cannot be fetched successfully
     */
    String fetchTrustedListXml();
}
