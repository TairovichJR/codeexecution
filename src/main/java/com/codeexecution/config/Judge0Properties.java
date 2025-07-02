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
    
    private boolean wait = true;  // Whether to wait for execution to complete

    @DecimalMin("0.5")
    @DecimalMax("5.0")
    private double defaultCpuTimeLimit = 2.0;

    @Min(128000)
    @Max(512000)
    private int defaultMemoryLimit = 256000;

    @Min(100)
    @Max(5000)
    private int pollingIntervalMs = 500;  // Reduced default polling interval

    @Min(10)
    @Max(200)
    private int maxPollingAttempts = 60;  // Increased default max attempts
    
    @Min(1)
    @Max(20)
    private int batchSize = 5;  // Default batch size for submissions
    
    // Timeout settings in milliseconds
    @Min(1000)
    private int connectionTimeout = 5000;
    
    @Min(5000)
    private int readTimeout = 30000;
}