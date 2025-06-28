package com.codeexecution.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import java.lang.reflect.Method;
import java.util.Arrays;

public class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        String message = String.format(
                "Async method '%s' failed with parameters: %s. Error: %s",
                method.getName(),
                Arrays.toString(params),
                ex.getMessage()
        );
        // In production, use a proper logger
        System.err.println(message);
        ex.printStackTrace();
    }
}