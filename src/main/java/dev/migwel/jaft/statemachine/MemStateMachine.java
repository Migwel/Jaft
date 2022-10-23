package dev.migwel.jaft.statemachine;

import dev.migwel.jaft.statemachine.log.AddLogEntry;
import dev.migwel.jaft.statemachine.log.DeleteLogEntry;
import dev.migwel.jaft.statemachine.log.LogEntry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemStateMachine implements StateMachine<String, Long> {

    private final Map<String, Long> state = new ConcurrentHashMap<>();
    public Long getValue(String key) {
        return state.get(key);
    }

    public void apply(LogEntry<String, Long> logEntry) {
        switch (logEntry) {
            case AddLogEntry<String, Long> addLogEntry -> addEntry(addLogEntry.getKey(), addLogEntry.getValue());
            case DeleteLogEntry<String, ?> deleteLogEntry -> deleteEntry(deleteLogEntry.getKey());
        }
    }

    private void deleteEntry(String key) {
        state.remove(key);
    }

    private void addEntry(String key, Long value) {
        state.put(key, value);
    }
}
