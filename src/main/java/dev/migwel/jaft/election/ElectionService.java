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

@Service
@ParametersAreNonnullByDefault
public class ElectionService {

    private static final Logger log = LogManager.getLogger(ElectionService.class);
    private final HttpClient httpClient;
    private final ServerState serverState;
    private final ClusterInfo clusterInfo;
    private final ServerInfo serverInfo;
    private final ObjectMapper objectMapper;

    @Autowired
    public ElectionService(HttpClient httpClient, ServerState serverState, ClusterInfo clusterInfo, ServerInfo serverInfo, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.serverState = serverState;
        this.clusterInfo = clusterInfo;
        this.serverInfo = serverInfo;
        this.objectMapper = objectMapper;
    }

    public void newLeader(String leaderId, long term) {
        serverState.becomeFollower(term, leaderId);
    }

    /**
     *
     * @return true if election is won. False otherwise
     */
    public boolean startElection() {
        long electionTerm = serverState.startElection();
        VotingResult votingResult = requestVotes(electionTerm);
        return serverState.decideElection(votingResult);
    }

    private VotingResult requestVotes(long electionTerm) {
        int votes = 1; // We vote for ourselves
        long highestTermReceived = 0;
        for (ServerInfo serverInfo : clusterInfo.serversInfo()) {
            //Don't ask ourselves for our vote
            if (serverInfo.equals(this.serverInfo)) {
                continue;
            }
            RequestVoteResponse response = requestVote(electionTerm, serverInfo);
            if (response == null) {
                continue;
            }
            highestTermReceived = Math.max(highestTermReceived, response.term());
            if (response.voteGranted()) {
                votes++;
            }
        }
        return new VotingResult(votes, highestTermReceived);
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
                .uri(URI.create(serverInfo.serverUrl() + ":"+ serverInfo.serverPort() + "/requestVote"))
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
