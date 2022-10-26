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
            serverState.setCurrentTerm(request.term());
        }

        if (serverState.getLeadership() != Leadership.Follower || serverState.getCurrentLeader() == null) {
            becomeFollower(request.term(), request.leaderId());
        }

        campaignManager.postponeElection();
        return new AppendEntriesResponse(serverState.getCurrentTerm(), true);
    }

    private void becomeFollower(long term, String leaderId) {
        campaignManager.stopHeartbeat();
        serverState.becomeFollower(term, leaderId);
    }

}
