package dev.migwel.jaft.election;

import dev.migwel.jaft.rpc.RequestVoteRequest;
import dev.migwel.jaft.rpc.RequestVoteResponse;
import dev.migwel.jaft.server.Leadership;
import dev.migwel.jaft.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@Service
@ParametersAreNonnullByDefault
public class VoteService {
    private static final Logger log = LogManager.getLogger(VoteService.class);

    private final ServerState serverState;
    private final CampaignManager campaignManager;

    @Autowired
    public VoteService(ServerState serverState, CampaignManager campaignManager) {
        this.serverState = serverState;
        this.campaignManager = campaignManager;
    }

    //TODO: Check if synchronized can be moved to something more granular
    @Nonnull
    public RequestVoteResponse requestVote(RequestVoteRequest request) {
        boolean voteGranted = serverState.requestVote(request.term(), request.candidateId());
        log.info("Vote granted: "+ voteGranted +" for candidate "+ request.candidateId());
        if (voteGranted) {
            campaignManager.postponeElection();
        }
        return new RequestVoteResponse(serverState.getCurrentTerm(), voteGranted);
    }
}
