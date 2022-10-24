package dev.migwel.jaft.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@SpringBootApplication
public class JaftApplication {

	public static void main(String[] args) {
		SpringApplication.run(JaftApplication.class, args);
	}

	@Bean
	public ThreadPoolTaskScheduler threadPoolTaskScheduler(){
		return new ThreadPoolTaskScheduler();
	}

}
