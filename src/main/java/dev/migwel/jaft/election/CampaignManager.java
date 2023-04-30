package dev.migwel.jaft.election;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

@Component
public class CampaignManager {

    private final static long DEBUGGING_FACTOR = 50; //Make things slower to help debug
    private final static long INITIAL_ELECTION_DELAY = 2 * 1000;
    private final static long MIN_ELECTION_TIMEOUT_MS = 300 * DEBUGGING_FACTOR;
    private final static long MAX_ELECTION_TIMEOUT_MS = 600 * DEBUGGING_FACTOR;
    private final static long HEARTBEAT_MS = 100 * DEBUGGING_FACTOR;

    private final ThreadPoolTaskScheduler taskScheduler;
    private final ElectionService electionService;
    private final HeartbeatService heartbeatService;
    private final long electionTimeoutMs;
    private ScheduledFuture<?> revoteResult;
    private ScheduledFuture<?> heartbeatResult;

   @Autowired
    public CampaignManager(ThreadPoolTaskScheduler taskScheduler, ElectionService electionService, HeartbeatService heartbeatService) {
        this.taskScheduler = taskScheduler;
        this.electionService = electionService;
        this.heartbeatService = heartbeatService;
        this.electionTimeoutMs = MIN_ELECTION_TIMEOUT_MS + (long) (Math.random() * (MAX_ELECTION_TIMEOUT_MS - MIN_ELECTION_TIMEOUT_MS));
        scheduleElection(INITIAL_ELECTION_DELAY);
    }

    private void scheduleElection(long startInMs) {
        revoteResult = this.taskScheduler.scheduleWithFixedDelay(this::startElection, new Date(new Date().getTime() + startInMs), electionTimeoutMs);
    }

    private void scheduleHeartbeat() {
        heartbeatResult = this.taskScheduler.scheduleWithFixedDelay(this::sendHeartbeat, new Date(), HEARTBEAT_MS);
    }

    private void startElection() {
        boolean electionWon = electionService.startElection();
        if (electionWon) {
            cancelRevote();
            scheduleHeartbeat();
        }
    }

    private void sendHeartbeat() {
        if (!heartbeatService.sendHeartbeat()) {
            stopHeartbeat();
        }
    }

    public void stopHeartbeat() {
        if (heartbeatResult != null) {
            heartbeatResult.cancel(true);
            heartbeatResult = null;
        }
    }

    public void postponeElection() {
        cancelRevote();
        scheduleElection(electionTimeoutMs);
    }

    private void cancelRevote() {
        if (revoteResult != null) {
            revoteResult.cancel(true);
            revoteResult = null;
        }
    }

}
