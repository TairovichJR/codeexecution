package com.codeexecution.listener;

import com.codeexecution.config.Judge0Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationReadyListener {

    private final Judge0Properties judge0Properties;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("\n" +
                "=========================================================\n" +
                "  Code Execution Service is ready!\n" +
                "  Judge0 URL: {}\n" +
                "  Batch Size: {}\n" +
                "  Max Polling Attempts: {}\n" +
                "  Polling Interval: {}ms\n" +
                "  Connection Timeout: {}ms\n" +
                "  Read Timeout: {}ms\n" +
                "=========================================================",
                judge0Properties.getBaseUrl(),
                judge0Properties.getBatchSize(),
                judge0Properties.getMaxPollingAttempts(),
                judge0Properties.getPollingIntervalMs(),
                judge0Properties.getConnectionTimeout(),
                judge0Properties.getReadTimeout());
    }
}
