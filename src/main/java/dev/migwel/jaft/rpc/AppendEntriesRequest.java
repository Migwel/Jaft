package dev.migwel.jaft.rpc;

import dev.migwel.jaft.statemachine.log.LogEntry;

import java.util.List;

public record AppendEntriesRequest(long term, String leaderId, long prevLogIndex, List<LogEntry<?,?>> entries, long leaderCommit) {}
