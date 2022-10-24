package dev.migwel.jaft.rpc;

import dev.migwel.jaft.server.Leadership;

public record InfoResponse(Leadership leadership, long currentTerm, String currentLeader) {}
