package com.hieuboy.demo.kafka.producer;

import com.hieuboy.demo.kafka.dto.EventRequest;
import io.vertx.core.json.JsonObject;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyMessageFuture;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KafkaProducer {

    static final Logger LOGGER = LoggerFactory.getLogger(KafkaProducer.class);

    @Value("${zimji.application}")
    String application;

    static final String EVENT_KEY = "event";
    static final String EVENT_DEFAULT = "event";

    final KafkaTemplate<String, byte[]> kafkaTemplate;

    final Set<String> serviceTopics = Collections.synchronizedSet(new HashSet<>());

    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, Long companyId, Object payload, String event, String key, Map<String, Object> headers) {
        validateInputs(topic, payload);

        EventRequest<Object> eventRequest = createEventRequest(companyId, payload, event);
        byte[] message = JsonObject.mapFrom(eventRequest).toBuffer().getBytes();

        sendMessage(topic, message, event, key, headers);

        // Register service topic asynchronously
        if (serviceTopics.add(topic)) {
            registerServiceTopic(topic);
        }
    }

    public void publish(String topic, Long companyId, Object payload) {
        publish(topic, companyId, payload, EVENT_DEFAULT, createUUIDKey(), null);
    }

    public CompletableFuture<RequestReplyMessageFuture<String, byte[]>> publishAndReceive(
            String topic, Long companyId, EventRequest<?> eventRequest,
            ReplyingKafkaTemplate<String, byte[], byte[]> replyingKafkaTemplate
    ) throws Exception {

        validateInputs(topic, eventRequest);

        eventRequest.setCompanyId(companyId);

        String responseTopic = Optional.ofNullable(eventRequest.getResponseTopic())
                .filter(rt -> !rt.trim().isEmpty())
                .orElseThrow(() -> new Exception("responseTopic is required"));

        byte[] message = JsonObject.mapFrom(eventRequest).toBuffer().getBytes();

        return CompletableFuture.supplyAsync(() -> {
            try {
                Message<byte[]> kafkaMessage = createMessage(topic, message, eventRequest.getEvent(), createUUIDKey(), responseTopic, null);
                return replyingKafkaTemplate.sendAndReceive(kafkaMessage);
            } catch (Exception e) {
                LOGGER.error("Error during publishAndReceive: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    private void sendMessage(String topic, byte[] message, String event, String key, Map<String, Object> headers) {
        try {
            Message<byte[]> kafkaMessage = createMessage(topic, message, event, key, null, headers);
            kafkaTemplate.send(kafkaMessage);
        } catch (Exception e) {
            LOGGER.error("Failed to publish message to topic {}: {}", topic, e.getMessage(), e);
            throw new RuntimeException("Failed to publish message", e);
        }
    }

    @Async
    public void sendMessage(String topic, String key, byte[] value) {
        CompletableFuture<SendResult<String, byte[]>> future = CompletableFuture.supplyAsync(() -> {
            try {
                return kafkaTemplate.send(topic, key, value).get();  // Block and get result
            } catch (Exception e) {
                throw new RuntimeException("Message sending failed", e);
            }
        });

        future.thenAccept(result -> {
            LOGGER.info("Message sent to partition: {}", result.getRecordMetadata().partition());
        }).exceptionally(e -> {
            LOGGER.error("Message send failed: {}", e.getMessage());
            return null;
        });
    }

    public void sendMessageToKafka(String topic, String key, byte[] value) {
        kafkaTemplate.send(topic, key, value)
                .thenAccept(result -> {
                    LOGGER.info("Message sent successfully to partition: {}", result.getRecordMetadata().partition());
                })
                .exceptionally(e -> {
                    LOGGER.error("Message send failed: {}", e.getMessage());
                    return null;
                });
    }

    private Message<byte[]> createMessage(String topic, byte[] message, String event, String key,
                                          String responseTopic, Map<String, Object> headers) {
        MessageBuilder<byte[]> messageBuilder = MessageBuilder.withPayload(message)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeaderIfAbsent(KafkaHeaders.KEY, ObjectUtils.isNotEmpty(key) ? key : createUUIDKey())
                .setHeaderIfAbsent(EVENT_KEY, ObjectUtils.isNotEmpty(event) ? event : EVENT_DEFAULT);

        if (ObjectUtils.isNotEmpty(responseTopic)) {
            messageBuilder.setHeader(KafkaHeaders.REPLY_TOPIC, responseTopic);
        }

        if (ObjectUtils.isNotEmpty(headers)) {
            headers.forEach(messageBuilder::setHeader);
        }

        return messageBuilder.build();
    }

    private EventRequest<Object> createEventRequest(Long companyId, Object payload, String event) {
        EventRequest<Object> eventRequest = new EventRequest<>();
        eventRequest.setCompanyId(companyId);
        eventRequest.setPayload(payload);
        eventRequest.setEvent(Optional.ofNullable(event).orElse(EVENT_DEFAULT));
        return eventRequest;
    }

    private void registerServiceTopic(String topic) {
        CompletableFuture.runAsync(() -> {
            try {
                EventRequest<Map<String, Object>> eventRequest = new EventRequest<>();
                Map<String, Object> payload = Map.of("service", application, "topic", topic);
                eventRequest.setPayload(payload);
                eventRequest.setCompanyId(0L);
                eventRequest.setEvent("register-service-topic");
                publish("zimji-service", 0L, eventRequest);
            } catch (Exception e) {
                LOGGER.warn("Failed to register service topic {}: {}", topic, e.getMessage(), e);
            }
        });
    }

    private void validateInputs(String topic, Object payload) {
        if (ObjectUtils.isEmpty(topic)) {
            throw new IllegalArgumentException("Topic cannot be null or empty");
        }
        if (ObjectUtils.isEmpty(payload)) {
            throw new IllegalArgumentException("Payload cannot be null");
        }
    }

    public String createUUIDKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
