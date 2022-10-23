package dev.migwel.jaft.server;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public record ClusterInfo(List<ServerInfo> serversInfo){}
