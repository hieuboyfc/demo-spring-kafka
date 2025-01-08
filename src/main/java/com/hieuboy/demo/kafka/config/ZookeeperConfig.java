package com.hieuboy.demo.kafka.config;

import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.*;

@Configuration
public class ZookeeperConfig {

    static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperConfig.class);

    @Value("${spring.zookeeper.connect-string}")
    private String zookeeperConnectString;

    @Value("${spring.zookeeper.session-timeout}")
    private int sessionTimeout;

    @Value("${spring.zookeeper.connection-timeout}")
    private int connectionTimeout;

    @Value("${spring.zookeeper.max-retries}")
    private int maxRetries;

    @Value("${spring.zookeeper.retry-interval}")
    private int retryInterval;

    @Bean
    public ZooKeeper zooKeeper() throws IOException, InterruptedException {
        int retryAttempts = 0;
        ZooKeeper zooKeeper = null;

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        while (retryAttempts < maxRetries) {
            try {
                final CountDownLatch latch = new CountDownLatch(1);
                zooKeeper = new ZooKeeper(zookeeperConnectString, sessionTimeout, event -> {
                    switch (event.getState()) {
                        case SyncConnected -> {
                            LOGGER.info("Zookeeper connected");
                            latch.countDown(); // Signal that ZooKeeper is connected
                        }
                        case Disconnected -> LOGGER.warn("Zookeeper disconnected");
                        case Expired -> LOGGER.error("Zookeeper session expired");
                        case AuthFailed -> LOGGER.error("Zookeeper authentication failed");
                        case SaslAuthenticated -> LOGGER.info("Zookeeper SASL authentication successful");
                        default -> LOGGER.info("Zookeeper state: " + event.getState());
                    }
                });

                // Wait for the connection to be established or for timeout
                if (!latch.await(connectionTimeout, TimeUnit.MILLISECONDS)) {
                    LOGGER.error("Zookeeper connection timed out");
                    throw new IOException("Zookeeper connection timed out");
                }

                return zooKeeper;
            } catch (IOException | InterruptedException e) {
                retryAttempts++;
                if (retryAttempts < maxRetries) {
                    LOGGER.warn("Retrying to connect to Zookeeper... Attempt " + retryAttempts);

                    // Instead of Thread.sleep, schedule the next retry
                    try {
                        executorService.schedule(() -> {
                        }, retryInterval, TimeUnit.MILLISECONDS).get();
                    } catch (ExecutionException | InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Error during retry scheduling", ex);
                    }
                } else {
                    LOGGER.error("Failed to connect to Zookeeper after " + maxRetries + " attempts", e);
                    throw new IOException("Failed to connect to Zookeeper after " + maxRetries + " attempts", e);
                }
            }
        }
        executorService.shutdown();
        return zooKeeper;
    }

}
