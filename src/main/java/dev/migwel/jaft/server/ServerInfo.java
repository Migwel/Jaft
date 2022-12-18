package dev.migwel.jaft.server;

import javax.annotation.ParametersAreNonnullByDefault;
import java.net.URI;

@ParametersAreNonnullByDefault
public record ServerInfo(String serverId, String serverUrl, String serverPort) {

    public URI getURI() {
        return URI.create(serverUrl +":"+ serverPort);
    }

    public URI getURI(String endPoint) {
        return URI.create(serverUrl +":"+ serverPort + endPoint);
    }
}
