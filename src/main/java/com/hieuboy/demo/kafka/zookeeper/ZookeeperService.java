package com.hieuboy.demo.kafka.zookeeper;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.stereotype.Service;

@Service
public class ZookeeperService {

    private final ZooKeeper zooKeeper;

    public ZookeeperService(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public void registerNode(String path, byte[] data) throws Exception {
        Stat stat = zooKeeper.exists(path, false);
        if (ObjectUtils.isEmpty(stat)) {
            zooKeeper.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } else {
            zooKeeper.setData(path, data, -1);
        }
    }

    public byte[] getNodeData(String path) throws Exception {
        return zooKeeper.getData(path, false, null);
    }

}
