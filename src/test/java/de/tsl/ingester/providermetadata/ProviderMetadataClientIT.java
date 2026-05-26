package de.tsl.ingester.providermetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import de.tsl.ingester.gatewayprovider.GatewayProvider;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;

class ProviderMetadataClientIT {

    private final ProviderMetadataClient client = new ProviderMetadataClient(
        Duration.ofSeconds(2),
        Duration.ofSeconds(2),
        new ObjectMapper()
    );

    @Test
    void fetchesAndParsesProviderMetadataUsingTheProvidedCertificate() throws Exception {
        TestHttpsServer server = newServer(jsonHandler(HttpURLConnection.HTTP_OK, validMetadataJson()));
        server.start();
        try {
            ProviderMetadata metadata = client.fetchProviderMetadata(gatewayProvider(server.baseUri(), server.certificate().getEncoded()));

            assertThat(metadata.issuer()).isEqualTo("issuer");
            assertThat(metadata.authorizationEndpoint()).isEqualTo("https://localhost/auth");
            assertThat(metadata.responseTypesSupported()).containsExactly("code", "id_token");
        } finally {
            server.stop();
        }
    }

    @Test
    void followsRedirectsForProviderMetadataFetches() throws Exception {
        TestHttpsServer server = newServer(jsonHandler(HttpURLConnection.HTTP_OK, validMetadataJson()));
        server.server().createContext("/redirect/.well-known/openid-configuration", exchange -> {
            exchange.getResponseHeaders().add(
                "Location",
                server.baseUri() + "/final/.well-known/openid-configuration"
            );
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_MOVED_PERM, -1);
            exchange.close();
        });
        server.server().createContext("/final/.well-known/openid-configuration", jsonHandler(HttpURLConnection.HTTP_OK, validMetadataJson()));
        server.start();
        try {
            ProviderMetadata metadata = client.fetchProviderMetadata(
                gatewayProvider(server.baseUri() + "/redirect", server.certificate().getEncoded())
            );

            assertThat(metadata.issuer()).isEqualTo("issuer");
        } finally {
            server.stop();
        }
    }

    @Test
    void mapsTlsTrustMismatchToTlsValidationFailed() throws Exception {
        TestHttpsServer server = newServer(jsonHandler(HttpURLConnection.HTTP_OK, validMetadataJson()));
        server.start();
        try {
            KeyPair otherKeyPair = keyPair();
            X509Certificate otherCertificate = certificateFor(otherKeyPair);

            assertFailure(
                gatewayProvider(server.baseUri(), otherCertificate.getEncoded()),
                ProviderMetadataFetchFailure.TLS_VALIDATION_FAILED,
                null
            );
        } finally {
            server.stop();
        }
    }

    @Test
    void mapsNonSuccessHttpStatusToHttpError() throws Exception {
        TestHttpsServer server = newServer(jsonHandler(HttpURLConnection.HTTP_BAD_GATEWAY, "{\"error\":\"boom\"}"));
        server.start();
        try {
            assertFailure(
                gatewayProvider(server.baseUri(), server.certificate().getEncoded()),
                ProviderMetadataFetchFailure.HTTP_ERROR,
                "Unexpected HTTP status 502"
            );
        } finally {
            server.stop();
        }
    }

    @Test
    void mapsInvalidJsonToInvalidJson() throws Exception {
        TestHttpsServer server = newServer(jsonHandler(HttpURLConnection.HTTP_OK, "{"));
        server.start();
        try {
            assertFailure(
                gatewayProvider(server.baseUri(), server.certificate().getEncoded()),
                ProviderMetadataFetchFailure.INVALID_JSON,
                null
            );
        } finally {
            server.stop();
        }
    }

    @Test
    void mapsMissingRequiredFieldToInvalidConfiguration() throws Exception {
        TestHttpsServer server = newServer(jsonHandler(HttpURLConnection.HTTP_OK, """
            {
              "authorization_endpoint": "https://localhost/auth",
              "token_endpoint": "https://localhost/token",
              "jwks_uri": "https://localhost/jwks",
              "response_types_supported": ["code"]
            }
            """));
        server.start();
        try {
            assertFailure(
                gatewayProvider(server.baseUri(), server.certificate().getEncoded()),
                ProviderMetadataFetchFailure.INVALID_CONFIGURATION,
                "issuer must be present"
            );
        } finally {
            server.stop();
        }
    }

    @Test
    void mapsWrongResponseTypesShapeToInvalidConfiguration() throws Exception {
        TestHttpsServer server = newServer(jsonHandler(HttpURLConnection.HTTP_OK, """
            {
              "issuer": "issuer",
              "authorization_endpoint": "https://localhost/auth",
              "token_endpoint": "https://localhost/token",
              "jwks_uri": "https://localhost/jwks",
              "response_types_supported": "code"
            }
            """));
        server.start();
        try {
            assertFailure(
                gatewayProvider(server.baseUri(), server.certificate().getEncoded()),
                ProviderMetadataFetchFailure.INVALID_CONFIGURATION,
                "response_types_supported must be an array"
            );
        } finally {
            server.stop();
        }
    }

    private void assertFailure(
        GatewayProvider provider,
        ProviderMetadataFetchFailure expectedStatus,
        String expectedMessage
    ) {
        assertThatThrownBy(() -> client.fetchProviderMetadata(provider))
            .isInstanceOfSatisfying(ProviderMetadataFetchException.class, exception -> {
                assertThat(exception.status()).isEqualTo(expectedStatus);
                if (expectedMessage != null) {
                    assertThat(exception).hasMessageContaining(expectedMessage);
                }
            });
    }

    private TestHttpsServer newServer(HttpHandler defaultHandler) throws Exception {
        KeyPair keyPair = keyPair();
        X509Certificate certificate = certificateFor(keyPair);
        SSLContext sslContext = sslContext(keyPair, certificate);

        HttpsServer server = HttpsServer.create(new InetSocketAddress(0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        server.createContext("/.well-known/openid-configuration", defaultHandler);
        return new TestHttpsServer(server, certificate);
    }

    private SSLContext sslContext(KeyPair keyPair, X509Certificate certificate) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("server", keyPair.getPrivate(), "changeit".toCharArray(), new java.security.cert.Certificate[] { certificate });

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "changeit".toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());
        return sslContext;
    }

    private KeyPair keyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    private X509Certificate certificateFor(KeyPair keyPair) throws Exception {
        Instant now = Instant.now();
        X500Name subject = new X500Name("CN=localhost");
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
            subject,
            java.math.BigInteger.valueOf(now.toEpochMilli()),
            Date.from(now.minus(1, ChronoUnit.DAYS)),
            Date.from(now.plus(1, ChronoUnit.DAYS)),
            subject,
            keyPair.getPublic()
        );
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

    private HttpHandler jsonHandler(int status, String body) {
        return exchange -> writeResponse(exchange, status, body);
    }

    private void writeResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, responseBody.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBody);
        }
    }

    private String validMetadataJson() {
        return """
            {
              "issuer": "issuer",
              "authorization_endpoint": "https://localhost/auth",
              "token_endpoint": "https://localhost/token",
              "jwks_uri": "https://localhost/jwks",
              "response_types_supported": ["code", "id_token"]
            }
            """;
    }

    private GatewayProvider gatewayProvider(String configEndpoint, byte[] certificate) {
        return new GatewayProvider(
            "Provider",
            "svc-1",
            configEndpoint,
            true,
            certificate,
            null,
            null,
            null,
            null,
            List.of(),
            1L
        );
    }

    private record TestHttpsServer(HttpsServer server, X509Certificate certificate) {

        private void start() {
            server.start();
        }

        private void stop() {
            server.stop(0);
        }

        private String baseUri() {
            return "https://localhost:" + server.getAddress().getPort();
        }
    }
}
