package dev.migwel.jaft.application;

import dev.migwel.jaft.server.ClusterInfo;
import dev.migwel.jaft.server.ServerInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

@Configuration
@PropertySource("classpath:systemconfiguration.properties")
public class SystemConfiguration {

    private final Environment env;

    public SystemConfiguration(Environment env) {
        this.env = env;
    }

    @Bean
    public ServerInfo serverInfo() {
        String serverIdStr = env.getProperty("currentServer.id");
        if (serverIdStr == null) {
            throw new ConfigurationException("Missing id for current server");
        }
        int serverId = Integer.parseInt(serverIdStr);
        return buildServerInfo(serverId);
    }

    @Bean
    public ClusterInfo clusterInfo() {
        List<ServerInfo> serversInfo = new ArrayList<>();
        int serverId = 0;
        while(env.getProperty("server."+ serverId +".name") != null) {
            ServerInfo serverInfo = buildServerInfo(serverId);
            serversInfo.add(serverInfo);
            serverId++;
        }
        return new ClusterInfo(serversInfo);
    }

    private ServerInfo buildServerInfo(int serverId) {
        String serverName = env.getProperty("server."+ serverId +".name");
        String serverUrl = env.getProperty("server."+ serverId +".url");
        String serverPort = env.getProperty("server."+ serverId +".port");
        if (serverName == null || serverUrl == null || serverPort == null) {
            throw new ConfigurationException("Configuration for server "+ serverId +" is wrong");
        }
        return new ServerInfo(serverName, serverUrl, serverPort);
    }
}
