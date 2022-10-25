package dev.migwel.jaft.statemachine;

import dev.migwel.jaft.election.CampaignManager;
import dev.migwel.jaft.rpc.AppendEntriesRequest;
import dev.migwel.jaft.rpc.AppendEntriesResponse;
import dev.migwel.jaft.server.Leadership;
import dev.migwel.jaft.server.ServerState;
import org.springframework.stereotype.Service;

@Service
public class LogService {

    private final CampaignManager campaignManager;
    private final ServerState serverState;

    public LogService(CampaignManager campaignManager, ServerState serverState) {
        this.campaignManager = campaignManager;
        this.serverState = serverState;
    }

    public AppendEntriesResponse appendEntries(AppendEntriesRequest request) {
        if (request.term() < serverState.getCurrentTerm()) {
            return new AppendEntriesResponse(serverState.getCurrentTerm(), false);
        } else if (request.term() > serverState.getCurrentTerm()) {
            updateTerm(request.term());
        }

        if (serverState.getLeadership() != Leadership.Follower) {
            becomeFollower(request.term());
        }

        campaignManager.postponeElection();
        return new AppendEntriesResponse(serverState.getCurrentTerm(), true);
    }

    private void becomeFollower(long term) {
        serverState.getElectionLock().lock();
        if (serverState.getLeadership() != Leadership.Follower &&
            term >= serverState.getCurrentTerm()) {
            serverState.setLeadership(Leadership.Follower);
        }
        campaignManager.stopHeartbeat();
        serverState.getElectionLock().unlock();
    }

    private void updateTerm(long term) {
        serverState.getElectionLock().lock();
        if (term > serverState.getCurrentTerm()) {
            serverState.setCurrentTerm(term);
        }
        serverState.getElectionLock().unlock();
    }
}
