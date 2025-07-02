package com.codeexecution.controller;

import com.codeexecution.model.CodeExecutionRequest;
import com.codeexecution.model.ExecutionResult;
import com.codeexecution.model.TestCase;
import com.codeexecution.service.CodeExecutionService;
import com.codeexecution.service.MetricsService;
import com.codeexecution.service.TestCaseLoaderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/code")
@RequiredArgsConstructor
public class CodeExecutionController {
    private final CodeExecutionService executionService;
    private final TestCaseLoaderService testCaseLoaderService;
    private final MetricsService metricsService;


    @PostMapping(value = "/execute/{problemId}", 
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<ExecutionResult>> executeCode(
            @PathVariable String problemId,
            @Valid @RequestBody CodeExecutionRequest request) {
        
        log.info("Received code execution request for problem: {}", problemId);
        
        return executionService.executeWithTestCases(problemId, request.getSourceCode())
                .thenApply(result -> {
                    log.info("Code execution completed for problem: {}, passed: {}/{}", 
                            problemId, result.getPassedCount(), result.getTotalCount());
                    return ResponseEntity.ok(result);
                })
                .exceptionally(ex -> {
                    log.error("Error executing code for problem: {}", problemId, ex);
                    throw new RuntimeException("Failed to execute code: " + ex.getMessage(), ex);
                });
    }

    @GetMapping(value = "/test-cases/{problemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TestCase>> getTestCases(@PathVariable String problemId) {
        log.info("Retrieving test cases for problem: {}", problemId);
        List<TestCase> testCases = testCaseLoaderService.loadTestCasesFromFiles(problemId);
        return ResponseEntity.ok(testCases);
    }
}