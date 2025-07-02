package com.codeexecution.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CodeExecutionRequest {

    @NotBlank(message = "Source code is required")
    @JsonProperty("source_code")
    private String sourceCode;
}