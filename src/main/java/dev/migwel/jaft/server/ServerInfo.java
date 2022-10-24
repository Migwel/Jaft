package dev.migwel.jaft.server;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public record ServerInfo(String serverId, String serverUrl, String serverPort) {}
