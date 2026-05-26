package de.tsl.ingester.trustedlist.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import java.util.ArrayList;
import java.util.List;

/**
 * JAXB root model for the trusted-list XML subset consumed by the synchronization pipeline.
 */
@XmlRootElement(name = "TrustServiceStatusList")
@XmlAccessorType(XmlAccessType.FIELD)
public class TrustedListXml {

    @XmlElement(name = "SchemeInformation")
    private SchemeInformationXml schemeInformation;

    @XmlElement(name = "TrustServiceProviderList")
    private TrustedListProviderListXml trustedListProviderList;

    /**
     * Creates an empty trusted-list root object for JAXB unmarshalling.
     */
    public TrustedListXml() {
    }

    /**
     * Returns the TSL sequence number when present.
     *
     * @return the parsed sequence number or {@code null}
     */
    public Long sequenceNumber() {
        if (schemeInformation == null || schemeInformation.sequenceNumber == null || schemeInformation.sequenceNumber.isBlank()) {
            return null;
        }
        return Long.parseLong(schemeInformation.sequenceNumber.trim());
    }

    /**
     * Returns all trusted-list providers contained in the document.
     *
     * @return immutable provider list
     */
    public List<TrustedListProviderXml> providers() {
        if (trustedListProviderList == null || trustedListProviderList.providers == null) {
            return List.of();
        }
        return List.copyOf(trustedListProviderList.providers);
    }

    /**
     * JAXB model for the trusted-list scheme information section.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SchemeInformationXml {

        @XmlElement(name = "TSLSequenceNumber")
        private String sequenceNumber;

        SchemeInformationXml() {
        }
    }

    /**
     * JAXB model for the list of trusted service providers.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class TrustedListProviderListXml {

        @XmlElement(name = "TrustServiceProvider")
        private List<TrustedListProviderXml> providers = new ArrayList<>();

        TrustedListProviderListXml() {
        }
    }

    /**
     * JAXB model for a single trusted service provider.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class TrustedListProviderXml {

        @XmlElement(name = "TSPInformation")
        private TspInformationXml information;

        @XmlElement(name = "TSPServices")
        private ProviderServicesXml services;

        TrustedListProviderXml() {
        }

        /**
         * Returns the provider display name.
         *
         * @return the first localized provider name or {@code null}
         */
        public String providerName() {
            if (information == null || information.name == null || information.name.names == null || information.name.names.isEmpty()) {
                return null;
            }
            return information.name.names.getFirst().value;
        }

