package dev.migwel.jaft.election;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.migwel.jaft.rpc.RequestVoteResponse;
import dev.migwel.jaft.server.ClusterInfo;
import dev.migwel.jaft.server.Leadership;
import dev.migwel.jaft.server.ServerInfo;
import dev.migwel.jaft.server.ServerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ElectionServiceTest {

    HttpClient httpClient = mock(HttpClient.class);
    ServerState serverState;
    ClusterInfo clusterInfo = buildClusterInfo();
    ServerInfo serverInfo = clusterInfo.serversInfo().get(0);
    ObjectMapper objectMapper = new ObjectMapper();
    ElectionService electionService = new ElectionService(httpClient, serverState, clusterInfo, serverInfo, objectMapper);

    @BeforeEach
    void initSystem() {
        serverState = new ServerState();
        electionService = new ElectionService(httpClient, serverState, clusterInfo, serverInfo, objectMapper);
    }

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
    void cannotWinElectionWithNoVotesGranted() throws IOException, InterruptedException {
        when(httpClient.send(any(), any())).thenReturn(buildHttpResponse(200, 1, false));
        assertFalse(electionService.startElection());
    }

    @Test
    void cannotWinElectionIfUnreachableServers() throws IOException, InterruptedException {
        when(httpClient.send(any(), any())).thenReturn(buildHttpResponse(404, 0, false));
        assertFalse(electionService.startElection());
    }

    @Test
    void responseWithHigherTermShouldUpdateTerm() throws IOException, InterruptedException {
        when(httpClient.send(any(), any())).thenReturn(buildHttpResponse(200, 3, false));
        electionService.startElection();
        assertEquals(3, serverState.getCurrentTerm());
    }

    @Test
    void winElectionIfEnoughVotes() throws IOException, InterruptedException {
        when(httpClient.send(any(), any())).thenReturn(buildHttpResponse(200, 1, true));
        assertTrue(electionService.startElection());
    }

    @Test
    void otherLeaderShouldStopElection() throws IOException, InterruptedException {
        when(httpClient.send(any(), any())).thenAnswer((Answer<HttpResponse<Object>>) invocationOnMock -> {
            Thread.sleep(100);
            return buildHttpResponse(200, 1, true);
        });
        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        threadPool.execute(() -> electionService.startElection());
        threadPool.execute(() -> {
            try {
                Thread.sleep(100);
                electionService.newLeader("MyServer-2", 3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        threadPool.shutdown();
        if (! threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("Could not await termination");
        }
        assertEquals(Leadership.Follower, serverState.getLeadership());
    }

    @Test
    void dontRequestVoteFromOurselves() throws IOException, InterruptedException {
        when(httpClient.send(any(), any())).thenReturn(buildHttpResponse(200, 1, true));
        ArgumentCaptor<HttpRequest> arg = ArgumentCaptor.forClass(HttpRequest.class);
        electionService.startElection();
        verify(httpClient, atLeast(1)).send(arg.capture(), any());
        for (HttpRequest request : arg.getAllValues()) {
            assertNotEquals(URI.create(serverInfo.serverUrl() + ":"+ serverInfo.serverPort() + "/requestVote"), request.uri());
        }
    }

    private HttpResponse<Object> buildHttpResponse(int statusCode, long term, boolean voteGranted) {
        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return statusCode;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<Object>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return null;
            }

            @Override
            public String body() {
                try {
                    return objectMapper.writeValueAsString(new RequestVoteResponse(term, voteGranted));
                } catch (JsonProcessingException e) {
                    return "";
                }
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return null;
            }

            @Override
            public HttpClient.Version version() {
                return null;
            }
        };
    }

}