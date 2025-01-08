package com.hieuboy.demo.kafka.config;

import com.hieuboy.demo.kafka.partition.CustomPartitioner;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.group-id}")
    private String groupId;

    @Bean
    public ProducerFactory<String, byte[]> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        configProps.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, CustomPartitioner.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Dam bao du lieu khong bi mat
        configProps.put(ProducerConfig.RETRIES_CONFIG, 5); // Retry gui du lieu neu loi
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 65536); // Batch kich thuoc lon hon
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10); // Delay nho de batch du lieu
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // Bo nho cho producer buffer
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); // Nen du lieu
        configProps.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 10485760); // Kich thuoc request toi da (10MB)
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 2000); // Timeout khi gui request
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000); // Thoi gian timeout tong cho delivery
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, byte[]> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, byte[]> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Xu ly thu cong offset
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500); // So luong record toi da moi lan Poll
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024); // Kich thuoc du lieu toi thieu
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 50); // Thoi gian cho toi da
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000);
        configProps.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1048576); // Cau hinh so Byte toi da tren mot phan vung
        configProps.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 5242880); // Tong so toi da moi lan fetch
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // Thoi gian toi da giua cac lan poll (5 phut)
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 1000); // Thoi gian heartbeat giua cac poll
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(10); // Tang concurrency de xu ly song song
        factory.setBatchListener(true); // Su dung listener theo batch de toi uu hieu suat
        return factory;
    }

}