        /**
         * Returns the services exposed by this provider.
         *
         * @return immutable service list
         */
        public List<ProviderServiceXml> services() {
            if (services == null || services.services == null) {
                return List.of();
            }
            return List.copyOf(services.services);
        }
    }

    /**
     * JAXB model for the provider information section.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class TspInformationXml {

        @XmlElement(name = "TSPName")
        private NameListXml name;

        TspInformationXml() {
        }
    }

    /**
     * JAXB model for the services container of one provider.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ProviderServicesXml {

        @XmlElement(name = "TSPService")
        private List<ProviderServiceXml> services = new ArrayList<>();

        ProviderServicesXml() {
        }
    }

    /**
     * JAXB model for one trusted service entry.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ProviderServiceXml {

        @XmlElement(name = "ServiceInformation")
        private ServiceInformationXml information;

        ProviderServiceXml() {
        }

        /**
         * Returns the service information payload.
         *
         * @return the service information or {@code null}
         */
        public ServiceInformationXml information() {
            return information;
        }
    }

    /**
     * JAXB model for the service information section used by the ingester.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ServiceInformationXml {

        @XmlElement(name = "ServiceTypeIdentifier")
        private String serviceTypeIdentifier;

        @XmlElement(name = "ServiceName")
        private NameListXml serviceName;

        @XmlElement(name = "ServiceSupplyPoints")
        private ServiceSupplyPointsXml serviceSupplyPoints;

        @XmlElement(name = "ServiceDigitalIdentity")
        private ServiceDigitalIdentityXml serviceDigitalIdentity;

        @XmlElement(name = "ServiceStatus")
        private String serviceStatus;

        @XmlElement(name = "ServiceInformationExtensions")
        private ServiceInformationExtensionsXml serviceInformationExtensions;

        ServiceInformationXml() {
        }

        /**
         * Returns the ETSI service type identifier.
         *
         * @return the service type identifier or {@code null}
         */
        public String serviceTypeIdentifier() {
            return serviceTypeIdentifier;
        }

        /**
         * Returns the service display name.
         *
         * @return the first localized service name or {@code null}
         */
        public String serviceName() {
            if (serviceName == null || serviceName.names == null || serviceName.names.isEmpty()) {
                return null;
            }
            return serviceName.names.getFirst().value;
        }

        /**
         * Returns the provider configuration endpoint.
         *
         * @return the first service supply point or {@code null}
         */
        public String configEndpoint() {
            if (serviceSupplyPoints == null || serviceSupplyPoints.supplyPoints == null || serviceSupplyPoints.supplyPoints.isEmpty()) {
                return null;
            }
            return serviceSupplyPoints.supplyPoints.getFirst();
        }

        /**
         * Returns the ETSI service status URI.
         *
         * @return the service status or {@code null}
         */
        public String serviceStatus() {
            return serviceStatus;
        }

        /**
         * Returns all raw extension values on the service.
         *
         * @return immutable extension value list
         */
        public List<String> extensionValues() {
            if (serviceInformationExtensions == null || serviceInformationExtensions.extensions == null) {
                return List.of();
            }
            return serviceInformationExtensions.extensions.stream()
                .map(ExtensionXml::value)
                .toList();
        }

        /**
         * Returns the first embedded X.509 certificate value.
         *
         * @return the first non-blank certificate value or {@code null}
         */
        public String firstCertificate() {
            if (serviceDigitalIdentity == null || serviceDigitalIdentity.digitalIds == null) {
                return null;
            }
            for (DigitalIdXml digitalId : serviceDigitalIdentity.digitalIds) {
                if (digitalId != null && digitalId.x509Certificate != null && !digitalId.x509Certificate.isBlank()) {
                    return digitalId.x509Certificate;
                }
            }
            return null;
        }
    }

    /**
     * JAXB model for a localized name collection.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class NameListXml {

        @XmlElement(name = "Name")
        private List<LocalizedNameXml> names = new ArrayList<>();

        NameListXml() {
        }
    }

    /**
     * JAXB model for a single localized name value.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LocalizedNameXml {

        @XmlValue
        private String value;

        LocalizedNameXml() {
        }
    }

    /**
     * JAXB model for service supply points.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ServiceSupplyPointsXml {

        @XmlElement(name = "ServiceSupplyPoint")
        private List<String> supplyPoints = new ArrayList<>();

        ServiceSupplyPointsXml() {
        }
    }

    /**
     * JAXB model for service digital identities.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ServiceDigitalIdentityXml {

        @XmlElement(name = "DigitalId")
        private List<DigitalIdXml> digitalIds = new ArrayList<>();

        ServiceDigitalIdentityXml() {
        }
    }

    /**
     * JAXB model for one digital identity entry.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DigitalIdXml {

        @XmlElement(name = "X509Certificate")
        private String x509Certificate;

        DigitalIdXml() {
        }
    }

    /**
     * JAXB model for service information extensions.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ServiceInformationExtensionsXml {

        @XmlElement(name = "Extension")
        private List<ExtensionXml> extensions = new ArrayList<>();

        ServiceInformationExtensionsXml() {
        }
    }

    /**
     * JAXB model for one service extension.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ExtensionXml {

        @XmlElement(name = "ExtensionValue")
        private String extensionValue;

        ExtensionXml() {
        }

        /**
         * Returns the raw extension value.
         *
         * @return the extension value or {@code null}
         */
        public String value() {
            return extensionValue;
        }
    }
}
