package com.codeexecution.service;

import com.codeexecution.config.Judge0Properties;
import com.codeexecution.model.SubmissionRequest;
import com.codeexecution.model.SubmissionResponse;
import com.codeexecution.model.SubmissionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@RateLimiter(name = "judge0RateLimiter")
public class Judge0Service {
    private static final int JAVA_LANGUAGE_ID = 62;

    private final RestTemplate restTemplate;
    private final Judge0Properties properties;
    private final ObjectMapper objectMapper;

    public SubmissionResponse submitSubmission(SubmissionRequest request) {
        return submitBatch(Collections.singletonList(request)).get(0);
    }

    @CircuitBreaker(name = "judge0CircuitBreaker", fallbackMethod = "fallbackHandler")
    public List<SubmissionResponse> submitBatch(List<SubmissionRequest> requests) {
        requests.forEach(this::validateSubmissionRequest);

        try {
            // Wrap requests in a map to match Judge0's expected format
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("submissions", requests);

            String requestJson = objectMapper.writeValueAsString(requestBody);
            log.debug("Submitting batch to Judge0: {}", redactSensitive(requestJson));

            String url = String.format("%s/submissions/batch?base64_encoded=%b",
                    properties.getBaseUrl(),
                    properties.isBase64Encoded());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            // Judge0 batch response is a list of maps like [{ token: "..." }, ...]
            return objectMapper.readValue(
                    response.getBody(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, SubmissionResponse.class)
            );
        } catch (Exception e) {
            log.error("Error submitting batch to Judge0: {}", e.getMessage(), e);
            throw new Judge0Exception("Failed to submit batch to Judge0: " + e.getMessage(), e);
        }
    }


    public SubmissionResult getSubmissionResult(String token) {
        try {
            String url = String.format("%s/submissions/%s?base64_encoded=%b",
                    properties.getBaseUrl(),
                    token,
                    properties.isBase64Encoded());

            ResponseEntity<SubmissionResult> response = restTemplate.getForEntity(
                    url, SubmissionResult.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting submission result for token {}: {}", token, e.getMessage(), e);
            throw new Judge0Exception("Failed to get submission result: " + e.getMessage(), e);
        }
    }

    public CompletableFuture<SubmissionResult> pollSubmissionResult(String token) {
        return CompletableFuture.supplyAsync(() -> {
            int attempts = 0;
            int maxAttempts = properties.getMaxPollingAttempts();
            long pollInterval = properties.getPollingIntervalMs();

            while (attempts < maxAttempts) {
                try {
                    SubmissionResult result = getSubmissionResult(token);
                    if (isProcessingComplete(result)) {
                        log.debug("Submission {} completed after {} attempts", token, attempts + 1);
                        return result;
                    }

                    if (attempts > 0 && attempts % 5 == 0) {
                        log.debug("Polling attempt {}/{} for token {}", attempts, maxAttempts, token);
                    }

                    TimeUnit.MILLISECONDS.sleep(pollInterval);
                    attempts++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Polling interrupted for token: {}", token, e);
                    throw new Judge0Exception("Polling interrupted for token: " + token, e);
                } catch (Exception e) {
                    log.error("Polling attempt {}/{} failed for token {}",
                            attempts, maxAttempts, token, e);
                    attempts++;

                    try {
                        long backoffTime = Math.min(
                            (long) (pollInterval * Math.pow(1.5, attempts / 5)),
                            10000L // Max 10 seconds
                        );
                        TimeUnit.MILLISECONDS.sleep(backoffTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new Judge0Exception("Backoff interrupted", ie);
                    }
                }
            }

            String errorMsg = String.format("Max polling attempts (%d) exceeded for token: %s",
                    maxAttempts, token);
            log.warn(errorMsg);
            throw new Judge0Exception(errorMsg);
        });
    }

    // Fallback method for circuit breaker
    public List<SubmissionResponse> fallbackHandler(List<SubmissionRequest> requests, Throwable t) {
        log.error("Judge0 service unavailable, using fallback", t);
        SubmissionResponse fallback = new SubmissionResponse();
        fallback.setToken("service-unavailable");
        fallback.setError("Judge0 service is temporarily unavailable");
        // Return a fallback response for each submission
        return Collections.nCopies(requests.size(), fallback);
    }


    private void validateSubmissionRequest(SubmissionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        if (request.getLanguageId() == null) {
            request.setLanguageId(JAVA_LANGUAGE_ID);
        }

        if (!StringUtils.hasText(request.getSourceCode())) {
            throw new IllegalArgumentException("source_code is required");
        }
    }

    private boolean isProcessingComplete(SubmissionResult result) {
        return result != null &&
                result.getStatus() != null &&
                result.getStatus().getId() != null &&
                result.getStatus().getId() > 2;
    }

    private String redactSensitive(String json) {
        final int MAX_LENGTH = 500;
        if (json.length() > MAX_LENGTH) {
            return json.substring(0, MAX_LENGTH) + "... [truncated]";
        }
        return json;
    }

    public static class Judge0Exception extends RuntimeException {
        public Judge0Exception(String message) {
            super(message);
        }

        public Judge0Exception(String message, Throwable cause) {
            super(message, cause);
        }
    }
}


