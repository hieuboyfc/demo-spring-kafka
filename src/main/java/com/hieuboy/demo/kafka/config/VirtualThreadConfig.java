package com.hieuboy.demo.kafka.config;

import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
public class VirtualThreadConfig {

    @Bean
    public ExecutorService virtualThreadExecutor() {
        return Executors.newThreadPerTaskExecutor(new ThreadFactory() {
            private int threadCount = 0;

            @Override
            public Thread newThread(@NonNull Runnable r) {
                // Tao 1 virtual thread moi voi ten theo dinh dang "VirtualThread-<threadCount>"
                Thread thread = new Thread(r);
                thread.setName("ZimJi-VirtualThread-" + (++threadCount)); // Dat ten cho luong ao
                return thread;
            }
        });
    }

}
