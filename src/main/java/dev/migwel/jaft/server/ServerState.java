package dev.migwel.jaft.server;

import dev.migwel.jaft.statemachine.log.LogEntry;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

/**
 * This represents the mutable state of a server, things that may change as elections happen for example
 * The immutable part of a server is stored in {@link dev.migwel.jaft.server.ServerInfo}
 **/

@ParametersAreNonnullByDefault
public class ServerState {

    private long currentTerm;
    private String currentLeader;
    private String votedFor;
    private Leadership leadership;
    private final List<LogEntry<?, ?>> logs;
    private long commitIndex;
    private long lastApplied;

    public ServerState() {
        this(0);
    }

    public ServerState(long term) {
        this(term, Leadership.Follower);
    }

    public ServerState(long term, Leadership leadership) {
        this.currentTerm = term;
        this.currentLeader = null;
        this.leadership = leadership;
        this.votedFor = null;
        this.logs = new ArrayList<>();
        this.commitIndex = 0;
        this.lastApplied = 0;
    }

    public long getCurrentTerm() {
        return currentTerm;
    }

    public void setCurrentTerm(long currentTerm) {
        this.currentTerm = currentTerm;
    }

    public Leadership getLeadership() {
        return leadership;
    }

    public void becomeFollower(long term) {
        becomeFollower(term, null);
    }

    public void becomeFollower(long term, @CheckForNull String newLeader) {
        if(term <= currentTerm) {
            return;
        }
        this.currentTerm = term;
        this.leadership = Leadership.Follower;
        this.currentLeader = newLeader;
    }

    public void setLeadership(Leadership leadership) {
        this.leadership = leadership;
    }

    public String getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(String votedFor) {
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

    public void setCurrentLeader(String currentLeader) {
        this.currentLeader = currentLeader;
    }

    public void startElection() {
        this.currentTerm++;
        this.leadership = Leadership.Candidate;
    }
}
