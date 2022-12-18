package dev.migwel.jaft.controller;

import dev.migwel.jaft.rpc.KeyValue;
import dev.migwel.jaft.server.ClusterInfo;
import dev.migwel.jaft.server.Leadership;
import dev.migwel.jaft.server.ServerInfo;
import dev.migwel.jaft.server.ServerState;
import dev.migwel.jaft.statemachine.MemStateMachine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {StateMachineController.class, MemStateMachine.class})
class StateMachineControllerTest {

    @MockBean
    private ServerState serverState;
    @MockBean
    private ClusterInfo clusterInfo;
    private final StateMachineController stateMachineController;

    @Autowired
    StateMachineControllerTest(StateMachineController stateMachineController) {
        this.stateMachineController = stateMachineController;
    }

    @Test
    public void contextLoads() {
        assertNotNull(stateMachineController);
    }

    @Test
    public void canSetValueOnLeader() {
        when(serverState.getLeadership()).thenReturn(Leadership.Leader);
        stateMachineController.set(new KeyValue("key", 1L));
        verify(serverState, times(1)).addLog(any());
    }

    @Test
    public void setValueOnFollowerShouldRedirect() {
        when(serverState.getLeadership()).thenReturn(Leadership.Follower);
        ServerInfo leaderInfo = new ServerInfo("0", "http://localhost", "8080");
        when(serverState.getCurrentLeader()).thenReturn(leaderInfo.serverId());
        when(clusterInfo.serversInfo())
                .thenReturn(List.of(leaderInfo));
        ResponseEntity<Void> response = stateMachineController.set(new KeyValue("key", 1L));
        verify(serverState, times(0)).addLog(any());
        assertEquals(leaderInfo.getURI(), response.getHeaders().getLocation());
    }

    @Test
    public void canDeleteValueOnLeader() {
        when(serverState.getLeadership()).thenReturn(Leadership.Leader);
        stateMachineController.delete("key");
        verify(serverState, times(1)).addLog(any());
    }

    @Test
    public void deleteOnFollowerShouldRedirect() {
        when(serverState.getLeadership()).thenReturn(Leadership.Follower);
        ServerInfo leaderInfo = new ServerInfo("0", "http://localhost", "8080");
        when(serverState.getCurrentLeader()).thenReturn(leaderInfo.serverId());
        when(clusterInfo.serversInfo())
                .thenReturn(List.of(leaderInfo));
        ResponseEntity<Void> response = stateMachineController.delete("key");
        verify(serverState, times(0)).addLog(any());
        assertEquals(leaderInfo.getURI(), response.getHeaders().getLocation());
    }

}