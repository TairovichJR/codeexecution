package com.codeexecution.service;

import com.codeexecution.model.SubmissionRequest;
import com.codeexecution.model.SubmissionResponse;
import com.codeexecution.model.SubmissionResult;
import com.codeexecution.model.TestCase;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeExecutionService {
    private final Judge0Service judge0Service;
    private final TestCaseLoaderService testCaseLoaderService;
    private final Executor taskExecutor;

    @Async("taskExecutor")
    public CompletableFuture<ExecutionResult> executeWithTestCases(String problemId, String sourceCode, boolean multiFile) {
        List<TestCase> testCases = testCaseLoaderService.loadTestCasesFromFiles(problemId);
        List<CompletableFuture<TestCaseResult>> futures = testCases.stream()
                .map(testCase -> processTestCase(problemId, sourceCode, testCase, multiFile))
                .collect(Collectors.toList());

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        return allOf.thenApply(v -> {
            List<TestCaseResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            return aggregateResults(results);
        });
    }

    private CompletableFuture<TestCaseResult> processTestCase(String problemId, String sourceCode, TestCase testCase, boolean multiFile) {
        return CompletableFuture.supplyAsync(() -> {
            SubmissionRequest request = createSubmissionRequest(sourceCode, testCase, multiFile);
            SubmissionResponse response = judge0Service.submitSubmission(request);
            SubmissionResult result = judge0Service.pollSubmissionResult(response.getToken());
            boolean passed = result.getStatus().getId() == 3; // Accepted
            return new TestCaseResult(testCase, result, passed);
        }, taskExecutor);
    }

    private SubmissionRequest createSubmissionRequest(String sourceCode, TestCase testCase, boolean multiFile) {
        SubmissionRequest request = new SubmissionRequest();
        if (multiFile) {
            request.setLanguageId(89); // Multi-file program
            request.setAdditionalFiles(createMultiFileZip(sourceCode));
        } else {
            request.setSourceCode(sourceCode);
            request.setLanguageId(62); // Java
        }
        request.setStdin(testCase.getInput());
        request.setExpectedOutput(testCase.getExpectedOutput());
        return request;
    }

    private String createMultiFileZip(String sourceCode) {
        Map<String, String> files = Map.of(
                "Main.java", sourceCode,
                "compile", "#!/bin/bash\njavac  Main.java",
                "run", "#!/bin/bash\njava Main"
        );
        return judge0Service.createBase64Zip(files);
    }

    private ExecutionResult aggregateResults(List<TestCaseResult> results) {
        boolean allPassed = results.stream().allMatch(TestCaseResult::isPassed);
        int passedCount = (int) results.stream().filter(TestCaseResult::isPassed).count();
        return new ExecutionResult(allPassed, passedCount, results.size(), results);
    }

    @RequiredArgsConstructor
    @Data
    public static class ExecutionResult {
        private final boolean overallPassed;
        private final int passedCount;
        private final int totalCount;
        private final List<TestCaseResult> testCaseResults;
    }

    @RequiredArgsConstructor
    @Data
    public static class TestCaseResult {
        private final TestCase testCase;
        private final SubmissionResult executionResult;
        private final boolean passed;
    }
}