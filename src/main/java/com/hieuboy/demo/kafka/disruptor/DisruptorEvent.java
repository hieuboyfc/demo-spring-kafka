package com.hieuboy.demo.kafka.disruptor;

import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Setter
@Getter
public class DisruptorEvent<K, V> {

    private ConsumerRecord<K, V> consumerRecord;

}
