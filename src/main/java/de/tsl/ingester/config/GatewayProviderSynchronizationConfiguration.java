package de.tsl.ingester.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tsl.ingester.gatewayprovider.persistence.GatewayProviderBatchWriter;
import de.tsl.ingester.gatewayprovider.persistence.GatewayProviderEntityMapper;
import de.tsl.ingester.gatewayprovider.persistence.GatewayProviderRepository;
import de.tsl.ingester.providermetadata.ProviderMetadataClient;
import de.tsl.ingester.synchronization.GatewayProviderDirectoryReconciler;
import de.tsl.ingester.synchronization.GatewayProviderSynchronizationService;
import de.tsl.ingester.synchronization.ProviderMetadataEnrichmentService;
import de.tsl.ingester.trustedlist.HttpTrustedListClient;
import de.tsl.ingester.trustedlist.TrustedListClient;
import de.tsl.ingester.trustedlist.TrustedListGatewayProviderExtractor;
import de.tsl.ingester.trustedlist.TrustedListXmlParser;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean configuration for the synchronization pipeline and its adapters.
 */
@Configuration
@EnableConfigurationProperties(GatewayProviderSynchronizationProperties.class)
public class GatewayProviderSynchronizationConfiguration {

    /**
     * Creates the Spring configuration instance.
     */
    public GatewayProviderSynchronizationConfiguration() {
    }

    /**
     * Creates the directory reconciler bean.
     *
     * @return the reconciler used to compare trusted-list and persisted providers
     */
    @Bean
    GatewayProviderDirectoryReconciler gatewayProviderDirectoryReconciler() {
        return new GatewayProviderDirectoryReconciler();
    }

    /**
     * Creates the shared JSON mapper bean.
     *
     * @return the application's Jackson object mapper
     */
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Creates the gateway provider entity mapper bean.
     *
     * @return the mapper between domain and persistence models
     */
    @Bean
    GatewayProviderEntityMapper gatewayProviderEntityMapper() {
        return new GatewayProviderEntityMapper();
    }

    /**
     * Creates the trusted-list XML parser bean.
     *
     * @return the JAXB-based trusted-list parser
     */
    @Bean
    TrustedListXmlParser trustedListXmlParser() {
        return new TrustedListXmlParser();
    }

    /**
     * Creates the trusted-list provider extractor bean.
     *
     * @return the extractor for TI gateway providers
     */
    @Bean
    TrustedListGatewayProviderExtractor trustedListGatewayProviderExtractor() {
        return new TrustedListGatewayProviderExtractor();
    }

    /**
     * Creates the trusted-list client bean.
     *
     * @param properties synchronization properties containing the trusted-list URL and timeouts
     * @return the HTTP-based trusted-list client
     */
    @Bean
    TrustedListClient trustedListClient(GatewayProviderSynchronizationProperties properties) {
        return new HttpTrustedListClient(
            properties.trustedListUrl(),
            properties.connectTimeout(),
            properties.readTimeout()
        );
    }

    /**
     * Creates the provider metadata client bean.
     *
     * @param properties synchronization properties containing the HTTP timeouts
     * @param objectMapper shared JSON mapper
     * @return the provider metadata client
     */
    @Bean
    ProviderMetadataClient providerMetadataClient(
        GatewayProviderSynchronizationProperties properties,
        ObjectMapper objectMapper
    ) {
        return new ProviderMetadataClient(properties.connectTimeout(), properties.readTimeout(), objectMapper);
    }

    /**
     * Creates the metadata enrichment service bean.
     *
     * @param client provider metadata client used for outbound well-known fetches
     * @param properties synchronization properties containing the concurrency setting
     * @return the metadata enrichment service
     */
    @Bean
    ProviderMetadataEnrichmentService providerMetadataEnrichmentService(
        ProviderMetadataClient client,
        GatewayProviderSynchronizationProperties properties
    ) {
        return new ProviderMetadataEnrichmentService(client, properties.metadataFetchConcurrency());
    }

    /**
     * Creates the synchronization service bean.
     *
     * @param trustedListClient client for downloading the trusted list XML
     * @param trustedListXmlParser parser for trusted-list XML
     * @param trustedListGatewayProviderExtractor extractor for gateway provider entries
     * @param gatewayProviderRepository repository for current database state
     * @param gatewayProviderEntityMapper mapper between entities and domain objects
     * @param gatewayProviderDirectoryReconciler reconciler for insert, update, and delete planning
     * @param providerMetadataEnrichmentService service for fetching OpenID provider metadata
     * @param gatewayProviderBatchWriter transactional batch writer
     * @return the orchestration service for one synchronization run
     */
    @Bean
    GatewayProviderSynchronizationService gatewayProviderSynchronizationService(
        TrustedListClient trustedListClient,
        TrustedListXmlParser trustedListXmlParser,
        TrustedListGatewayProviderExtractor trustedListGatewayProviderExtractor,
        GatewayProviderRepository gatewayProviderRepository,
        GatewayProviderEntityMapper gatewayProviderEntityMapper,
        GatewayProviderDirectoryReconciler gatewayProviderDirectoryReconciler,
        ProviderMetadataEnrichmentService providerMetadataEnrichmentService,
        GatewayProviderBatchWriter gatewayProviderBatchWriter
    ) {
        return new GatewayProviderSynchronizationService(
            trustedListClient,
            trustedListXmlParser,
            trustedListGatewayProviderExtractor,
            gatewayProviderRepository,
            gatewayProviderEntityMapper,
            gatewayProviderDirectoryReconciler,
            providerMetadataEnrichmentService,
            gatewayProviderBatchWriter
        );
    }
}
