package com.hieuboy.demo.kafka.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private final ExecutorService virtualThreadExecutor;

    public AsyncConfig(ExecutorService virtualThreadExecutor) {
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    @Override
    public Executor getAsyncExecutor() {
        // Tra ve ExecutorService cho cac tac vu @Async
        return virtualThreadExecutor;
    }

}
