package dev.migwel.jaft.statemachine;

import dev.migwel.jaft.election.CampaignManager;
import dev.migwel.jaft.rpc.AppendEntriesRequest;
import dev.migwel.jaft.rpc.AppendEntriesResponse;
import dev.migwel.jaft.server.ClusterInfo;
import dev.migwel.jaft.server.Leadership;
import dev.migwel.jaft.server.ServerInfo;
import dev.migwel.jaft.server.ServerState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogServiceTest {

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
    void successfulResponseIfAllIsCorrect() {
        ServerState serverState = new ServerState(clusterInfo, 1);
        LogService logService = new LogService(campaignManager, serverState);
        AppendEntriesRequest request = new AppendEntriesRequest(1, "MyServer", 0, Collections.emptyList(), 0);
        AppendEntriesResponse response = logService.appendEntries(request);
        assertTrue(response.success());
    }

    @Test
    void correctRequestShouldPostponeElections() throws InterruptedException {
        ServerState serverState = new ServerState(clusterInfo, 1);
        LogService logService = new LogService(campaignManager, serverState);
        AppendEntriesRequest request = new AppendEntriesRequest(1, "MyServer", 0, Collections.emptyList(), 0);
        for (int i = 0; i < 10; i++) {
            logService.appendEntries(request);
            Thread.sleep(100);
        }
        verify(campaignManager, times(10)).postponeElection();
    }

    @Test
    void rejectAppendEntriesWithLowerTerm() {
        ServerState serverState = new ServerState(clusterInfo, 3);
        LogService logService = new LogService(campaignManager, serverState);
        AppendEntriesRequest request = new AppendEntriesRequest(1, "MyServer", 0, Collections.emptyList(), 0);
        AppendEntriesResponse response = logService.appendEntries(request);
        assertFalse(response.success());
    }

    @Test
    void candidateReceivingAppendEntriesShouldConvertToFollower() {
        ServerState serverState = new ServerState(clusterInfo, 1, Leadership.Candidate);
        LogService logService = new LogService(campaignManager, serverState);
        AppendEntriesRequest request = new AppendEntriesRequest(1, "MyServer", 0, Collections.emptyList(), 0);
        logService.appendEntries(request);
        assertEquals(Leadership.Follower, serverState.getLeadership());
    }

    @Test
    void appendEntriesWithHigherTermShouldUpdateServerCurrentTerm() {
        ServerState serverState = new ServerState(clusterInfo, 1);
        LogService logService = new LogService(campaignManager, serverState);
        AppendEntriesRequest request = new AppendEntriesRequest(2, "MyServer", 0, Collections.emptyList(), 0);
        logService.appendEntries(request);
        assertEquals(2, serverState.getCurrentTerm());
    }
}