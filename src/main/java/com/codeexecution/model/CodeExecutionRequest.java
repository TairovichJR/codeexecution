package com.codeexecution.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CodeExecutionRequest {

    @JsonProperty("source_code")
    private String sourceCode;
    @JsonProperty("multifile")
    private boolean multiFile;
}