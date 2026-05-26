package de.tsl.ingester.synchronization;

/**
 * Summary of a completed synchronization run.
 *
 * @param version synchronization version applied during the run
 * @param insertCount number of inserted provider rows
 * @param updateCount number of updated provider rows
 * @param deleteCount number of deleted provider rows
 */
public record GatewayProviderSynchronizationResult(
    long version,
    int insertCount,
    int updateCount,
    int deleteCount
) {
}
