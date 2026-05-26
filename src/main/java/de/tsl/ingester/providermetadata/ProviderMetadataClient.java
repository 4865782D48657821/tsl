package de.tsl.ingester.providermetadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tsl.ingester.gatewayprovider.GatewayProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLHandshakeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches and validates OpenID provider metadata using the certificate supplied by the trusted
 * list entry as the TLS trust anchor.
 */
public class ProviderMetadataClient {

    private static final Logger log = LoggerFactory.getLogger(ProviderMetadataClient.class);
    private static final String WELL_KNOWN_PATH = "/.well-known/openid-configuration";

    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final ObjectMapper objectMapper;

    /**
     * Creates a client for fetching well-known OpenID provider metadata.
     *
     * @param connectTimeout connect timeout for outbound HTTP requests
     * @param readTimeout read timeout for outbound HTTP requests
     * @param objectMapper JSON mapper used to parse metadata responses
     */
    public ProviderMetadataClient(Duration connectTimeout, Duration readTimeout, ObjectMapper objectMapper) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Fetches the provider's well-known OpenID configuration and maps it into the internal metadata
     * representation.
     *
     * @param provider the gateway provider to enrich
     * @return normalized provider metadata
     * @throws ProviderMetadataFetchException when input validation, TLS validation, HTTP transport,
     *     or JSON parsing fails
     */
    public ProviderMetadata fetchProviderMetadata(GatewayProvider provider) {
        URI wellKnownUri = toWellKnownUri(provider);
        byte[] certificate = requiredCertificate(provider);

        try {
            log.info("Fetching provider metadata for serviceName={} from {}", provider.serviceName(), wellKnownUri);
            HttpResponse<String> response = sendRequest(wellKnownUri, certificate);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.info(
                    "Provider metadata fetch failed for serviceName={} from {} with HTTP status {}",
                    provider.serviceName(),
                    wellKnownUri,
                    response.statusCode()
                );
                throw new ProviderMetadataFetchException(
                    ProviderMetadataFetchFailure.HTTP_ERROR,
                    "Unexpected HTTP status " + response.statusCode()
                );
            }
            log.info(
                "Provider metadata fetch succeeded for serviceName={} from {} with HTTP status {}",
                provider.serviceName(),
                response.uri(),
                response.statusCode()
            );
            try {
                return parseProviderMetadata(response.body());
            } catch (IOException exception) {
                log.info(
                    "Provider metadata JSON parse failed for serviceName={} from {}: {}",
                    provider.serviceName(),
                    wellKnownUri,
                    exception.getMessage()
                );
                throw new ProviderMetadataFetchException(ProviderMetadataFetchFailure.INVALID_JSON, exception.getMessage(), exception);
            }
        } catch (java.net.http.HttpTimeoutException exception) {
            log.info("Provider metadata fetch timed out for serviceName={} from {}: {}", provider.serviceName(), wellKnownUri, exception.getMessage());
            throw new ProviderMetadataFetchException(ProviderMetadataFetchFailure.TIMEOUT, exception.getMessage(), exception);
        } catch (SSLHandshakeException exception) {
            log.info("TLS validation failed for serviceName={} from {}: {}", provider.serviceName(), wellKnownUri, exception.getMessage());
            throw new ProviderMetadataFetchException(ProviderMetadataFetchFailure.TLS_VALIDATION_FAILED, exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.info("Provider metadata fetch interrupted for serviceName={} from {}: {}", provider.serviceName(), wellKnownUri, exception.getMessage());
            throw new ProviderMetadataFetchException(ProviderMetadataFetchFailure.HTTP_ERROR, exception.getMessage(), exception);
        } catch (IOException exception) {
            log.info("Provider metadata fetch IO failure for serviceName={} from {}: {}", provider.serviceName(), wellKnownUri, exception.getMessage());
            throw new ProviderMetadataFetchException(ProviderMetadataFetchFailure.HTTP_ERROR, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            log.info("Provider metadata payload invalid for serviceName={} from {}: {}", provider.serviceName(), wellKnownUri, exception.getMessage());
            throw new ProviderMetadataFetchException(ProviderMetadataFetchFailure.INVALID_CONFIGURATION, exception.getMessage(), exception);
        }
    }

    private HttpResponse<String> sendRequest(URI wellKnownUri, byte[] certificate) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(connectTimeout)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .sslContext(createSslContext(certificate))
            .build();
        HttpRequest request = HttpRequest.newBuilder(wellKnownUri)
            .GET()
            .timeout(readTimeout)
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI toWellKnownUri(GatewayProvider provider) {
        String configEndpoint = provider.configEndpoint();
        if (configEndpoint == null) {
            log.info("Skipping provider metadata fetch for {} because the config endpoint is missing", provider.serviceName());
            throw new ProviderMetadataFetchException(ProviderMetadataFetchFailure.MISSING_CONFIG_ENDPOINT, "Missing config endpoint");
        }

        String trimmedEndpoint = configEndpoint.trim();
        if (trimmedEndpoint.isEmpty()) {
            log.info("Skipping provider metadata fetch for {} because the config endpoint is missing", provider.serviceName());
            throw new ProviderMetadataFetchException(ProviderMetadataFetchFailure.MISSING_CONFIG_ENDPOINT, "Missing config endpoint");
        }

        try {
            URI endpoint = URI.create(trimmedEndpoint);
            if (!"https".equalsIgnoreCase(endpoint.getScheme())) {
                throw new ProviderMetadataFetchException(
                    ProviderMetadataFetchFailure.INVALID_CONFIGURATION,
                    "Config endpoint must use HTTPS"
                );
            }
            String normalizedEndpoint = endpoint.toString().endsWith("/")
                ? endpoint.toString().substring(0, endpoint.toString().length() - 1)
                : endpoint.toString();
            return URI.create(normalizedEndpoint + WELL_KNOWN_PATH);
        } catch (IllegalArgumentException exception) {
            throw new ProviderMetadataFetchException(
                ProviderMetadataFetchFailure.INVALID_CONFIGURATION,
                "Invalid config endpoint",
                exception
            );
        }
    }

    private byte[] requiredCertificate(GatewayProvider provider) {
        byte[] certificate = provider.certificate();
        if (certificate == null || certificate.length == 0) {
            log.info("Skipping provider metadata fetch for {} because no trust certificate is available", provider.serviceName());
            throw new ProviderMetadataFetchException(ProviderMetadataFetchFailure.MISSING_CERTIFICATE, "Missing certificate");
        }
        return certificate;
    }

    private SSLContext createSslContext(byte[] certificateBytes) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certificateFactory
                .generateCertificate(new ByteArrayInputStream(certificateBytes));
            keyStore.setCertificateEntry("tsl-0", certificate);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            return sslContext;
        } catch (Exception exception) {
            throw new ProviderMetadataFetchException(
                ProviderMetadataFetchFailure.INVALID_CONFIGURATION,
                "Invalid certificate",
                exception
            );
        }
    }

    private ProviderMetadata parseProviderMetadata(String body) throws IOException {
        JsonNode node = objectMapper.readTree(body);
        String issuer = requiredText(node, "issuer");
        String authorizationEndpoint = requiredText(node, "authorization_endpoint");
        String tokenEndpoint = requiredText(node, "token_endpoint");
        String jwksUri = requiredText(node, "jwks_uri");
        JsonNode responseTypesNode = node.get("response_types_supported");
        if (responseTypesNode == null || !responseTypesNode.isArray()) {
            throw new IllegalStateException("response_types_supported must be an array");
        }
        return new ProviderMetadata(
            issuer,
            authorizationEndpoint,
            tokenEndpoint,
            jwksUri,
            responseTypes(responseTypesNode)
        );
    }

    private List<String> responseTypes(JsonNode responseTypesNode) {
        return java.util.stream.StreamSupport.stream(responseTypesNode.spliterator(), false)
            .map(JsonNode::asText)
            .toList();
    }

    private String requiredText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.asText().isBlank()) {
            throw new IllegalStateException(fieldName + " must be present");
        }
        return value.asText();
    }
}
