package dev.migwel.jaft.rpc;

public record AppendEntriesResponse(long term, boolean success) {}
