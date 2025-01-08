package com.hieuboy.demo.kafka.consumer;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KafkaConsumer {

    static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumer.class);

    @KafkaListener(topics = "${spring.kafka.topic}", groupId = "${spring.kafka.group-id}")
    public void consumeMessage(@Header(KafkaHeaders.RECEIVED_PARTITION) int partition, byte[] message) {
        LOGGER.info("Received message from partition: {}", partition);
        // Add filtering or processing logic here
    }

}
