package de.tsl.ingester.gatewayprovider.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * JPA entity mapping for rows in the {@code ti_gateway_provider} table.
 */
@Entity
@Table(name = "ti_gateway_provider")
public class GatewayProviderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ti_gateway_provider_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "config_endpoint")
    private String configEndpoint;

    @Column
    private Boolean active;

    @Lob
    @Column(columnDefinition = "BLOB")
    private byte[] certificate;

    @Column
    private String issuer;

    @Column(name = "authorization_endpoint")
    private String authorizationEndpoint;

    @Column(name = "token_endpoint")
    private String tokenEndpoint;

    @Column(name = "jwks_uri")
    private String jwksUri;

    @Column(name = "response_types_supported")
    private String responseTypesSupported;

    @Column(name = "created_date", insertable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_date", insertable = false, updatable = false)
    private LocalDateTime updatedDate;

    @Column(name = "version", nullable = false)
    private Long providerVersion;

    /**
     * Creates an empty entity instance for JPA.
     */
    protected GatewayProviderEntity() {
    }

    private GatewayProviderEntity(Long id) {
        this.id = id;
    }

    /**
     * Recreates an entity instance from known persisted values.
     *
     * @param id database primary key
     * @param name provider display name
     * @param serviceName provider business key
     * @param configEndpoint provider configuration endpoint
     * @param active active flag derived from the trusted list
     * @param certificate trusted-list certificate bytes
     * @param issuer OpenID issuer
     * @param authorizationEndpoint OpenID authorization endpoint
     * @param tokenEndpoint OpenID token endpoint
     * @param jwksUri OpenID JWKS URI
     * @param responseTypesSupported serialized response types
     * @param providerVersion synchronization version
     * @return the populated entity
     */
    public static GatewayProviderEntity rehydrate(
        Long id,
        String name,
        String serviceName,
        String configEndpoint,
        Boolean active,
        byte[] certificate,
        String issuer,
        String authorizationEndpoint,
        String tokenEndpoint,
        String jwksUri,
        String responseTypesSupported,
        Long providerVersion
    ) {
        GatewayProviderEntity entity = new GatewayProviderEntity(id);
        entity.name = name;
        entity.serviceName = serviceName;
        entity.configEndpoint = configEndpoint;
        entity.active = active;
        entity.certificate = copyCertificate(certificate);
        entity.issuer = issuer;
        entity.authorizationEndpoint = authorizationEndpoint;
        entity.tokenEndpoint = tokenEndpoint;
        entity.jwksUri = jwksUri;
        entity.responseTypesSupported = responseTypesSupported;
        entity.providerVersion = providerVersion;
        return entity;
    }

    /**
     * Applies the supplied state to this entity instance.
     *
     * @param name provider display name
     * @param serviceName provider business key
     * @param configEndpoint provider configuration endpoint
     * @param active active flag derived from the trusted list
     * @param certificate trusted-list certificate bytes
     * @param issuer OpenID issuer
     * @param authorizationEndpoint OpenID authorization endpoint
     * @param tokenEndpoint OpenID token endpoint
     * @param jwksUri OpenID JWKS URI
     * @param responseTypesSupported serialized response types
     * @param providerVersion synchronization version
     */
    public void apply(
        String name,
        String serviceName,
        String configEndpoint,
        Boolean active,
        byte[] certificate,
        String issuer,
        String authorizationEndpoint,
        String tokenEndpoint,
        String jwksUri,
        String responseTypesSupported,
        long providerVersion
    ) {
        this.name = name;
        this.serviceName = serviceName;
        this.configEndpoint = configEndpoint;
        this.active = active;
        this.certificate = copyCertificate(certificate);
        this.issuer = issuer;
        this.authorizationEndpoint = authorizationEndpoint;
        this.tokenEndpoint = tokenEndpoint;
        this.jwksUri = jwksUri;
        this.responseTypesSupported = responseTypesSupported;
        this.providerVersion = providerVersion;
    }

    private static byte[] copyCertificate(byte[] certificate) {
        return certificate == null ? null : Arrays.copyOf(certificate, certificate.length);
    }

    /**
     * Returns the database primary key.
     *
     * @return the primary key value or {@code null} for transient instances
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the provider display name.
     *
     * @return the display name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the provider business key.
     *
     * @return the {@code service_name} value
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Returns the provider configuration endpoint.
     *
     * @return the configuration endpoint
     */
    public String getConfigEndpoint() {
        return configEndpoint;
    }

    /**
     * Returns the provider active flag.
     *
     * @return whether the provider is active
     */
    public Boolean getActive() {
        return active;
    }

    /**
     * Returns a defensive copy of the stored certificate bytes.
     *
     * @return the certificate bytes or {@code null}
     */
    public byte[] getCertificate() {
        return copyCertificate(certificate);
    }

    /**
     * Returns the OpenID issuer.
     *
     * @return the issuer value
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Returns the OpenID authorization endpoint.
     *
     * @return the authorization endpoint
     */
    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    /**
     * Returns the OpenID token endpoint.
     *
     * @return the token endpoint
     */
    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    /**
     * Returns the OpenID JWKS URI.
     *
     * @return the JWKS URI
     */
    public String getJwksUri() {
        return jwksUri;
    }

    /**
     * Returns the serialized response types.
     *
     * @return the comma-separated response types
     */
    public String getResponseTypesSupported() {
        return responseTypesSupported;
    }

    /**
     * Returns the row creation timestamp.
     *
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Returns the row update timestamp.
     *
     * @return the update timestamp
     */
    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    /**
     * Returns the synchronization version stored for this row.
     *
     * @return the synchronization version
     */
    public Long getProviderVersion() {
        return providerVersion;
    }
}
