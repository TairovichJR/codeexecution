package com.codeexecution.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionRequest {
    @JsonProperty("source_code")
    private String sourceCode;

    @JsonProperty("language_id")
    private Integer languageId;

    @JsonProperty("stdin")
    private String stdin;

    @JsonProperty("expected_output")
    private String expectedOutput;

    @JsonProperty("cpu_time_limit")
    private Double cpuTimeLimit;

    @JsonProperty("cpu_extra_time")
    private Double cpuExtraTime;

    @JsonProperty("wall_time_limit")
    private Double wallTimeLimit;

    @JsonProperty("memory_limit")
    private Integer memoryLimit;

    @JsonProperty("stack_limit")
    private Integer stackLimit;

    @JsonProperty("max_processes_and_or_threads")
    private Integer maxProcessesAndOrThreads;

    @JsonProperty("enable_per_process_and_thread_time_limit")
    private Boolean enablePerProcessAndThreadTimeLimit;

    @JsonProperty("enable_per_process_and_thread_memory_limit")
    private Boolean enablePerProcessAndThreadMemoryLimit;

    @JsonProperty("max_file_size")
    private Integer maxFileSize;

    @JsonProperty("redirect_stderr_to_stdout")
    private Boolean redirectStderrToStdout;

    @JsonProperty("enable_network")
    private Boolean enableNetwork;

    @JsonProperty("number_of_runs")
    private Integer numberOfRuns;

    @JsonProperty("additional_files")
    private String additionalFiles;

    @JsonProperty("callback_url")
    private String callbackUrl;
}