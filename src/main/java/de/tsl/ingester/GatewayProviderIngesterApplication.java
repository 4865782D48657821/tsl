package de.tsl.ingester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point for the gateway provider ingester application.
 */
@SpringBootApplication
@EnableScheduling
public class GatewayProviderIngesterApplication {

    /**
     * Creates the Spring Boot application configuration instance.
     */
    public GatewayProviderIngesterApplication() {
    }

    /**
     * Starts the Spring Boot application.
     *
     * @param args process arguments passed to the JVM entry point
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayProviderIngesterApplication.class, args);
    }
}
