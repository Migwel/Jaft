package dev.migwel.jaft.statemachine.log;

public final class DeleteLogEntry<K, V> implements LogEntry<K, V> {

    private final K key;

    public DeleteLogEntry(K key) {
        this.key = key;
    }

    public K getKey() {
        return key;
    }
}
