package de.tsl.ingester.synchronization;

import de.tsl.ingester.gatewayprovider.GatewayProvider;
import de.tsl.ingester.gatewayprovider.persistence.GatewayProviderBatchWriter;
import de.tsl.ingester.gatewayprovider.persistence.GatewayProviderEntity;
import de.tsl.ingester.gatewayprovider.persistence.GatewayProviderEntityMapper;
import de.tsl.ingester.gatewayprovider.persistence.GatewayProviderRepository;
import de.tsl.ingester.trustedlist.TrustedListClient;
import de.tsl.ingester.trustedlist.TrustedListFormatException;
import de.tsl.ingester.trustedlist.TrustedListGatewayProviderExtractor;
import de.tsl.ingester.trustedlist.TrustedListXmlParser;
import de.tsl.ingester.trustedlist.xml.TrustedListXml;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrates the end-to-end gateway provider synchronization pipeline.
 */
public class GatewayProviderSynchronizationService {

    private static final Logger log = LoggerFactory.getLogger(GatewayProviderSynchronizationService.class);

    private final TrustedListClient trustedListClient;
    private final TrustedListXmlParser trustedListXmlParser;
    private final TrustedListGatewayProviderExtractor trustedListGatewayProviderExtractor;
    private final GatewayProviderRepository gatewayProviderRepository;
    private final GatewayProviderEntityMapper gatewayProviderEntityMapper;
    private final GatewayProviderDirectoryReconciler gatewayProviderDirectoryReconciler;
    private final ProviderMetadataEnrichmentService providerMetadataEnrichmentService;
    private final GatewayProviderBatchWriter gatewayProviderBatchWriter;

    /**
     * Creates the synchronization service with all required adapters and collaborators.
     *
     * @param trustedListClient client for downloading the trusted list XML
     * @param trustedListXmlParser parser for trusted-list XML
     * @param trustedListGatewayProviderExtractor extractor for gateway provider entries
     * @param gatewayProviderRepository repository for current database state
     * @param gatewayProviderEntityMapper mapper between entities and domain objects
     * @param gatewayProviderDirectoryReconciler reconciler for insert, update, and delete planning
     * @param providerMetadataEnrichmentService service for fetching OpenID provider metadata
     * @param gatewayProviderBatchWriter transactional batch writer
     */
    public GatewayProviderSynchronizationService(
        TrustedListClient trustedListClient,
        TrustedListXmlParser trustedListXmlParser,
        TrustedListGatewayProviderExtractor trustedListGatewayProviderExtractor,
        GatewayProviderRepository gatewayProviderRepository,
        GatewayProviderEntityMapper gatewayProviderEntityMapper,
        GatewayProviderDirectoryReconciler gatewayProviderDirectoryReconciler,
        ProviderMetadataEnrichmentService providerMetadataEnrichmentService,
        GatewayProviderBatchWriter gatewayProviderBatchWriter
    ) {
        this.trustedListClient = trustedListClient;
        this.trustedListXmlParser = trustedListXmlParser;
        this.trustedListGatewayProviderExtractor = trustedListGatewayProviderExtractor;
        this.gatewayProviderRepository = gatewayProviderRepository;
        this.gatewayProviderEntityMapper = gatewayProviderEntityMapper;
        this.gatewayProviderDirectoryReconciler = gatewayProviderDirectoryReconciler;
        this.providerMetadataEnrichmentService = providerMetadataEnrichmentService;
        this.gatewayProviderBatchWriter = gatewayProviderBatchWriter;
    }

    /**
     * Executes one complete synchronization run from trusted-list download to atomic persistence.
     *
     * @return a summary of the completed synchronization run
     * @throws TrustedListFormatException when trusted-list fetch or parsing fails
     * @throws GatewayProviderSynchronizationException when metadata enrichment or persistence
     *     preparation fails
     */
    public GatewayProviderSynchronizationResult synchronize() {
        log.info("Starting gateway provider synchronization");

        log.info("Fetching trusted list XML");
        String xml = trustedListClient.fetchTrustedListXml();

        log.info("Parsing trusted list XML");
        TrustedListXml trustedList = trustedListXmlParser.parseTrustedList(xml);

        log.info("Extracting gateway providers from the trusted list");
        long version = trustedList.sequenceNumber() != null ? trustedList.sequenceNumber() : System.currentTimeMillis();
        List<GatewayProvider> trustedListProviders = trustedListGatewayProviderExtractor.extractGatewayProviders(trustedList).stream()
            .map(provider -> provider.withVersion(version))
            .toList();

        log.info("Loading current ti_gateway_provider rows from database");
        List<GatewayProvider> persistedProviders = gatewayProviderRepository.findAll().stream()
            .map(gatewayProviderEntityMapper::toDomain)
            .toList();
        log.info(
            "Trusted list parsed: version={}, matchingProviders={}, currentDbEntries={}",
            version,
            trustedListProviders.size(),
            persistedProviders.size()
        );

        log.info("Reconciling provider directory");
        GatewayProviderDirectoryChanges directoryChanges = gatewayProviderDirectoryReconciler.reconcile(
            trustedListProviders,
            persistedProviders,
            version
        );
        log.info(
            "Directory changes prepared: inserts={}, updates={}, deletions={}",
            directoryChanges.inserts().size(),
            directoryChanges.updates().size(),
            directoryChanges.deletions().size()
        );

        log.info("Enriching gateway providers with provider metadata");
        List<GatewayProvider> enrichedProviders = providerMetadataEnrichmentService.enrichProviders(
            directoryChanges.currentTrustedListProviders()
        );
        java.util.Map<String, GatewayProvider> byServiceName = enrichedProviders.stream()
            .collect(java.util.stream.Collectors.toMap(GatewayProvider::serviceName, provider -> provider));
        List<GatewayProvider> inserts = directoryChanges.inserts().stream()
            .map(provider -> byServiceName.get(provider.serviceName()))
            .toList();
        List<GatewayProvider> updates = directoryChanges.updates().stream()
            .map(provider -> byServiceName.get(provider.serviceName()))
            .toList();
        List<GatewayProvider> deletions = directoryChanges.deletions();
        List<GatewayProviderEntity> insertEntities = inserts.stream()
            .map(gatewayProviderEntityMapper::toEntity)
            .toList();
        List<GatewayProviderEntity> updateEntities = updates.stream()
            .map(gatewayProviderEntityMapper::toEntity)
            .toList();
        List<GatewayProviderEntity> deletionEntities = deletions.stream()
            .map(gatewayProviderEntityMapper::toEntity)
            .toList();

        log.info(
            "Writing directory changes atomically: inserts={}, updates={}, deletions={}",
            insertEntities.size(),
            updateEntities.size(),
            deletionEntities.size()
        );
        gatewayProviderBatchWriter.writeChanges(insertEntities, updateEntities, deletionEntities);

        GatewayProviderSynchronizationResult result = new GatewayProviderSynchronizationResult(
            version,
            insertEntities.size(),
            updateEntities.size(),
            deletionEntities.size()
        );
        log.info(
            "Gateway provider synchronization completed successfully: version={}, inserts={}, updates={}, deletions={}",
            result.version(),
            result.insertCount(),
            result.updateCount(),
            result.deleteCount()
        );
        return result;
    }
}
