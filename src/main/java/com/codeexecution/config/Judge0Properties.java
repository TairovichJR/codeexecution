package com.codeexecution.config;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Validated
@Configuration
@ConfigurationProperties(prefix = "judge0")
@Getter @Setter
public class Judge0Properties {

    @NotBlank
    private String baseUrl = "http://localhost:2358";

    private boolean base64Encoded = false;

    @DecimalMin("0.5")
    @DecimalMax("5.0")
    private double defaultCpuTimeLimit = 2.0;

    @Min(128000)
    @Max(512000)
    private int defaultMemoryLimit = 256000;

    @Min(500)
    @Max(5000)
    private int pollingIntervalMs = 1000;

    @Min(5)
    @Max(100)
    private int maxPollingAttempts = 30;
}