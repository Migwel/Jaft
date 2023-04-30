package dev.migwel.jaft.election;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.migwel.jaft.http.HttpSender;
import dev.migwel.jaft.rpc.AppendEntriesRequest;
import dev.migwel.jaft.rpc.AppendEntriesResponse;
import dev.migwel.jaft.server.ClusterInfo;
import dev.migwel.jaft.server.Leadership;
import dev.migwel.jaft.server.ServerInfo;
import dev.migwel.jaft.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;

@Service
@ParametersAreNonnullByDefault
public class HeartbeatService {

    private static final Logger log = LogManager.getLogger(HeartbeatService.class);

    private final ServerState serverState;
    private final ServerInfo serverInfo;
    private final ClusterInfo clusterInfo;
    private final HttpSender httpSender;

    @Autowired
    public HeartbeatService(ServerState serverState, ServerInfo serverInfo, ClusterInfo clusterInfo, ObjectMapper objectMapper, HttpSender httpSender) {
        this.serverState = serverState;
        this.serverInfo = serverInfo;
        this.clusterInfo = clusterInfo;
        this.httpSender = httpSender;
    }

    public boolean sendHeartbeat() {
        ServerState.CurrentTermLeadership currentTermLeadership = serverState.getCurrentTermLeadership();
        if (currentTermLeadership.leadership() != Leadership.Leader) {
            log.info("We were going to send a heartbeat but we're no longer the leader, "+ serverState.getCurrentLeader() +" is");
            return false;
        }
        AppendEntriesRequest request = buildHeartbeatRequest(currentTermLeadership.currentTerm());
        long highestTermReceived = sendHeartbeat(request);
        if (highestTermReceived > serverState.getCurrentTerm()) {
            serverState.becomeFollower(highestTermReceived, null);
        }
        return true;
    }

    private long sendHeartbeat(AppendEntriesRequest request) {
        long highestTermReceived = serverState.getCurrentTerm();
        for (ServerInfo serverInfo : clusterInfo.serversInfo()) {
            //Don't ask ourselves for our vote
            if (serverInfo.equals(this.serverInfo)) {
                continue;
            }
            AppendEntriesResponse response = httpSender.send(serverInfo.getURI("/appendEntries"), request, AppendEntriesResponse.class);
            if (response == null) {
                continue;
            }
            highestTermReceived = Math.max(highestTermReceived, response.term());
        }
        return highestTermReceived;
    }

    private AppendEntriesRequest buildHeartbeatRequest(long currentTerm) {
        return new AppendEntriesRequest(currentTerm,
                serverInfo.serverId(),
                0,
                Collections.emptyList(),
                0);
    }
}
