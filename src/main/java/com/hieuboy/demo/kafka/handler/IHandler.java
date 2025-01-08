package com.hieuboy.demo.kafka.handler;

import com.hieuboy.demo.kafka.disruptor.DisruptorEvent;

public interface IHandler<ZimJi> {

    String getType();

    String getRegex();

    boolean check(String key);

    Object process(DisruptorEvent<String, byte[]> event);

    Object process(ZimJi event);

}
