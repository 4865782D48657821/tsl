package de.tsl.ingester.trustedlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class HttpTrustedListClientTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    @Test
    void fetchesTrustedListXmlFromConfiguredUrl() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/trusted-list.xml", xmlHandler(200, "<xml>trusted-list</xml>"));
        server.start();
        try {
            HttpTrustedListClient client = new HttpTrustedListClient(
                "http://localhost:%d/trusted-list.xml".formatted(server.getAddress().getPort()),
                TIMEOUT,
                TIMEOUT
            );

            String xml = client.fetchTrustedListXml();

            assertThat(xml).isEqualTo("<xml>trusted-list</xml>");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsNonSuccessHttpResponses() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/trusted-list.xml", xmlHandler(502, "<error/>"));
        server.start();
        try {
            HttpTrustedListClient client = new HttpTrustedListClient(
                "http://localhost:%d/trusted-list.xml".formatted(server.getAddress().getPort()),
                TIMEOUT,
                TIMEOUT
            );

            assertThatThrownBy(client::fetchTrustedListXml)
                .isInstanceOf(TrustedListFormatException.class)
                .hasMessage("Trusted list fetch failed with HTTP status 502");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void wrapsTransportErrorsWhenTheEndpointCannotBeReached() throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        HttpTrustedListClient client = new HttpTrustedListClient(
            "http://localhost:%d/trusted-list.xml".formatted(port),
            TIMEOUT,
            TIMEOUT
        );

        assertThatThrownBy(client::fetchTrustedListXml)
            .isInstanceOf(TrustedListFormatException.class)
            .hasMessage("Unable to fetch trusted list XML");
    }

    private HttpHandler xmlHandler(int status, String body) {
        return exchange -> writeResponse(exchange, status, body);
    }

    private void writeResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/xml");
        exchange.sendResponseHeaders(status, responseBody.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBody);
        }
    }
}
