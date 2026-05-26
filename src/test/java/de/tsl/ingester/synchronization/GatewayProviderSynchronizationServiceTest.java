package de.tsl.ingester.synchronization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tsl.ingester.gatewayprovider.GatewayProvider;
import de.tsl.ingester.gatewayprovider.persistence.GatewayProviderBatchWriter;
import de.tsl.ingester.gatewayprovider.persistence.GatewayProviderEntity;
import de.tsl.ingester.gatewayprovider.persistence.GatewayProviderEntityMapper;
import de.tsl.ingester.gatewayprovider.persistence.GatewayProviderRepository;
import de.tsl.ingester.providermetadata.ProviderMetadata;
import de.tsl.ingester.trustedlist.TrustedListClient;
import de.tsl.ingester.trustedlist.TrustedListGatewayProviderExtractor;
import de.tsl.ingester.trustedlist.TrustedListXmlParser;
import de.tsl.ingester.trustedlist.xml.TrustedListXml;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Captor;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GatewayProviderSynchronizationServiceTest {

    @Mock
    private TrustedListClient trustedListClient;

    @Mock
    private TrustedListXmlParser trustedListXmlParser;

    @Mock
    private TrustedListGatewayProviderExtractor trustedListGatewayProviderExtractor;

    @Mock
    private GatewayProviderRepository gatewayProviderRepository;

    @Mock
    private ProviderMetadataEnrichmentService providerMetadataEnrichmentService;

    @Mock
    private GatewayProviderBatchWriter gatewayProviderBatchWriter;

    @Captor
    private ArgumentCaptor<List<GatewayProviderEntity>> insertsCaptor;

    @Captor
    private ArgumentCaptor<List<GatewayProviderEntity>> updatesCaptor;

    @Captor
    private ArgumentCaptor<List<GatewayProviderEntity>> deletionsCaptor;

    private final GatewayProviderDirectoryReconciler gatewayProviderDirectoryReconciler =
        new GatewayProviderDirectoryReconciler();
    private final GatewayProviderEntityMapper gatewayProviderEntityMapper = new GatewayProviderEntityMapper();

    @Test
    void abortsWithoutPersistingWhenARequiredProviderMetadataFetchFails() {
        GatewayProviderSynchronizationService synchronizationService = synchronizationService();
        TrustedListXml trustedList = parsedTrustedList(9L);
        GatewayProvider gatewayProvider = new GatewayProvider(
            "Provider",
            "svc-1",
            "https://svc-1.example",
            true,
            new byte[] { 0x01, 0x02, 0x03 },
            null,
            null,
            null,
            null,
            List.of(),
            null
        );

        when(trustedListClient.fetchTrustedListXml()).thenReturn("<xml/>");
        when(trustedListXmlParser.parseTrustedList("<xml/>")).thenReturn(trustedList);
        when(trustedListGatewayProviderExtractor.extractGatewayProviders(trustedList)).thenReturn(List.of(gatewayProvider));
        when(gatewayProviderRepository.findAll()).thenReturn(List.of());
        when(providerMetadataEnrichmentService.enrichProviders(anyList())).thenThrow(
            new GatewayProviderSynchronizationException("One or more provider metadata fetches failed: HTTP_ERROR [svc-1] boom")
        );

        assertThatThrownBy(synchronizationService::synchronize)
            .isInstanceOf(GatewayProviderSynchronizationException.class)
            .hasMessageContaining("provider metadata")
            .hasMessageContaining("HTTP_ERROR")
            .hasMessageContaining("boom");

        verify(gatewayProviderBatchWriter, never()).writeChanges(anyList(), anyList(), anyList());
    }

    @Test
    void writesPreparedDirectoryChangesAfterSuccessfulProviderMetadataEnrichment() {
        GatewayProviderSynchronizationService synchronizationService = synchronizationService();
        TrustedListXml trustedList = parsedTrustedList(9L);
        GatewayProvider gatewayProvider = new GatewayProvider(
            "Provider",
            "svc-1",
            "https://svc-1.example",
            true,
            new byte[] { 0x01, 0x02, 0x03 },
            null,
            null,
            null,
            null,
            List.of(),
            null
        );
        GatewayProvider enrichedProvider = gatewayProvider.withVersion(9L).withProviderMetadata(providerMetadata());

        when(trustedListClient.fetchTrustedListXml()).thenReturn("<xml/>");
        when(trustedListXmlParser.parseTrustedList("<xml/>")).thenReturn(trustedList);
        when(trustedListGatewayProviderExtractor.extractGatewayProviders(trustedList)).thenReturn(List.of(gatewayProvider));
        when(gatewayProviderRepository.findAll()).thenReturn(List.of());
        when(providerMetadataEnrichmentService.enrichProviders(anyList())).thenReturn(List.of(enrichedProvider));

        GatewayProviderSynchronizationResult result = synchronizationService.synchronize();

        assertThat(result.version()).isEqualTo(9L);
        assertThat(result.insertCount()).isEqualTo(1);
        verify(gatewayProviderBatchWriter).writeChanges(
            insertsCaptor.capture(),
            updatesCaptor.capture(),
            deletionsCaptor.capture()
        );
        assertThat(insertsCaptor.getValue()).singleElement().satisfies(entity -> {
            assertThat(((GatewayProviderEntity) entity).getServiceName()).isEqualTo("svc-1");
            assertThat(((GatewayProviderEntity) entity).getIssuer()).isEqualTo("issuer");
        });
        assertThat(updatesCaptor.getValue()).isEmpty();
        assertThat(deletionsCaptor.getValue()).isEmpty();
    }

    @Test
    void deletesPersistedProvidersThatAreNoLongerPresentInTheTrustedList() {
        GatewayProviderSynchronizationService synchronizationService = synchronizationService();
        TrustedListXml trustedList = parsedTrustedList(11L);
        GatewayProvider trustedListProvider = new GatewayProvider(
            "Provider",
            "svc-current",
            "https://svc-current.example",
            true,
            new byte[] { 0x01, 0x02, 0x03 },
            null,
            null,
            null,
            null,
            List.of(),
            null
        );
        GatewayProvider persistedProvider = new GatewayProvider(
            "Removed",
            "svc-removed",
            "https://svc-removed.example",
            true,
            new byte[] { 0x04, 0x05, 0x06 },
            "old-issuer",
            "https://svc-removed.example/auth",
            "https://svc-removed.example/token",
            "https://svc-removed.example/jwks",
            List.of("code"),
            7L
        );
        GatewayProvider enrichedProvider = trustedListProvider.withVersion(11L).withProviderMetadata(providerMetadata());

        when(trustedListClient.fetchTrustedListXml()).thenReturn("<xml/>");
        when(trustedListXmlParser.parseTrustedList("<xml/>")).thenReturn(trustedList);
        when(trustedListGatewayProviderExtractor.extractGatewayProviders(trustedList)).thenReturn(List.of(trustedListProvider));
        when(gatewayProviderRepository.findAll()).thenReturn(List.of(gatewayProviderEntityMapper.toEntity(persistedProvider)));
        when(providerMetadataEnrichmentService.enrichProviders(anyList())).thenReturn(List.of(enrichedProvider));

        GatewayProviderSynchronizationResult result = synchronizationService.synchronize();

        assertThat(result.insertCount()).isEqualTo(1);
        assertThat(result.updateCount()).isZero();
        assertThat(result.deleteCount()).isEqualTo(1);
        verify(gatewayProviderBatchWriter).writeChanges(
            insertsCaptor.capture(),
            updatesCaptor.capture(),
            deletionsCaptor.capture()
        );
        assertThat(insertsCaptor.getValue()).singleElement()
            .extracting(GatewayProviderEntity::getServiceName)
            .isEqualTo("svc-current");
        assertThat(updatesCaptor.getValue()).isEmpty();
        assertThat(deletionsCaptor.getValue()).singleElement()
            .extracting(GatewayProviderEntity::getServiceName)
            .isEqualTo("svc-removed");
    }

    private GatewayProviderSynchronizationService synchronizationService() {
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

    private TrustedListXml parsedTrustedList(long sequenceNumber) {
        return new TrustedListXmlParser().parseTrustedList("""
            <TrustServiceStatusList xmlns="http://uri.etsi.org/02231/v2#">
              <SchemeInformation>
                <TSLSequenceNumber>%s</TSLSequenceNumber>
              </SchemeInformation>
            </TrustServiceStatusList>
            """.formatted(sequenceNumber));
    }

    private ProviderMetadata providerMetadata() {
        return new ProviderMetadata(
            "issuer",
            "https://svc-1.example/auth",
            "https://svc-1.example/token",
            "https://svc-1.example/jwks",
            List.of("code")
        );
    }
}
