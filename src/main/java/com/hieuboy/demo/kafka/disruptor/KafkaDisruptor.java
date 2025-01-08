package com.hieuboy.demo.kafka.disruptor;

import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.ser.std.ByteArraySerializer;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;
import com.hieuboy.demo.kafka.dto.EventRequest;
import com.hieuboy.demo.kafka.handler.KafkaAbstractHandler;
import com.hieuboy.demo.kafka.partition.CustomPartitioner;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KafkaDisruptor<ZimJi> {

    static final Logger LOGGER = LoggerFactory.getLogger(KafkaDisruptor.class);
    static final int BUFFER_SIZE_DEFAULT = 2048;

    Disruptor<EventRequest<ZimJi>> disruptor;
    RingBuffer<EventRequest<ZimJi>> ringBuffer;

    KafkaConsumer<String, byte[]> consumer;
    KafkaProducer<String, byte[]> producer;
    ExecutorService kafkaListenerExecutor;
    ConcurrentMessageListenerContainer<String, byte[]> messageListenerContainer;

    private KafkaDisruptor() {
    }

    @SafeVarargs
    public static <ZimJi> void build(String bootstrapServers,
                                     String groupId,
                                     String topic,
                                     EventHandler<EventRequest<ZimJi>>... handlers) {
        KafkaDisruptor<ZimJi> instance = new KafkaDisruptor<>();

        instance.initializeDisruptor(handlers);
        instance.initializeKafkaComponents(bootstrapServers, groupId);
        instance.initializeMessageListenerContainer(bootstrapServers, groupId, topic);
        instance.initializeKafkaListenerExecutor(topic);
    }

    /**
     * Khoi tao Disruptor
     */
    @SafeVarargs
    private void initializeDisruptor(EventHandler<EventRequest<ZimJi>>... handlers) {
        ThreadFactory virtualThreadFactory = Executors.defaultThreadFactory();
        disruptor = new Disruptor<>(EventRequest::new, BUFFER_SIZE_DEFAULT, virtualThreadFactory, ProducerType.SINGLE, new YieldingWaitStrategy());
        if (ObjectUtils.isNotEmpty(handlers)) {
            disruptor.handleEventsWith(handlers);
        } else {
            disruptor.handleEventsWith(new KafkaAbstractHandler<>());
        }
        disruptor.start();
        ringBuffer = disruptor.getRingBuffer();
    }

    /**
     * Tao va khoi tao thanh phan Kafka
     */
    private void initializeKafkaComponents(String bootstrapServers, String groupId) {
        // Tao va khoi tao Kafka Consumer voi cau hinh da cung cap
        consumer = new KafkaConsumer<>(createConsumerProps(bootstrapServers, groupId));

        // Tao va khoi tao Kafka Producer voi cau hinh da cung cap
        producer = new KafkaProducer<>(createProducerProps(bootstrapServers));
    }

    private void initializeMessageListenerContainer(String bootstrapServers, String groupId, String topic) {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = createListenerFactory(bootstrapServers, groupId);
        messageListenerContainer = factory.createContainer(topic);
        // Su dung Virtual Threads de xu ly cac message song song
        messageListenerContainer.setupMessageListener((MessageListener<String, byte[]>) this::handleMessage);
        messageListenerContainer.start();
    }

    /**
     * Khoi tao Disruptor voi ThreadFactory tao Virtual Threads
     */
    private void initializeKafkaListenerExecutor(String topic) {
        kafkaListenerExecutor = Executors.newSingleThreadExecutor();
        kafkaListenerExecutor.submit(() -> consumeMessages(topic));
    }

    /**
     * Cau hinh ConcurrentMessageListenerContainer
     */
    private ConcurrentKafkaListenerContainerFactory<String, byte[]> createListenerFactory(String bootstrapServers, String groupId) {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(createConsumerProps(bootstrapServers, groupId)));
        factory.setConcurrency(100);
        factory.setBatchListener(true);
        return factory;
    }

    /**
     * Tao Virtual Thread cho moi record xu ly
     */
    private void handleMessage(ConsumerRecord<String, byte[]> record) {
        kafkaListenerExecutor.submit(() -> publishEvent(record.key(), record.value()));
    }

    /**
     * Phuong thuc de lang nghe va xu ly cac tin nhan tu Kafka
     */
    private void consumeMessages(String topic) {
        // Dang ky tieu thu tin nhan tu Topic cu the
        consumer.subscribe(List.of(topic));
        try {
            while (true) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(1000));
                if (ObjectUtils.isEmpty(records)) {
                    break;
                }
                for (ConsumerRecord<String, byte[]> record : records) {
                    kafkaListenerExecutor.submit(() -> {
                        publishEvent(record.key(), record.value());
                    });
                }
            }
        } catch (WakeupException e) {
            // Xu ly khi consumer wakeup
        } finally {
            consumer.close();
        }
    }

    /**
     * Phuong thuc gui du lieu voi Kafka Producer
     */
    public void sendToKafka(String topic, String key, byte[] value) {
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, key, value);
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                LOGGER.error("Error sending message to Kafka: ", exception);
            } else {
                LOGGER.info("Message sent to Kafka: " + metadata.toString());
            }
        });
    }

    /**
     * Phuong thuc publish su kien
     */
    public void publishEvent(String key, byte[] value) {
        // Lay sequence tu ringBuffer
        long sequence = ringBuffer.next();
        try {
            // Lay su kien tai sequence va thiet lap gia tri
            EventRequest<ZimJi> event = ringBuffer.get(sequence);
            event.setEvent(key);
            event.setPayload((ZimJi) value);
        } finally {
            // Xuat ban su kien
            ringBuffer.publish(sequence);
        }
    }

    /**
     * Phuong thuc publish cho KafkaEventHandler
     */
    public void publish(String key, byte[] value) {
        disruptor.publishEvent((event, sequence) -> {
            event.setEvent(key);
            event.setPayload((ZimJi) value);
        });
    }

    /**
     * Phuong thuc dung Disruptor khi khong con su dung
     */
    @PreDestroy
    public void shutdown() throws InterruptedException {
        // Dong Kafka Consumer va dong cac tai nguyen
        consumer.close();

        // Dong Kafka Producer va dong cac tai nguyen
        producer.close();

        // Dong Disruptor va cac luong Worker
        disruptor.shutdown();

        if (ObjectUtils.isNotEmpty(messageListenerContainer)) {
            messageListenerContainer.stop();
        }

        // Dung thread Kafka Listener
        shutdownExecutor();
        LOGGER.info("Disruptor and Kafka Consumer shutdown successfully");
    }

    private void shutdownExecutor() throws InterruptedException {
        kafkaListenerExecutor.shutdown();
        if (!kafkaListenerExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
            kafkaListenerExecutor.shutdownNow();
        }
    }

    /**
     * Phuong thuc cau hinh Consumer
     */
    private static Map<String, Object> createConsumerProps(String bootstrap, String group) {
        return Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap,
                ConsumerConfig.GROUP_ID_CONFIG, group,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true,
                ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "100",
                ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "15000",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class,
                ConsumerConfig.FETCH_MIN_BYTES_CONFIG, "1048576", // 1MB toi uu
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "500" // So ban ghi toi da moi lan Poll
        );
    }

    /**
     * Phuong thuc cau hinh Producer
     */
    private static Properties createProducerProps(String bootstrap) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // Dam bao tat ca cac Broker deu gi nhan
        props.put(ProducerConfig.RETRIES_CONFIG, 3); // So lan thu lai khi gui that bai
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // Kich thuoc batch
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1); // Thoi gian cho truoc khi gui batch
        props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, CustomPartitioner.class); // Cau hinh partitioner tuy chinh
        return props;
    }

}
