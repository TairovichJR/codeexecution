package com.codeexecution.model;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
@Builder
public class TestCaseResult {
    private final TestCase testCase;
    private final SubmissionResult executionResult;
    private final boolean passed;
}