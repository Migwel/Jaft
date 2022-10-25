package dev.migwel.jaft.election;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.migwel.jaft.rpc.AppendEntriesRequest;
import dev.migwel.jaft.rpc.AppendEntriesResponse;
import dev.migwel.jaft.server.ClusterInfo;
import dev.migwel.jaft.server.Leadership;
import dev.migwel.jaft.server.ServerInfo;
import dev.migwel.jaft.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;

@Service
@ParametersAreNonnullByDefault
public class HeartbeatService {

    private static final Logger log = LogManager.getLogger(HeartbeatService.class);

    private final ServerState serverState;
    private final ServerInfo serverInfo;
    private final ClusterInfo clusterInfo;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public HeartbeatService(ServerState serverState, ServerInfo serverInfo, ClusterInfo clusterInfo, ObjectMapper objectMapper, HttpClient httpClient) {
        this.serverState = serverState;
        this.serverInfo = serverInfo;
        this.clusterInfo = clusterInfo;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public void sendHeartbeat() {
        serverState.getElectionLock().lock();
        if (serverState.getLeadership() != Leadership.Leader) {
            log.info("We were going to send a heartbeat but we're no longner the leader, "+ serverState.getCurrentLeader() +" is");
            return;
        }
        String request = buildHeartbeatRequest();
        serverState.getElectionLock().unlock();
        if (request == null) {
            log.warn("Could not build heartbeat request");
            return;
        }
        long highestTermReceived = sendHeartbeat(request);
        if (highestTermReceived > serverState.getCurrentTerm()) {
            serverState.getElectionLock().lock();
            if (highestTermReceived > serverState.getCurrentTerm()) {
                serverState.setCurrentTerm(highestTermReceived);
                serverState.setLeadership(Leadership.Follower);
            }
            serverState.getElectionLock().unlock();
        }
    }

    private long sendHeartbeat(String request) {
        long highestTermReceived = serverState.getCurrentTerm();
        for (ServerInfo serverInfo : clusterInfo.serversInfo()) {
            //Don't ask ourselves for our vote
            if (serverInfo.equals(this.serverInfo)) {
                continue;
            }
            AppendEntriesResponse response = sendHeartbeat(serverInfo, request);
            if (response == null) {
                continue;
            }
            highestTermReceived = Math.max(highestTermReceived, response.term());
        }
        return highestTermReceived;
    }

    @CheckForNull
    private AppendEntriesResponse sendHeartbeat(ServerInfo serverInfo, String request) {
        HttpResponse<String> responseStr;
        try {
            responseStr = httpClient.send(buildHttpRequest(serverInfo, request), HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.warn("Something went wrong while calling server: "+ serverInfo, e);
            return null;
        }
        if (responseStr.statusCode() != 200) {
            return null;
        }
        AppendEntriesResponse response;
        try {
            response = objectMapper.readValue(responseStr.body(), AppendEntriesResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("Could not deserialize the response: "+ responseStr.body(), e);
            return null;
        }
        return response;
    }

    @Nonnull
    private HttpRequest buildHttpRequest(ServerInfo serverInfo, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(serverInfo.serverUrl() + ":"+ serverInfo.serverPort() + "/appendEntries"))
                .header("Content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    @CheckForNull
    private String buildHeartbeatRequest() {
        AppendEntriesRequest request = new AppendEntriesRequest(serverState.getCurrentTerm(),
                serverInfo.serverId(),
                0,
                Collections.emptyList(),
                0);
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.warn("Something went wrong while serializing the request", e);
            return null;
        }
    }
}
