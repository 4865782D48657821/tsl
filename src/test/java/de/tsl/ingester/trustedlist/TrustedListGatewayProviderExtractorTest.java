package de.tsl.ingester.trustedlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tsl.ingester.gatewayprovider.GatewayProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class TrustedListGatewayProviderExtractorTest {

    private static final String XML_PREFIX = """
        <?xml version="1.0" encoding="UTF-8"?>
        <TrustServiceStatusList xmlns="http://uri.etsi.org/02231/v2#">
          <SchemeInformation>
            <TSLSequenceNumber>42</TSLSequenceNumber>
          </SchemeInformation>
          <TrustServiceProviderList>
            <TrustServiceProvider>
              <TSPInformation>
                <TSPName>
                  <Name xml:lang="de">Provider Alpha</Name>
                </TSPName>
              </TSPInformation>
              <TSPServices>
                <TSPService>
                  <ServiceInformation>
                    <ServiceTypeIdentifier>http://uri.etsi.org/TrstSvc/Svctype/unspecified</ServiceTypeIdentifier>
                    <ServiceName>
                      <Name xml:lang="de">service-alpha</Name>
                    </ServiceName>
                    <ServiceSupplyPoints>
                      <ServiceSupplyPoint>https://alpha.example/config</ServiceSupplyPoint>
                    </ServiceSupplyPoints>
                    <ServiceDigitalIdentity>
                      <DigitalId>
                        <X509Certificate>%s</X509Certificate>
                      </DigitalId>
                    </ServiceDigitalIdentity>
                    <ServiceStatus>%s</ServiceStatus>
                    <ServiceInformationExtensions>
                      <Extension>
                        <ExtensionValue>oid_tigw_zugm</ExtensionValue>
                      </Extension>
                    </ServiceInformationExtensions>
                  </ServiceInformation>
                </TSPService>
              </TSPServices>
            </TrustServiceProvider>
          </TrustServiceProviderList>
        </TrustServiceStatusList>
        """;

    private static final String SERVICE_STATUS_IN_ACCORD =
        "http://uri.etsi.org/TrstSvc/Svcstatus/inaccord";
    private static final String SERVICE_STATUS_WITHDRAWN =
        "http://uri.etsi.org/TrstSvc/Svcstatus/withdrawn";

    private final TrustedListXmlParser trustedListXmlParser = new TrustedListXmlParser();
    private final TrustedListGatewayProviderExtractor trustedListGatewayProviderExtractor =
        new TrustedListGatewayProviderExtractor();

    @Test
    void extractsOnlyServicesMatchingTheRequiredTypeAndExtension() throws IOException {
        String xml = new ClassPathResource("fixtures/sample-tsl.xml").getContentAsString(StandardCharsets.UTF_8);

        var trustedList = trustedListXmlParser.parseTrustedList(xml);
        List<GatewayProvider> providers = trustedListGatewayProviderExtractor.extractGatewayProviders(trustedList);

        assertThat(trustedList.sequenceNumber()).isEqualTo(42L);
        assertThat(providers).singleElement().satisfies(provider -> {
            assertThat(provider.name()).isEqualTo("Provider Alpha");
            assertThat(provider.serviceName()).isEqualTo("service-alpha");
            assertThat(provider.configEndpoint()).isEqualTo("https://alpha.example/config");
            assertThat(provider.active()).isTrue();
            assertThat(provider.certificate()).containsExactly(0x01, 0x02, 0x03);
            assertThat(provider.responseTypesSupported()).isEmpty();
            assertThat(provider.version()).isNull();
        });
    }

    @Test
    void marksMatchingServicesOutsideInAccordAsInactive() {
        String xml = XML_PREFIX.formatted("AQID", SERVICE_STATUS_WITHDRAWN);

        var trustedList = trustedListXmlParser.parseTrustedList(xml);
        List<GatewayProvider> providers = trustedListGatewayProviderExtractor.extractGatewayProviders(trustedList);

        assertThat(providers).singleElement().satisfies(provider -> {
            assertThat(provider.serviceName()).isEqualTo("service-alpha");
            assertThat(provider.active()).isFalse();
            assertThat(provider.certificate()).containsExactly(0x01, 0x02, 0x03);
        });
    }

    @Test
    void throwsWhenCertificateCannotBeBase64Decoded() {
        String xml = XML_PREFIX.formatted("%%%invalid%%%", SERVICE_STATUS_IN_ACCORD);

        var trustedList = trustedListXmlParser.parseTrustedList(xml);

        assertThatThrownBy(() -> trustedListGatewayProviderExtractor.extractGatewayProviders(trustedList))
            .isInstanceOf(TrustedListFormatException.class)
            .hasMessage("Unable to decode trusted list certificate");
    }
}
