package com.codeexecution.service;

import com.codeexecution.model.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
public class TestCaseLoaderService {

    public List<TestCase> loadTestCasesFromFiles(String problemId) {
        Path baseDir = Paths.get("problems", problemId);
        List<TestCase> testCases = new ArrayList<>();

        try (Stream<Path> inputFiles = Files.list(baseDir.resolve("input"))) {
            inputFiles.sorted().forEach(inputPath -> {
                try {
                    String input = Files.readString(inputPath);
                    String fileName = inputPath.getFileName().toString();
                    Path outputPath = baseDir.resolve("output").resolve(fileName.replace("input", "output"));
                    if (Files.exists(outputPath)) {
                        String output = Files.readString(outputPath);
                        testCases.add(new TestCase(input, output));
                    } else {
                        log.error("Output file not found for input: {}", fileName);
                    }
                } catch (IOException e) {
                    log.error("Error reading test case file: {}", inputPath, e);
                }
            });
        } catch (IOException e) {
            log.error("Error accessing test case directory for problem: {}", problemId, e);
            throw new TestCaseLoadException("Error loading test cases for problem: " + problemId, e);
        }

        if (testCases.isEmpty()) {
            throw new TestCaseLoadException("No test cases found for problem: " + problemId);
        }
        return testCases;
    }

    public static class TestCaseLoadException extends RuntimeException {
        public TestCaseLoadException(String message) {
            super(message);
        }

        public TestCaseLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}