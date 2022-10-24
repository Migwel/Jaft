package dev.migwel.jaft.election;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

@Component
public class CampaignManager {

    private final static long INITIAL_ELECTION_DELAY = 2 * 1000 ;
    private final static long MIN_ELECTION_TIMEOUT_MS = 300;
    private final static long MAX_ELECTION_TIMEOUT_MS = 600;

    private final ThreadPoolTaskScheduler taskScheduler;
    private final ElectionService electionService;
    private final long electionTimeoutMs;
    private ScheduledFuture<?> revoteResult;

    public CampaignManager(ThreadPoolTaskScheduler taskScheduler, ElectionService electionService) {
        this.taskScheduler = taskScheduler;
        this.electionService = electionService;
        this.electionTimeoutMs = MIN_ELECTION_TIMEOUT_MS + (long) (Math.random() * (MAX_ELECTION_TIMEOUT_MS - MIN_ELECTION_TIMEOUT_MS));
        scheduleRevote(INITIAL_ELECTION_DELAY);
    }

    private void scheduleRevote(long startInMs) {
        revoteResult = this.taskScheduler.scheduleWithFixedDelay(electionService::startElection, new Date(new Date().getTime() + startInMs), electionTimeoutMs);
    }

    public void postponeRevote() {
        cancelRevote();
        scheduleRevote(electionTimeoutMs);
    }

    private void cancelRevote() {
        if (revoteResult != null) {
            revoteResult.cancel(true);
            revoteResult = null;
        }
    }

}
