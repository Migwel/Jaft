package dev.migwel.jaft.election;

import dev.migwel.jaft.rpc.RequestVoteRequest;
import dev.migwel.jaft.rpc.RequestVoteResponse;
import dev.migwel.jaft.server.Leadership;
import dev.migwel.jaft.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@Service
@ParametersAreNonnullByDefault
public class VoteService {

    private static final Logger log = LogManager.getLogger(VoteService.class);

    private final ServerState serverState;
    private final CampaignManager campaignManager;

    public VoteService(ServerState serverState, CampaignManager campaignManager) {
        this.serverState = serverState;
        this.campaignManager = campaignManager;
    }

    //TODO: Check if synchronized can be moved to something more granular
    @Nonnull
    public synchronized RequestVoteResponse requestVote(RequestVoteRequest request) {
        serverState.getElectionLock().lock();
        try {
            if (request.term() < serverState.getCurrentTerm()) {
                return new RequestVoteResponse(serverState.getCurrentTerm(), false);
            }

            if (request.term() == serverState.getCurrentTerm() &&
                    !request.candidateId().equals(serverState.getVotedFor())) {
                return new RequestVoteResponse(serverState.getCurrentTerm(), false);
            }

            serverState.setCurrentTerm(request.term());
            serverState.setLeadership(Leadership.Follower);
            serverState.setVotedFor(request.candidateId());
            campaignManager.postponeElection();
            log.info("We voted for server "+ request.candidateId());
            return new RequestVoteResponse(request.term(), true);
        } finally {
            serverState.getElectionLock().unlock();
        }
    }
}
