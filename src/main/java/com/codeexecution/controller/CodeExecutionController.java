package com.codeexecution.controller;

import com.codeexecution.model.TestCase;
import com.codeexecution.service.CodeExecutionService;
import com.codeexecution.service.TestCaseLoaderService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @PostMapping("/execute/{problemId}")
    public CompletableFuture<ResponseEntity<CodeExecutionService.ExecutionResult>> executeCode(
            @PathVariable String problemId,
            @RequestBody @Valid CodeExecutionRequest request
    ) {
        return executionService.executeWithTestCases(problemId, request.getSourceCode(), request.isMultiFile())
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/testcases/{problemId}")
    public ResponseEntity<List<TestCase>> getTestCases(@PathVariable String problemId) {
        List<TestCase> testCases = testCaseLoaderService.loadTestCasesFromFiles(problemId);
        return ResponseEntity.ok(testCases);
    }

    @Data
    public static class CodeExecutionRequest {
        private String sourceCode;
        private boolean multiFile;
    }
}