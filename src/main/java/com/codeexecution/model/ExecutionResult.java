package com.codeexecution.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import java.util.List;

@RequiredArgsConstructor
@Data
public class ExecutionResult {
    private final boolean overallPassed;
    private final int passedCount;
    private final int totalCount;
    private final List<TestCaseResult> testCaseResults;
}