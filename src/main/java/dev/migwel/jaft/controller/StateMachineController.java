package dev.migwel.jaft.controller;

import dev.migwel.jaft.rpc.KeyValue;
import dev.migwel.jaft.server.ServerState;
import dev.migwel.jaft.statemachine.MemStateMachine;
import dev.migwel.jaft.statemachine.log.AddLogEntry;
import dev.migwel.jaft.statemachine.log.DeleteLogEntry;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class StateMachineController {

    private final MemStateMachine stateMachine;
    private final ServerState serverState;

    public StateMachineController(MemStateMachine stateMachine, ServerState serverState) {
        this.stateMachine = stateMachine;
        this.serverState = serverState;
    }

    @GetMapping("get")
    public KeyValue get(@RequestBody String key) {
        return new KeyValue(key, stateMachine.getValue(key));
    }

    @PostMapping("set")
    public void set(@RequestBody KeyValue keyValue) {
        AddLogEntry<String, Long> logEntry = new AddLogEntry<>(keyValue.key(), keyValue.value());
        serverState.addLog(logEntry);
    }

    @DeleteMapping("delete")
    public void delete(@RequestBody String key) {
        DeleteLogEntry<String, ?> logEntry = new DeleteLogEntry<>(key);
        serverState.addLog(logEntry);
    }
}
