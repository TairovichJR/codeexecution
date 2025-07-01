package com.codeexecution.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class TestCaseResult {
    private final TestCase testCase;
    private final SubmissionResult executionResult;
    private final boolean passed;
}