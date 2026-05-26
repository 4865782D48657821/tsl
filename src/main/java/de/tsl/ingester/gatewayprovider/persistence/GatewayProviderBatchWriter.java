package de.tsl.ingester.gatewayprovider.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists a prepared synchronization change set as batched SQL statements inside one transaction.
 */
@Component
public class GatewayProviderBatchWriter {

    private static final Logger log = LoggerFactory.getLogger(GatewayProviderBatchWriter.class);

    private final JdbcTemplate jdbcTemplate;

    /**
     * Creates a new batch writer backed by the supplied JDBC template.
     *
     * @param jdbcTemplate JDBC template used for the batched SQL statements
     */
    public GatewayProviderBatchWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Applies all inserts, updates, and deletions atomically.
     *
     * @param inserts providers to insert
     * @param updates providers to update
     * @param deletions providers to delete
     */
    @Transactional
    public void writeChanges(
        List<GatewayProviderEntity> inserts,
        List<GatewayProviderEntity> updates,
        List<GatewayProviderEntity> deletions
    ) {
        log.info(
            "Writing persistence plan to database: inserts={}, updates={}, deletions={}",
            inserts.size(),
            updates.size(),
            deletions.size()
        );
        if (!inserts.isEmpty()) {
            jdbcTemplate.batchUpdate(
                """
                INSERT INTO ti_gateway_provider (
                    name,
                    service_name,
                    config_endpoint,
                    active,
                    certificate,
                    issuer,
                    authorization_endpoint,
                    token_endpoint,
                    jwks_uri,
                    response_types_supported,
                    version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                inserts,
                inserts.size(),
                this::bindForInsert
            );
        }

        if (!updates.isEmpty()) {
            jdbcTemplate.batchUpdate(
                """
                UPDATE ti_gateway_provider
                SET name = ?,
                    config_endpoint = ?,
                    active = ?,
                    certificate = ?,
                    issuer = ?,
                    authorization_endpoint = ?,
                    token_endpoint = ?,
                    jwks_uri = ?,
                    response_types_supported = ?,
                    version = ?,
                    updated_date = CURRENT_TIMESTAMP
                WHERE service_name = ?
                """,
                updates,
                updates.size(),
                this::bindForUpdate
            );
        }

        if (!deletions.isEmpty()) {
            jdbcTemplate.batchUpdate(
                """
                DELETE FROM ti_gateway_provider
                WHERE service_name = ?
                """,
                deletions,
                deletions.size(),
                this::bindForDelete
            );
        }
        log.info("Database persistence plan applied successfully");
    }

    private void bindForInsert(PreparedStatement statement, GatewayProviderEntity entity) throws SQLException {
        statement.setString(1, entity.getName());
        statement.setString(2, entity.getServiceName());
        statement.setString(3, entity.getConfigEndpoint());
        statement.setObject(4, entity.getActive());
        statement.setBytes(5, entity.getCertificate());
        statement.setString(6, entity.getIssuer());
        statement.setString(7, entity.getAuthorizationEndpoint());
        statement.setString(8, entity.getTokenEndpoint());
        statement.setString(9, entity.getJwksUri());
        statement.setString(10, entity.getResponseTypesSupported());
        statement.setLong(11, entity.getProviderVersion());
    }

    private void bindForUpdate(PreparedStatement statement, GatewayProviderEntity entity) throws SQLException {
        statement.setString(1, entity.getName());
        statement.setString(2, entity.getConfigEndpoint());
        statement.setObject(3, entity.getActive());
        statement.setBytes(4, entity.getCertificate());
        statement.setString(5, entity.getIssuer());
        statement.setString(6, entity.getAuthorizationEndpoint());
        statement.setString(7, entity.getTokenEndpoint());
        statement.setString(8, entity.getJwksUri());
        statement.setString(9, entity.getResponseTypesSupported());
        statement.setLong(10, entity.getProviderVersion());
        statement.setString(11, entity.getServiceName());
    }

    private void bindForDelete(PreparedStatement statement, GatewayProviderEntity entity) throws SQLException {
        statement.setString(1, entity.getServiceName());
    }
}
