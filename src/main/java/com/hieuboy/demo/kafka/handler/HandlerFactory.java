/*
package com.hieuboy.demo.kafka.handler;

import com.hieuboy.demo.kafka.disruptor.DisruptorEvent;
import com.hieuboy.demo.kafka.dto.EventRequest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class HandlerFactory<ZimJi> extends AbstractHandlerFactory<ZimJi> {

    static final Logger LOGGER = LoggerFactory.getLogger(HandlerFactory.class);

    static final String EVENT_KEY = "event";

    @SafeVarargs
    public HandlerFactory(IHandler<ZimJi>... handler) {
        super(handler);
    }

    public HandlerFactory(List<IHandler<ZimJi>> handler) {
        super(handler);
    }

    @Override
    public void onEvent(DisruptorEvent<String, byte[]> event, long l, boolean b) throws Exception {
        try {
            if (ObjectUtils.isEmpty(mapper)) {
                return;
            }

            // Lay ten su kien tu headers hoac payload
            String eventName = extractEventName(event);

            // Xau dung keys can thiet de xu ly
            Set<String> keys = buildKeys(event, eventName);

            // Xu ly cac handler trong mapper
            if (mapper.size() == 1) {
                processHandlersInParallel(event, mapper.values().stream().flatMap(List::stream));
            } else {
                processHandlersByKey(event, keys);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing event", e);
        }
    }

    private String extractEventName(DisruptorEvent<String, byte[]> event) {
        for (Header header : event.getConsumerRecord().headers()) {
            if (header.key().equalsIgnoreCase(EVENT_KEY)) {
                return new String(header.value(), StandardCharsets.UTF_8);
            }
        }

        // Neu khong tim thay event trong header, lay tu payload
        Buffer record = Buffer.buffer(event.getConsumerRecord().value());
        EventRequest eventRequest = Json.decodeValue(record, EventRequest.class);
        return eventRequest.getEvent();
    }

    private Set<String> buildKeys(DisruptorEvent<String, byte[]> event, String eventName) {
        Set<String> keys = new HashSet<>();
        keys.add(event.getConsumerRecord().topic());
        if (!eventName.isEmpty()) {
            keys.add(eventName);
        }
        return keys;
    }

    private void processHandlersInParallel(DisruptorEvent<String, byte[]> event,
                                           Stream<IHandler<ZimJi>> handlersStream) {
        handlersStream.parallel().forEach(handler -> handler.process(event));
    }

    private void processHandlersByKey(DisruptorEvent<String, byte[]> event, Set<String> keys) {
        keys.parallelStream().forEach(key -> {
            List<IHandler<ZimJi>> handlers = mapper.getOrDefault(key, new ArrayList<>());
            handlers.parallelStream().forEach(handler -> {
                if (handler.check(key)) {
                    handler.process(event);
                }
            });
        });
    }

}
*/
