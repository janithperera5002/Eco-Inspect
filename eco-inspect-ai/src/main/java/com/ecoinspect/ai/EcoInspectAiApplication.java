package com.ecoinspect.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;



@SpringBootApplication
public class EcoInspectAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcoInspectAiApplication.class, args);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

}
