package dev.migwel.jaft.controller;

import dev.migwel.jaft.rpc.KeyValue;
import dev.migwel.jaft.server.ClusterInfo;
import dev.migwel.jaft.server.Leadership;
import dev.migwel.jaft.server.ServerInfo;
import dev.migwel.jaft.server.ServerState;
import dev.migwel.jaft.statemachine.MemStateMachine;
import dev.migwel.jaft.statemachine.log.AddLogEntry;
import dev.migwel.jaft.statemachine.log.DeleteLogEntry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class StateMachineController {

    private final MemStateMachine stateMachine;
    private final ServerState serverState;
    private final ClusterInfo clusterInfo;

    public StateMachineController(MemStateMachine stateMachine, ServerState serverState, ClusterInfo clusterInfo) {
        this.stateMachine = stateMachine;
        this.serverState = serverState;
        this.clusterInfo = clusterInfo;
    }

    @GetMapping("get")
    public ResponseEntity<KeyValue> get(@RequestBody String key) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new KeyValue(key, stateMachine.getValue(key)));
    }

    @PostMapping("set")
    public ResponseEntity<Void> set(@RequestBody KeyValue keyValue) {
        if (serverState.getLeadership() != Leadership.Leader) {
            HttpHeaders headers = new HttpHeaders();
            String currentLeader = serverState.getCurrentLeader();
            ServerInfo leaderServerInfo = clusterInfo.serversInfo()
                    .stream()
                    .filter(e -> currentLeader.equals(e.serverId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Could not find currentLeader")); // TODO: Use better exception
            headers.setLocation(leaderServerInfo.getURI());
            return ResponseEntity
                    .status(HttpStatus.MOVED_PERMANENTLY)
                    .headers(headers)
                    .build();
        }
        AddLogEntry<String, Long> logEntry = new AddLogEntry<>(keyValue.key(), keyValue.value());
        serverState.addLog(logEntry);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("delete")
    public ResponseEntity<Void> delete(@RequestBody String key) {
        DeleteLogEntry<String, ?> logEntry = new DeleteLogEntry<>(key);
        serverState.addLog(logEntry);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
