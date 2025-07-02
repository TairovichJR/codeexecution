package com.codeexecution.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a code submission request to Judge0.
 * All fields are optional except source_code and language_id.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionRequest {
    
    /**
     * Source code to be executed
     */
    @NotBlank(message = "Source code is required")
    @JsonProperty("source_code")
    private String sourceCode;

    /**
     * Language ID (defaults to Java 11)
     */
    @Builder.Default
    @JsonProperty("language_id")
    private Integer languageId = 62; // Default to Java 11

    /**
     * Standard input for the program
     */
    @JsonProperty("stdin")
    private String stdin;

    /**
     * Expected output for the test case (used for checking correctness)
     */
    @JsonProperty("expected_output")
    private String expectedOutput;

    /**
     * CPU time limit in seconds
     */
    @JsonProperty("cpu_time_limit")
    private Double cpuTimeLimit;

    /**
     * Memory limit in KB
     */
    @JsonProperty("memory_limit")
    private Integer memoryLimit;

    /**
     * Whether to wait for execution to complete (synchronous mode)
     */
    @Builder.Default
    @JsonProperty("wait")
    private Boolean wait = true;

    /**
     * Whether the source code is base64 encoded
     */
    @Builder.Default
    @JsonProperty("base64_encoded")
    private Boolean base64Encoded = false;

    /**
     * Whether to redirect stderr to stdout
     */
    @Builder.Default
    @JsonProperty("redirect_stderr_to_stdout")
    private Boolean redirectStderrToStdout = true;

    /**
     * Callback URL for asynchronous execution
     */
    @JsonProperty("callback_url")
    private String callbackUrl;

    /**
     * Number of runs
     */
    @JsonProperty("number_of_runs")
    private Integer numberOfRuns;

    /**
     * Additional files
     */
    @JsonProperty("additional_files")
    private String additionalFiles;
}