package com.hieuboy.demo.kafka.handler.example;

import com.hieuboy.demo.kafka.dto.EventRequest;
import com.hieuboy.demo.kafka.handler.KafkaAbstractHandler;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Service
public class TestHandler extends KafkaAbstractHandler<Map<String, Object>> {

    static Logger LOGGER = LoggerFactory.getLogger(TestHandler.class);

    @Override
    public String getType() {
        return "edit";
    }

    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public Object process(EventRequest<Map<String, Object>> record) {
        if (ObjectUtils.isEmpty(record)) {
            return null;
        }
        return null;
    }

}
