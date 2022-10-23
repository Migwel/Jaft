package dev.migwel.jaft.statemachine.log;

public sealed interface LogEntry<K, V> permits AddLogEntry, DeleteLogEntry {
}
