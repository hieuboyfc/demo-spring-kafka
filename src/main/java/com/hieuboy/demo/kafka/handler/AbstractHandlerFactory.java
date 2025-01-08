/*
package com.hieuboy.demo.kafka.handler;

import com.hieuboy.demo.kafka.disruptor.DisruptorEvent;
import com.lmax.disruptor.EventHandler;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@FieldDefaults(level = AccessLevel.PROTECTED)
public abstract class AbstractHandlerFactory<ZimJi> implements EventHandler<DisruptorEvent<String, byte[]>> {

    final Map<String, List<IHandler<ZimJi>>> mapper = new ConcurrentHashMap<>();
    final List<IHandler<ZimJi>> handlers = new ArrayList<>();

    @SafeVarargs
    public AbstractHandlerFactory(IHandler<ZimJi>... handlers) {
        addHandlers(handlers);
    }

    public AbstractHandlerFactory(List<IHandler<ZimJi>> handlers) {
        addHandlers(handlers);
    }

    public void addHandlers(List<IHandler<ZimJi>> handlers) {
        handlers.forEach(handler ->
                mapper.computeIfAbsent(handler.getType(), key -> new ArrayList<>()).add(handler)
        );
        this.handlers.addAll(handlers);
    }

    public void mergeHandlers(List<IHandler<ZimJi>> handlers) {
        this.handlers.clear();  // Clear current handlers
        this.handlers.addAll(handlers);

        // Clear and rebuild the mapper
        mapper.clear();
        handlers.forEach(handler ->
                mapper.computeIfAbsent(handler.getType(), key -> new ArrayList<>()).add(handler)
        );
    }

    @SafeVarargs
    public final void addHandlers(IHandler<ZimJi>... handlers) {
        addHandlers(Arrays.asList(handlers));
    }

}
*/
