package dev.migwel.jaft.server;

import dev.migwel.jaft.election.VotingResult;
import dev.migwel.jaft.statemachine.log.LogEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This represents the mutable state of a server, things that may change as elections happen for example
 * The immutable part of a server is stored in {@link dev.migwel.jaft.server.ServerInfo}
 **/

@ParametersAreNonnullByDefault
public class ServerState {
    private static final Logger log = LogManager.getLogger(ServerState.class);

    private final ClusterInfo clusterInfo;
    private long currentTerm;
    private String currentLeader;
    @CheckForNull private String votedFor;
    private Leadership leadership;
    private final List<LogEntry<?, ?>> logs;
    private long commitIndex;
    private long lastApplied;
    private final Lock electionLock;

    public ServerState(ClusterInfo clusterInfo) {
        this(clusterInfo, 0);
    }

    public ServerState(ClusterInfo clusterInfo, long term) {
        this(clusterInfo, term, Leadership.Follower);
    }

    public ServerState(ClusterInfo clusterInfo, long term, Leadership leadership) {
        this.clusterInfo = clusterInfo;
        this.currentTerm = term;
        this.currentLeader = null;
        this.leadership = leadership;
        this.votedFor = null;
        this.logs = new ArrayList<>();
        this.commitIndex = 0;
        this.lastApplied = 0;
        this.electionLock = new ReentrantLock();
    }

    public long getCurrentTerm() {
        return currentTerm;
    }

    public void setCurrentTerm(long newTerm) {
        electionLock.lock();
        try {
            if (newTerm > this.currentTerm) {
                this.currentTerm = newTerm;
            }
        } finally {
            electionLock.unlock();
        }
    }

    public Leadership getLeadership() {
        return leadership;
    }

    public CurrentTermLeadership getCurrentTermLeadership() {
        electionLock.lock();
        try {
            return new CurrentTermLeadership(currentTerm, leadership);
        } finally {
            electionLock.unlock();
        }
    }

    public record CurrentTermLeadership(long currentTerm, Leadership leadership){}

    public void becomeFollower(long term, @CheckForNull String newLeader) {
        electionLock.lock();
        try {
            log.info("We are becoming follower for the new leader "+ newLeader +" of term "+ term);
            if (term < currentTerm) {
                return;
            }
            setCurrentTerm(term);
            setLeadership(Leadership.Follower);
            setCurrentLeader(newLeader);
        } finally {
            electionLock.unlock();
        }
    }

    public void setLeadership(Leadership leadership) {
        this.leadership = leadership;
    }

    public String getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(@CheckForNull String votedFor) {
        this.votedFor = votedFor;
    }

    public List<LogEntry<?, ?>> getLogs() {
        return logs;
    }

    public void addLog(LogEntry<?, ?> logEntry) {
        logs.add(logEntry);
    }

    public long getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(long commitIndex) {
        this.commitIndex = commitIndex;
    }

    public long getLastApplied() {
        return lastApplied;
    }

    public void setLastApplied(long lastApplied) {
        this.lastApplied = lastApplied;
    }

    public long nextTerm() {
        return ++this.currentTerm;
    }

    public String getCurrentLeader() {
        return currentLeader;
    }

    public void setCurrentLeader(@CheckForNull String currentLeader) {
        this.currentLeader = currentLeader;
    }

    public long startElection() {
        electionLock.lock();
        try {
            setLeadership(Leadership.Candidate);
            log.info("Starting election for term "+ currentTerm + 1);
            return ++this.currentTerm;
        } finally {
            electionLock.unlock();
        }
    }

    public boolean decideElection(VotingResult votingResult) {
        electionLock.lock();
        try {
            //It can happen that a new leader has been elected in the meantime
            //In which case we should not become a leader
            if (leadership != Leadership.Candidate) {
                log.info("We are not longer candidates but "+ leadership);
                return false;
            }

            if (votingResult.highestTermReceived() > currentTerm) {
                log.info("Highest term received ("+ votingResult.highestTermReceived() +") is higher than currentTerm ("+ currentTerm +")");
                setLeadership(Leadership.Follower);
                setCurrentTerm(votingResult.highestTermReceived());
                setVotedFor(null);
                return false;
            }

            if (votingResult.nbVotes() > clusterInfo.serversInfo().size() / 2) {
                setLeadership(Leadership.Leader);
                log.info("We received "+ votingResult.nbVotes() + ". We are leaders now");
                return true;
            }
            log.info("We have not received enough votes : "+ votingResult.nbVotes() + ". We cannot become leaders");
            return false;
        } finally {
            electionLock.unlock();
        }
    }

    public boolean requestVote(long term, String candidateId) {
        electionLock.lock();
        try {
            log.info("Vote request received from "+ candidateId +" for term "+ term);
            if (term < currentTerm) {
                log.info("Term received ("+ term +") is lower than current term "+ currentTerm +")");
                return false;
            }
            if (term == currentTerm &&
                    !candidateId.equals(votedFor)) {
                log.info("We already voted for this term and voted for "+ votedFor);
                return false;
            }

            setCurrentTerm(term);
            setLeadership(Leadership.Follower);
            setVotedFor(candidateId);
            log.info("Giving our vote to "+ candidateId +" for term "+ term);
            return true;
        } finally {
            electionLock.unlock();
        }
    }
}
