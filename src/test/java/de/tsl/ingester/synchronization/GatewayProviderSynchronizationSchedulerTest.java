package de.tsl.ingester.synchronization;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GatewayProviderSynchronizationSchedulerTest {

    @Test
    void delegatesScheduledInvocationToTheSynchronizationService() {
        GatewayProviderSynchronizationService synchronizationService = Mockito.mock(GatewayProviderSynchronizationService.class);
        when(synchronizationService.synchronize()).thenReturn(new GatewayProviderSynchronizationResult(1L, 1, 2, 3));

        new GatewayProviderSynchronizationScheduler(synchronizationService).runSynchronization();

        verify(synchronizationService).synchronize();
    }
}
