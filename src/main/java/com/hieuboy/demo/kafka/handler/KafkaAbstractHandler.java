package com.hieuboy.demo.kafka.handler;

import com.hieuboy.demo.kafka.disruptor.DisruptorEvent;
import com.hieuboy.demo.kafka.dto.EventRequest;
import com.lmax.disruptor.EventHandler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class KafkaAbstractHandler<ZimJi>
        implements EventHandler<EventRequest<ZimJi>>, IHandler<EventRequest<ZimJi>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaAbstractHandler.class);

    @Override
    public String getType() {
        return "KafkaEvent";
    }

    @Override
    public String getRegex() {
        return ".*";
    }

    @Override
    public boolean check(String key) {
        return ObjectUtils.isNotEmpty(key);
    }

    @Override
    public Object process(DisruptorEvent<String, byte[]> event) {
        LOGGER.info("Processing event with DisruptorEvent: {}", event);

        if (ObjectUtils.isEmpty(event.getConsumerRecord().value())) {
            LOGGER.error("Received null value in event.");
            return null;
        }

        JsonObject payload = Buffer.buffer(event.getConsumerRecord().value()).toJsonObject();
        EventRequest<ZimJi> eventRequest = payload.mapTo(EventRequest.class);

        return process(eventRequest);
    }

    @Override
    public Object process(EventRequest<ZimJi> event) {
        LOGGER.info("Processing event with EventRequest: {}", event);
        return null;
    }

    @Override
    public void onEvent(EventRequest<ZimJi> event, long sequence, boolean endOfBatch) throws Exception {
        LOGGER.info("Processing event in onEvent method: {}", event);
        if (check(event.getEvent())) {
            // VD: su dung process() tu ICustomHandler de xu ly su kien
            process(event);
        }
    }

}