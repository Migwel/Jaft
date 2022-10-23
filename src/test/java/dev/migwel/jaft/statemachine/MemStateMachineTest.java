package dev.migwel.jaft.statemachine;

import dev.migwel.jaft.statemachine.log.AddLogEntry;
import dev.migwel.jaft.statemachine.log.DeleteLogEntry;
import dev.migwel.jaft.statemachine.log.LogEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemStateMachineTest {

    @Test
    void addEntry() {
        MemStateMachine stateMachine = new MemStateMachine();
        LogEntry<String, Long> addLogEntry = new AddLogEntry<>("myEntry", 1L);
        stateMachine.apply(addLogEntry);
        assertEquals(stateMachine.getValue("myEntry"), 1L);
    }

    @Test
    void updateEntry() {
        MemStateMachine stateMachine = new MemStateMachine();
        LogEntry<String, Long> addLogEntry = new AddLogEntry<>("myEntry", 1L);
        stateMachine.apply(addLogEntry);
        assertEquals(stateMachine.getValue("myEntry"), 1L);
        LogEntry<String, Long> updateLogEntry = new AddLogEntry<>("myEntry", 2L);
        stateMachine.apply(updateLogEntry);
        assertEquals(stateMachine.getValue("myEntry"), 2L);
    }

    @Test
    void deleteEntry() {
        MemStateMachine stateMachine = new MemStateMachine();
        LogEntry<String, Long> addLogEntry = new AddLogEntry<>("myEntry", 1L);
        stateMachine.apply(addLogEntry);
        LogEntry<String, Long> deleteLogEntry = new DeleteLogEntry<>("myEntry");
        stateMachine.apply(deleteLogEntry);
        assertNull(stateMachine.getValue("myEntry"));
    }

}