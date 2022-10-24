package dev.migwel.jaft.application;

import dev.migwel.jaft.server.ClusterInfo;
import dev.migwel.jaft.server.Leadership;
import dev.migwel.jaft.server.ServerInfo;
import dev.migwel.jaft.server.ServerState;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication(scanBasePackages = "dev.migwel.jaft")
public class JaftApplication {

	public static void main(String[] args) {
		SpringApplication.run(JaftApplication.class, args);
	}

	@Bean
	public ThreadPoolTaskScheduler threadPoolTaskScheduler(){
		return new ThreadPoolTaskScheduler();
	}

	@Bean
	public ServerState serverState() {
		return new ServerState(0, Leadership.Follower);
	}

	//TODO: Move this to a properties file or command line argument or something
	@Bean
	public ServerInfo serverInfo() {
		return buildServerInfo("MyServerId-0", "8080");
	}

	//TODO: Move this to a properties file or something
	@Bean
	public ClusterInfo clusterInfo() {
		List<ServerInfo> serversInfo = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			ServerInfo serverInfo = buildServerInfo("MyServer-"+ i, "808"+ i);
			serversInfo.add(serverInfo);
		}
		return new ClusterInfo(serversInfo);
	}

	private ServerInfo buildServerInfo(String serverId, String port) {
		return new ServerInfo(serverId, "https://localhost", port);
	}

	@Bean
	public HttpClient httpClient() {
		return HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_2)
				.connectTimeout(Duration.of(50, ChronoUnit.MILLIS))
				.build();
	}

}
