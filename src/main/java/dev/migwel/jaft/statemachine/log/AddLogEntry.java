package dev.migwel.jaft.statemachine.log;

public final class AddLogEntry<K, V> implements LogEntry<K, V> {

    private final K key;
    private final V value;

    public AddLogEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}
