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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
@RateLimiter(name = "judge0RateLimiter")
@CircuitBreaker(name = "judge0CircuitBreaker", fallbackMethod = "fallbackHandler")
public class Judge0Service {
    private static final int JAVA_LANGUAGE_ID = 62;
    private static final int MULTI_FILE_LANGUAGE_ID = 89;

    private final RestTemplate restTemplate;
    private final Judge0Properties properties;
    private final ObjectMapper objectMapper;

    public SubmissionResponse submitSubmission(SubmissionRequest request) {
        // Validate mandatory fields
        validateSubmissionRequest(request);

        // Apply default constraints
        applyDefaultConstraints(request);

        // Wrap Java code if needed
        if (JAVA_LANGUAGE_ID == request.getLanguageId()) {
            request.setSourceCode(wrapJavaCode(request.getSourceCode()));
        }

        try {
            // Serialize request
            String requestJson = objectMapper.writeValueAsString(request);
            log.debug("Submitting to Judge0: {}", redactSensitive(requestJson));

            // Build headers and entity
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            // Build URL
            String url = buildSubmissionUrl();

            // Execute request
            ResponseEntity<String> response = executeJudge0Request(url, entity);

            // Deserialize and return response
            return objectMapper.readValue(response.getBody(), SubmissionResponse.class);
        } catch (IOException e) {
            log.error("JSON processing error", e);
            throw new Judge0Exception("Failed to process JSON payload", e);
        }
    }

    public SubmissionResult getSubmissionResult(String token) {
        try {
            String url = properties.getBaseUrl() + "/submissions/" + token +
                    "?base64_encoded=" + properties.isBase64Encoded();

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to get submission: {} - {}", response.getStatusCode(), response.getBody());
                throw new Judge0Exception("Judge0 API error: " + response.getStatusCode());
            }

            return objectMapper.readValue(response.getBody(), SubmissionResult.class);
        } catch (IOException e) {
            log.error("JSON deserialization error for token: {}", token, e);
            throw new Judge0Exception("Failed to parse submission result", e);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Judge0 API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new Judge0Exception("Judge0 API error: " + e.getStatusCode(), e);
        }
    }

    public SubmissionResult pollSubmissionResult(String token) {
        int attempts = 0;
        while (attempts < properties.getMaxPollingAttempts()) {
            try {
                SubmissionResult result = getSubmissionResult(token);
                if (isProcessingComplete(result)) {
                    return result;
                }
                TimeUnit.MILLISECONDS.sleep(properties.getPollingIntervalMs());
                attempts++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Judge0Exception("Polling interrupted", e);
            } catch (Exception e) {
                log.error("Polling attempt {} failed for token {}", attempts, token, e);
                attempts++;
            }
        }
        throw new Judge0Exception("Max polling attempts exceeded for token: " + token);
    }

    public String createBase64Zip(Map<String, String> files) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (Map.Entry<String, String> entry : files.entrySet()) {
                addFileToZip(zos, entry.getKey(), entry.getValue());
            }
            zos.finish();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new Judge0Exception("Failed to create zip file", e);
        }
    }

    // Fallback method for circuit breaker
    public SubmissionResponse fallbackHandler(SubmissionRequest request, Throwable t) {
        log.error("Judge0 service unavailable, using fallback", t);
        SubmissionResponse response = new SubmissionResponse();
        response.setToken("service-unavailable");
        response.setError("Judge0 service is temporarily unavailable");
        return response;
    }

    private String wrapJavaCode(String userClassCode) {
        return """
            import java.util.*;
            import java.util.stream.*;
            
            public class Main {
                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    
                    // Read input count
                    int n = scanner.nextInt();
                    scanner.nextLine();  // Consume newline
                    
                    // Read main input
                    List<Integer> arr = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        arr.add(scanner.nextInt());
                    }
                    
                    // Execute solution
                    int result = Solution.migratoryBirds(arr);
                    System.out.println(result);
                }
            }
            """ + userClassCode;
    }

    private void validateSubmissionRequest(SubmissionRequest request) {
        if (request.getLanguageId() == null) {
            throw new IllegalArgumentException("language_id is required");
        }

        if (request.getLanguageId() == MULTI_FILE_LANGUAGE_ID) {
            if (!StringUtils.hasText(request.getAdditionalFiles())) {
                throw new IllegalArgumentException("additional_files is required for multi-file submissions");
            }
        } else {
            if (!StringUtils.hasText(request.getSourceCode())) {
                throw new IllegalArgumentException("source_code is required");
            }
        }
    }

    private void applyDefaultConstraints(SubmissionRequest request) {
        if (request.getCpuTimeLimit() == null) {
            request.setCpuTimeLimit(properties.getDefaultCpuTimeLimit());
        }
        if (request.getMemoryLimit() == null) {
            request.setMemoryLimit(properties.getDefaultMemoryLimit());
        }
    }

    private String buildSubmissionUrl() {
        return properties.getBaseUrl() + "/submissions?base64_encoded=" +
                properties.isBase64Encoded() + "&wait=false";
    }

    private ResponseEntity<String> executeJudge0Request(String url, HttpEntity<String> entity) {
        try {
//            ResponseEntity<String> response = restTemplate.exchange(
//                    url, HttpMethod.POST, entity, String.class
//            );
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);


            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Judge0 API error: {} - {}", response.getStatusCode(), response.getBody());
                throw new Judge0Exception("Judge0 API returned: " + response.getStatusCode());
            }
            return response;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Judge0 API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new Judge0Exception("Judge0 API error: " + e.getStatusCode(), e);
        }
    }

    private boolean isProcessingComplete(SubmissionResult result) {
        return result != null &&
                result.getStatus() != null &&
                result.getStatus().getId() != null &&
                result.getStatus().getId() > 2;  // Status > 2 means not queued/processing
    }

    private void addFileToZip(ZipOutputStream zos, String fileName, String content) throws IOException {
        ZipEntry zipEntry = new ZipEntry(fileName);
        zos.putNextEntry(zipEntry);
        zos.write(content.getBytes());
        zos.closeEntry();
    }

    private String redactSensitive(String json) {
        final int MAX_LENGTH = 500;
        if (json.length() > MAX_LENGTH) {
            return json.substring(0, MAX_LENGTH) + "... [REDACTED]";
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