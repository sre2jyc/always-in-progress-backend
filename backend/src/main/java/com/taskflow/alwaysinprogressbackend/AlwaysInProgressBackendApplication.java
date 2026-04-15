package com.taskflow.alwaysinprogressbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AlwaysInProgressBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlwaysInProgressBackendApplication.class, args);
	}

}
