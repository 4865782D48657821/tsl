package de.tsl.ingester.gatewayprovider.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for accessing persisted gateway provider rows.
 */
public interface GatewayProviderRepository extends JpaRepository<GatewayProviderEntity, Long> {
}
