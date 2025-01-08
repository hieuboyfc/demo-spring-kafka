package com.hieuboy.demo.kafka.handler.example;

import com.hieuboy.demo.kafka.dto.EventRequest;
import com.lmax.disruptor.EventHandler;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

public class TestListener extends BaseListener<Map<String, Object>> {

    @Value("${zimji.sync-handler.test-sync}")
    private String topic;

    @Override
    protected String getTopic() {
        return topic;
    }

    @Override
    protected EventHandler<EventRequest<Map<String, Object>>> createHandler() {
        return new TestHandler();
    }

}
