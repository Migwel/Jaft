package dev.migwel.jaft.election;

import dev.migwel.jaft.rpc.RequestVoteRequest;
import dev.migwel.jaft.rpc.RequestVoteResponse;
import dev.migwel.jaft.server.Leadership;
import dev.migwel.jaft.server.ServerState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VoteServiceTest {

    @Test
    void rejectVoteRequestWithLowerTerm() {
        ServerState serverState = new ServerState(3);
        VoteService voteService = new VoteService(serverState);
        RequestVoteRequest request = new RequestVoteRequest(2, "myId", 0, 0);
        RequestVoteResponse response = voteService.requestVote(request);
        assertFalse(response.voteGranted());
    }

    @Test
    void rejectVoteRequestIfAlreadyVotedInCurrentTermForOtherCandidate() {
        ServerState serverState = new ServerState(3);
        VoteService voteService = new VoteService(serverState);
        RequestVoteRequest request = new RequestVoteRequest(4, "myId", 0, 0);
        voteService.requestVote(request);
        RequestVoteRequest otherRequest = new RequestVoteRequest(4, "myOtherId", 0, 0);
        RequestVoteResponse response = voteService.requestVote(otherRequest);
        assertFalse(response.voteGranted());
    }

    @Test
    void grantVoteRequestIfAlreadyVotedInCurrentTermForSameCandidate() {
        ServerState serverState = new ServerState(3);
        VoteService voteService = new VoteService(serverState);
        RequestVoteRequest request = new RequestVoteRequest(4, "myId", 0, 0);
        voteService.requestVote(request);
        RequestVoteRequest otherRequest = new RequestVoteRequest(4, "myId", 0, 0);
        RequestVoteResponse response = voteService.requestVote(otherRequest);
        assertTrue(response.voteGranted());
    }

    @Test
    void serverInCorrectStateAfterGrantingVote() {
        ServerState serverState = new ServerState(3);
        VoteService voteService = new VoteService(serverState);
        RequestVoteRequest request = new RequestVoteRequest(4, "myId", 0, 0);
        voteService.requestVote(request);
        assertEquals(4, serverState.getCurrentTerm());
        assertEquals(Leadership.Follower, serverState.getLeadership());
        assertEquals("myId", serverState.getVotedFor());
    }

    @Test
    void grantVoteRequestWithAllGoodValues() {
        ServerState serverState = new ServerState(3);
        VoteService voteService = new VoteService(serverState);
        RequestVoteRequest request = new RequestVoteRequest(4, "myId", 0, 0);
        RequestVoteResponse response = voteService.requestVote(request);
        assertTrue(response.voteGranted());
    }

}