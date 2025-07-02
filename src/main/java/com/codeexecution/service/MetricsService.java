package com.codeexecution.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {
    
    private final MeterRegistry meterRegistry;
    
    // Counters
    private static final String SUBMISSIONS_TOTAL = "code_execution_submissions_total";
    private static final String SUBMISSIONS_FAILED = "code_execution_submissions_failed";
    private static final String TEST_CASES_TOTAL = "code_execution_test_cases_total";
    private static final String TEST_CASES_PASSED = "code_execution_test_cases_passed";
    
    // Timers
    private static final String EXECUTION_TIME = "code_execution_time_seconds";
    private static final String JUDGE0_API_TIME = "code_execution_judge0_api_time_seconds";
    
    public void recordSubmission(boolean success) {
        Counter.builder(SUBMISSIONS_TOTAL)
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
                
        if (!success) {
            Counter.builder(SUBMISSIONS_FAILED)
                    .register(meterRegistry)
                    .increment();
        }
    }
    
    public void recordTestCases(int total, int passed) {
        Counter.builder(TEST_CASES_TOTAL)
                .register(meterRegistry)
                .increment(total);
                
        Counter.builder(TEST_CASES_PASSED)
                .register(meterRegistry)
                .increment(passed);
    }
    
    public void recordApiCall(String endpoint, String method, int status, long durationMs) {
        Timer.builder("code_execution_api_calls")
                .tag("endpoint", endpoint)
                .tag("method", method)
                .tag("status", String.valueOf(status))
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    public void recordApiError(String endpoint, String method, Exception e) {
        Counter.builder("code_execution_api_errors")
                .tag("endpoint", endpoint)
                .tag("method", method)
                .tag("error", e.getClass().getSimpleName())
                .register(meterRegistry)
                .increment();
    }
    
    public void recordExecutionTime(String problemId, long durationMs, boolean success) {
        Timer.builder(EXECUTION_TIME)
                .tag("problem_id", problemId)
                .tag("success", String.valueOf(success))
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    // Deprecated methods for backward compatibility
    @Deprecated
    public void recordJudge0ApiError(String endpoint, String error) {
        Counter.builder("code_execution_judge0_api_errors")
                .tag("endpoint", endpoint)
                .tag("error", error)
                .register(meterRegistry)
                .increment();
    }
    
    @Deprecated
    public Timer.Sample startExecutionTimer() {
        return Timer.start(meterRegistry);
    }
    
    @Deprecated
    public void stopExecutionTimer(Timer.Sample sample, String problemId, boolean success) {
        if (sample != null) {
            sample.stop(Timer.builder(EXECUTION_TIME)
                    .tag("problem_id", problemId)
                    .tag("success", String.valueOf(success))
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry));
        }
    }
    
    @Deprecated
    public Timer.Sample startJudge0ApiTimer() {
        return Timer.start(meterRegistry);
    }
    
    @Deprecated
    public void stopJudge0ApiTimer(Timer.Sample sample, String endpoint) {
        if (sample != null) {
            sample.stop(Timer.builder(JUDGE0_API_TIME)
                    .tag("endpoint", endpoint)
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry));
        }
    }
}
