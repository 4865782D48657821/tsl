package de.tsl.ingester.synchronization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled entry point for periodic gateway provider synchronization.
 */
@Component
public class GatewayProviderSynchronizationScheduler {

    private static final Logger log = LoggerFactory.getLogger(GatewayProviderSynchronizationScheduler.class);

    private final GatewayProviderSynchronizationService synchronizationService;

    /**
     * Creates a scheduler delegating to the synchronization service.
     *
     * @param synchronizationService service invoked on each scheduled execution
     */
    public GatewayProviderSynchronizationScheduler(GatewayProviderSynchronizationService synchronizationService) {
        this.synchronizationService = synchronizationService;
    }

    /**
     * Executes one scheduled synchronization run.
     */
    @Scheduled(
        cron = "${gateway-provider-synchronization.cron}",
        zone = "${gateway-provider-synchronization.zone}"
    )
    public void runSynchronization() {
        log.info("Scheduled gateway provider synchronization triggered");
        GatewayProviderSynchronizationResult result = synchronizationService.synchronize();
        log.info(
            "Scheduled gateway provider synchronization finished: version={}, inserts={}, updates={}, deletions={}",
            result.version(),
            result.insertCount(),
            result.updateCount(),
            result.deleteCount()
        );
    }
}
