package com.hieuboy.demo.kafka.partition;

import com.google.common.hash.Hashing;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomPartitioner implements Partitioner {

    static final Logger LOGGER = LoggerFactory.getLogger(CustomPartitioner.class);

    // Cache to store partition info for topics to avoid repetitive calls to cluster API
    final Map<String, List<PartitionInfo>> partitionCache = new ConcurrentHashMap<>();
    long lastUpdate = 0L;
    static final long CACHE_EXPIRY = 60000; // Cache expiry time (1 minute)

    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                         Object value, byte[] valueBytes, Cluster cluster) {

        /*List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int numPartitions = partitions.size();
        return (key.hashCode() & Integer.MAX_VALUE) % numPartitions;*/

        // Cap nhat cache phan vung neu da het thoi gian luu
        long now = System.currentTimeMillis();
        if (now - lastUpdate > CACHE_EXPIRY) {
            List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
            partitionCache.put(topic, partitions);
            lastUpdate = now;
            LOGGER.info("Cache updated for topic: {}", topic);
        }

        // Lay thong tin phan vung tu Cache
        List<PartitionInfo> partitions = partitionCache.get(topic);
        if (partitions == null || partitions.isEmpty()) {
            LOGGER.error("No partitions found for topic: {}", topic);
            throw new IllegalArgumentException("No partitions found for topic " + topic);
        }

        int numPartitions = partitions.size();

        // Su dung MurmurHash3 (128-bit) de phan phoi key tot hon
        int hash = Hashing.murmur3_128().hashBytes(key.toString().getBytes()).asInt();
        int partition = (hash & Integer.MAX_VALUE) % numPartitions;

        LOGGER.debug("Partitioning key: {} into partition: {} for topic: {}", key, partition, topic);

        return partition;
    }

    @Override
    public void close() {
        LOGGER.info("Partitioner closed.");

        // Neu co bat ky tai nguyen nao can giai phong (e.g., ket noi DB, cache), thuc hien tai day
        partitionCache.clear(); // Xoa cache neu can thiet
    }

    @Override
    public void configure(Map<String, ?> configs) {
        LOGGER.info("Partitioner configured with: {}", configs);
    }
}