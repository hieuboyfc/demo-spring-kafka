package com.hieuboy.demo.kafka.zookeeper;

import lombok.Getter;
import lombok.Setter;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

@Setter
@Getter
public class ZookeeperClient {

    private ZooKeeper zooKeeper;

    public ZookeeperClient(String connectString, int sessionTimeout, Watcher watcher) throws IOException {
        this.zooKeeper = new ZooKeeper(connectString, sessionTimeout, watcher);
    }

    public void close() throws InterruptedException {
        zooKeeper.close();
    }

}
