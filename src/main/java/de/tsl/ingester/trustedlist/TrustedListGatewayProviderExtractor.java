package de.tsl.ingester.trustedlist;

import de.tsl.ingester.gatewayprovider.GatewayProvider;
import de.tsl.ingester.trustedlist.xml.TrustedListXml;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts TI gateway provider entries from the parsed trusted list.
 *
 * <p>Only services matching the required service type and extension are mapped into the domain
 * model used by the synchronization flow.
 */
public class TrustedListGatewayProviderExtractor {

    private static final Logger log = LoggerFactory.getLogger(TrustedListGatewayProviderExtractor.class);

    private static final String SERVICE_TYPE_UNSPECIFIED = "http://uri.etsi.org/TrstSvc/Svctype/unspecified";
    private static final String SERVICE_STATUS_IN_ACCORD = "http://uri.etsi.org/TrstSvc/Svcstatus/inaccord";

    /**
     * Creates a new stateless trusted-list gateway provider extractor.
     */
    public TrustedListGatewayProviderExtractor() {
    }

    /**
     * Filters trusted-list services and maps matching entries into normalized domain providers.
     *
     * @param trustedList the parsed trusted list document
     * @return all matching gateway providers in traversal order
     * @throws TrustedListFormatException when a referenced certificate cannot be decoded
     */
    public List<GatewayProvider> extractGatewayProviders(TrustedListXml trustedList) {
        List<GatewayProvider> providers = new ArrayList<>();
        for (TrustedListXml.TrustedListProviderXml provider : trustedList.providers()) {
            for (TrustedListXml.ProviderServiceXml service : provider.services()) {
                TrustedListXml.ServiceInformationXml information = service.information();
                if (information == null || !matchesService(information)) {
                    continue;
                }
                providers.add(new GatewayProvider(
                    provider.providerName(),
                    information.serviceName(),
                    information.configEndpoint(),
                    SERVICE_STATUS_IN_ACCORD.equals(information.serviceStatus()),
                    decodeCertificate(information.firstCertificate()),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null
                ));
            }
        }
        log.info("Extracted {} matching gateway providers from the trusted list", providers.size());
        return providers;
    }

    private boolean matchesService(TrustedListXml.ServiceInformationXml service) {
        if (!SERVICE_TYPE_UNSPECIFIED.equals(service.serviceTypeIdentifier())) {
            return false;
        }
        for (String extensionValue : service.extensionValues()) {
            if ("oid_tigw_zugm".equals(extensionValue == null ? null : extensionValue.trim())) {
                return true;
            }
        }
        return false;
    }

    private byte[] decodeCertificate(String value) {
        if (value == null) {
            return new byte[0];
        }
        try {
            return Base64.getDecoder().decode(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new TrustedListFormatException("Unable to decode trusted list certificate", exception);
        }
    }
}
