package de.tsl.ingester.trustedlist;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP-based {@link TrustedListClient} implementation for downloading the trusted list XML.
 */
public class HttpTrustedListClient implements TrustedListClient {

    private static final Logger log = LoggerFactory.getLogger(HttpTrustedListClient.class);

    private final URI tslUri;
    private final Duration connectTimeout;
    private final Duration readTimeout;

    /**
     * Creates a trusted-list client for the configured endpoint and timeouts.
     *
     * @param trustedListUrl trusted-list URL to download
     * @param connectTimeout connect timeout for the HTTP client
     * @param readTimeout read timeout for the HTTP request
     */
    public HttpTrustedListClient(String trustedListUrl, Duration connectTimeout, Duration readTimeout) {
        this.tslUri = URI.create(trustedListUrl);
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    /**
     * Downloads the configured trusted list document.
     *
     * @return the response body when the remote endpoint returns a 2xx status
     * @throws TrustedListFormatException when the request fails, is interrupted, or returns a non-2xx status
     */
    @Override
    public String fetchTrustedListXml() {
        log.info("Downloading trusted list XML from {}", tslUri);
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(connectTimeout)
            .build();
        HttpRequest request = HttpRequest.newBuilder(tslUri)
            .GET()
            .timeout(readTimeout)
            .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Trusted list download failed with HTTP status {} from {}", response.statusCode(), tslUri);
                throw new TrustedListFormatException("Trusted list fetch failed with HTTP status " + response.statusCode());
            }
            log.info(
                "Downloaded trusted list XML from {} with HTTP status {} and {} characters",
                tslUri,
                response.statusCode(),
                response.body().length()
            );
            return response.body();
        } catch (IOException exception) {
            log.warn("Trusted list download failed from {}: {}", tslUri, exception.getMessage());
            throw new TrustedListFormatException("Unable to fetch trusted list XML", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Trusted list download interrupted for {}: {}", tslUri, exception.getMessage());
            throw new TrustedListFormatException("Trusted list fetch was interrupted", exception);
        }
    }
}
