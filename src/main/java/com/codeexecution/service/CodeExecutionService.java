package com.codeexecution.service;

import com.codeexecution.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeExecutionService {
    private static final int BATCH_SIZE = 20; // Adjust based on Judge0 rate limits

    private final Judge0Service judge0Service;
    private final TestCaseLoaderService testCaseLoaderService;
    private final Executor taskExecutor;
    private final MetricsService metricsService;

    @Cacheable(value = "testCases", key = "#problemId")
    public List<TestCase> getCachedTestCases(String problemId) {
        log.info("Loading test cases for problem: {}", problemId);
        return testCaseLoaderService.loadTestCasesFromFiles(problemId);
    }

    @Async("taskExecutor")
    public CompletableFuture<ExecutionResult> executeWithTestCases(String problemId, String sourceCode) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        CompletableFuture<List<TestCase>> testCasesFuture = CompletableFuture.supplyAsync(
            () -> getCachedTestCases(problemId),
            taskExecutor
        );

        return testCasesFuture.thenCompose(testCases -> {
            // Process test cases in batches
            List<List<TestCase>> batches = partitionList(testCases, BATCH_SIZE);

            List<CompletableFuture<List<TestCaseResult>>> batchFutures = batches.stream()
                    .map(batch -> processBatch(problemId, sourceCode, batch))
                    .toList();

            CompletableFuture<Void> allBatches = CompletableFuture.allOf(
                    batchFutures.toArray(new CompletableFuture[0])
            );

            return allBatches.thenApply(v -> {
                List<TestCaseResult> allResults = batchFutures.stream()
                        .flatMap(future -> future.join().stream())
                        .collect(Collectors.toList());

                ExecutionResult result = aggregateResults(allResults);

                // Record metrics
                if (metricsService != null) {
                    metricsService.recordTestCases(allResults.size(), result.getPassedCount());
                    metricsService.recordSubmission(result.isOverallPassed());
                    stopWatch.stop();
                    metricsService.recordExecutionTime(problemId, stopWatch.getTotalTimeMillis(), result.isOverallPassed());
                }

                return result;
            });
        });
    }

    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        return IntStream.range(0, (list.size() + batchSize - 1) / batchSize)
                .mapToObj(i -> list.subList(i * batchSize, Math.min((i + 1) * batchSize, list.size())))
                .collect(Collectors.toList());
    }

    private CompletableFuture<List<TestCaseResult>> processBatch(String problemId, String sourceCode,
                                                               List<TestCase> batch) {
        // Create submission requests for the batch
        List<SubmissionRequest> requests = batch.stream()
                .map(testCase -> createSubmissionRequest(sourceCode, testCase))
                .toList();

        return CompletableFuture.supplyAsync(() -> {
            String endpoint = "/submissions/batch?wait=true";
            long startTime = System.currentTimeMillis();

            try {
                // Submit batch to Judge0
                List<SubmissionResponse> responses = judge0Service.submitBatch(requests);
                if (metricsService != null) {
                    long duration = System.currentTimeMillis() - startTime;
                    metricsService.recordApiCall(endpoint, "POST", 200, duration);
                }

                // Process responses
                return IntStream.range(0, batch.size())
                        .mapToObj(i -> {
                            TestCase testCase = batch.get(i);
                            SubmissionResponse response = responses.get(i);

                            try {
                                // Poll for submission result
                                SubmissionResult result = judge0Service.pollSubmissionResult(response.getToken())
                                    .join(); // Wait for completion

                                boolean passed = result.getStatus().getId() == 3; // Accepted
                                return new TestCaseResult(testCase, result, passed);
                            } catch (Exception e) {
                                log.error("Error polling submission result for token: {}", response.getToken(), e);
                                return new TestCaseResult(testCase,
                                    SubmissionResult.builder()
                                        .status(new SubmissionResult.Status(99, "Error"))
                                        .build(),
                                    false
                                );
                            }
                        })
                        .collect(Collectors.toList());
            } catch (Exception e) {
                if (metricsService != null) {
                    metricsService.recordApiError(endpoint, "POST", e);
                }
                log.error("Error processing batch for problem: {}", problemId, e);
                throw new RuntimeException("Failed to process batch: " + e.getMessage(), e);
            }
        }, taskExecutor);
    }

    private SubmissionRequest createSubmissionRequest(String sourceCode, TestCase testCase) {
        SubmissionRequest request = new SubmissionRequest();
        request.setSourceCode(sourceCode);
        request.setLanguageId(62); // Java
        request.setStdin(testCase.getInput());
        request.setExpectedOutput(testCase.getExpectedOutput());
        return request;
    }

    private ExecutionResult aggregateResults(List<TestCaseResult> results) {
        if (results == null || results.isEmpty()) {
            return new ExecutionResult(false, 0, 0, List.of());
        }
        boolean allPassed = results.stream().allMatch(TestCaseResult::isPassed);
        int passedCount = (int) results.stream().filter(TestCaseResult::isPassed).count();
        return new ExecutionResult(allPassed, passedCount, results.size(), results);
    }
}