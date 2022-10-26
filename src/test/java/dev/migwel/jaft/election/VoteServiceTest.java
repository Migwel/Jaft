package dev.migwel.jaft.election;

import dev.migwel.jaft.rpc.RequestVoteRequest;
import dev.migwel.jaft.rpc.RequestVoteResponse;
import dev.migwel.jaft.server.ClusterInfo;
import dev.migwel.jaft.server.Leadership;
import dev.migwel.jaft.server.ServerInfo;
import dev.migwel.jaft.server.ServerState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class VoteServiceTest {

    private final CampaignManager campaignManager = mock(CampaignManager.class);
    private final ClusterInfo clusterInfo = buildClusterInfo();

    private ClusterInfo buildClusterInfo() {
        List<ServerInfo> serversInfo = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ServerInfo serverInfo = buildServerInfo(i);
            serversInfo.add(serverInfo);
        }
        return new ClusterInfo(serversInfo);
    }

    private ServerInfo buildServerInfo(int serverId) {
        return new ServerInfo("MyServer-"+ serverId, "https://localhost", "808"+ serverId);
    }

    @Test
    void rejectVoteRequestWithLowerTerm() {
        ServerState serverState = new ServerState(clusterInfo, 3);
        VoteService voteService = new VoteService(serverState, campaignManager);
        RequestVoteRequest request = new RequestVoteRequest(2, "myId", 0, 0);
        RequestVoteResponse response = voteService.requestVote(request);
        assertFalse(response.voteGranted());
    }

    @Test
    void rejectVoteRequestIfAlreadyVotedInCurrentTermForOtherCandidate() {
        ServerState serverState = new ServerState(clusterInfo, 3);
        VoteService voteService = new VoteService(serverState, campaignManager);
        RequestVoteRequest request = new RequestVoteRequest(4, "myId", 0, 0);
        voteService.requestVote(request);
        RequestVoteRequest otherRequest = new RequestVoteRequest(4, "myOtherId", 0, 0);
        RequestVoteResponse response = voteService.requestVote(otherRequest);
        assertFalse(response.voteGranted());
    }

    @Test
    void grantVoteRequestIfAlreadyVotedInCurrentTermForSameCandidate() {
        ServerState serverState = new ServerState(clusterInfo, 3);
        VoteService voteService = new VoteService(serverState, campaignManager);
        RequestVoteRequest request = new RequestVoteRequest(4, "myId", 0, 0);
        voteService.requestVote(request);
        RequestVoteRequest otherRequest = new RequestVoteRequest(4, "myId", 0, 0);
        RequestVoteResponse response = voteService.requestVote(otherRequest);
        assertTrue(response.voteGranted());
    }

    @Test
    void serverInCorrectStateAfterGrantingVote() {
        ServerState serverState = new ServerState(clusterInfo, 3);
        VoteService voteService = new VoteService(serverState, campaignManager);
        RequestVoteRequest request = new RequestVoteRequest(4, "myId", 0, 0);
        voteService.requestVote(request);
        assertEquals(4, serverState.getCurrentTerm());
        assertEquals(Leadership.Follower, serverState.getLeadership());
        assertEquals("myId", serverState.getVotedFor());
    }

    @Test
    void grantVoteRequestWithAllGoodValues() {
        ServerState serverState = new ServerState(clusterInfo, 3);
        VoteService voteService = new VoteService(serverState, campaignManager);
        RequestVoteRequest request = new RequestVoteRequest(4, "myId", 0, 0);
        RequestVoteResponse response = voteService.requestVote(request);
        assertTrue(response.voteGranted());
    }

}