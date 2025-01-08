package com.hieuboy.demo.kafka.handler.example;

import com.hieuboy.demo.kafka.disruptor.KafkaDisruptor;
import com.hieuboy.demo.kafka.dto.EventRequest;
import com.lmax.disruptor.EventHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;

public abstract class BaseListener<ZimJi> {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.group-id}")
    private String groupId;

    protected abstract String getTopic();

    protected abstract EventHandler<EventRequest<ZimJi>> createHandler();

    @PostConstruct
    public void load() {
        String topic = getTopic();
        KafkaDisruptor.build(topic, bootstrapServers, groupId, createHandler());
    }

}