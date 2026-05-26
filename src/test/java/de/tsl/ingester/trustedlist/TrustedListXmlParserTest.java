package de.tsl.ingester.trustedlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tsl.ingester.trustedlist.xml.TrustedListXml;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class TrustedListXmlParserTest {

    private final TrustedListXmlParser parser = new TrustedListXmlParser();

    @Test
    void parsesRelevantTrustedListFieldsFromXmlFixture() throws IOException {
        String xml = new ClassPathResource("fixtures/sample-tsl.xml").getContentAsString(StandardCharsets.UTF_8);

        TrustedListXml trustedList = parser.parseTrustedList(xml);

        assertThat(trustedList.sequenceNumber()).isEqualTo(42L);
        assertThat(trustedList.providers()).singleElement().satisfies(provider -> {
            assertThat(provider.providerName()).isEqualTo("Provider Alpha");
            assertThat(provider.services()).hasSize(2);
            assertThat(provider.services().getFirst().information().serviceName()).isEqualTo("service-alpha");
            assertThat(provider.services().getFirst().information().configEndpoint()).isEqualTo("https://alpha.example/config");
            assertThat(provider.services().getFirst().information().serviceStatus())
                .isEqualTo("http://uri.etsi.org/TrstSvc/Svcstatus/inaccord");
            assertThat(provider.services().getFirst().information().extensionValues()).containsExactly("oid_tigw_zugm");
            assertThat(provider.services().getFirst().information().firstCertificate()).isEqualTo("AQID");
        });
    }

    @Test
    void wrapsMalformedXmlInTrustedListFormatException() {
        String malformedXml = "<TrustServiceStatusList><broken></TrustServiceStatusList>";

        assertThatThrownBy(() -> parser.parseTrustedList(malformedXml))
            .isInstanceOf(TrustedListFormatException.class)
            .hasMessage("Unable to parse trusted list XML");
    }
}
