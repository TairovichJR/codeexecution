package com.codeexecution.model;

import lombok.Data;

@Data
public class SubmissionResult {
    private String stdout;
    private String stderr;
    private String compileOutput;
    private String message;
    private Integer exitCode;
    private Integer exitSignal;
    private Status status;
    private String createdAt;
    private String finishedAt;
    private String token;
    private Double time;
    private Double wallTime;
    private Double memory;

    @Data
    public static class Status {
        private Integer id;
        private String description;
    }
}