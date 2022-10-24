package dev.migwel.jaft.election;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.mockito.Mockito.*;

class CampaignManagerTest {

    @Test
    public void electionShouldBeScheduledIfNothingElseHappens() throws InterruptedException {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.initialize();
        ElectionService electionService = mock(ElectionService.class);
        new CampaignManager(taskScheduler, electionService);
        Thread.sleep(3000);
        verify(electionService, atLeast(1)).startElection();
    }

    @Test
    public void electionShouldNotBeScheduledIfTheyArePostponed() throws InterruptedException {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.initialize();
        ElectionService electionService = mock(ElectionService.class);
        CampaignManager campaignManager = new CampaignManager(taskScheduler, electionService);
        for (int i = 0; i < 10; i++) {
            Thread.sleep(200);
            campaignManager.postponeElection();
        }
        verify(electionService, never()).startElection();
    }

}