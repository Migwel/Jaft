package dev.migwel.jaft.statemachine;

import dev.migwel.jaft.statemachine.log.LogEntry;

public interface StateMachine<K, V> {

    V getValue(K key);
    void apply(LogEntry<K, V> logEntry);
}
