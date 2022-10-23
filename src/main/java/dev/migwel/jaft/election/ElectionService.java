package dev.migwel.jaft.election;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.migwel.jaft.rpc.RequestVoteRequest;
import dev.migwel.jaft.rpc.RequestVoteResponse;
import dev.migwel.jaft.server.ClusterInfo;
import dev.migwel.jaft.server.Leadership;
import dev.migwel.jaft.server.ServerInfo;
import dev.migwel.jaft.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@ParametersAreNonnullByDefault
public class ElectionService {

    private static final Logger log = LogManager.getLogger(ElectionService.class);
    private final HttpClient httpClient;
    private final ServerState serverState;
    private final ClusterInfo clusterInfo;
    private final ServerInfo serverInfo;
    private final ObjectMapper objectMapper;
    private final Lock electionLock;

    @Autowired
    public ElectionService(HttpClient httpClient, ServerState serverState, ClusterInfo clusterInfo, ServerInfo serverInfo, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.serverState = serverState;
        this.clusterInfo = clusterInfo;
        this.serverInfo = serverInfo;
        this.objectMapper = objectMapper;
        this.electionLock = new ReentrantLock();
    }

    public void newLeader(String leaderId, long term) {
        electionLock.lock();
        serverState.becomeFollower(term, leaderId);
        electionLock.unlock();
    }

    /**
     *
     * @return true if election is won. False otherwise
     */
    public boolean startElection() {
        electionLock.lock();
        serverState.startElection();
        long electionTerm = serverState.getCurrentTerm();
        electionLock.unlock();
        Integer votes = requestVotes(electionTerm);
        if (votes == null) {
            return false;
        }

        //It can happen that a new leader has been elected in the meantime
        //In which case we should not become a leader
        if (serverState.getLeadership() != Leadership.Candidate) {
            return false;
        }

        if (votes > clusterInfo.serversInfo().size() / 2) {
            serverState.setLeadership(Leadership.Leader);
            return true;
        }
        return false;
    }

    private Integer requestVotes(long electionTerm) {
        int votes = 1; // We vote for ourselves
        for (ServerInfo serverInfo : clusterInfo.serversInfo()) {
            RequestVoteResponse response = requestVote(electionTerm, serverInfo);
            if (response == null) {
                continue;
            }
            if (response.voteGranted()) {
                votes++;
                continue;
            }
            if (response.term() > electionTerm) {
                electionLock.lock();
                serverState.becomeFollower(response.term());
                electionLock.unlock();
                return null;
            }
        }
        return votes;
    }

    @CheckForNull
    private RequestVoteResponse requestVote(long electionTerm, ServerInfo serverInfo) {
        String voteRequest = buildVoteRequest(electionTerm);
        if (voteRequest == null) {
            return null;
        }
        HttpResponse<String> responseStr;
        try {
            responseStr = httpClient.send(buildHttpRequest(serverInfo, voteRequest), HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.warn("Something went wrong while calling server: "+ serverInfo, e);
            return null;
        }
        if (responseStr.statusCode() != 200) {
            return null;
        }
        RequestVoteResponse response;
        try {
            response = objectMapper.readValue(responseStr.body(), RequestVoteResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("Could not deserialize the response: "+ responseStr.body(), e);
            return null;
        }
        return response;
    }

    @Nonnull
    private HttpRequest buildHttpRequest(ServerInfo serverInfo, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(serverInfo.serverUrl() + ":"+ serverInfo.serverPort()))
                .header("Content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    @CheckForNull
    private String buildVoteRequest(long electionTerm) {
        RequestVoteRequest request = new RequestVoteRequest(electionTerm, serverInfo.serverId(), 0L, 0L);
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.warn("Something went wrong while serializing request", e);
            return null;
        }
    }
}
