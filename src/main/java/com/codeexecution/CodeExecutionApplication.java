package com.codeexecution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class CodeExecutionApplication {
	public static void main(String[] args) {
		SpringApplication.run(CodeExecutionApplication.class, args);
	}
}