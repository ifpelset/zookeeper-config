package com.dfsx.library.zookeeperconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by ifpelset on 11/10/16.
 */
public class ZooKeeperConfig {
    private static Logger log = LoggerFactory.getLogger(ZooKeeperConfig.class);

    private String connectString;
    private int sessionTimeout;
    private ZooKeeperConfigImpl zooKeeperConfigImpl;

    public ZooKeeperConfig(String connectString) {
        this(connectString, 5000);
    }

    public ZooKeeperConfig(String connectString, int sessionTimeout) {
        this.connectString = connectString;
        this.sessionTimeout = sessionTimeout;
        this.zooKeeperConfigImpl = new ZooKeeperConfigImpl(connectString, sessionTimeout);
    }

    private synchronized void tryToConnect() {
        if (!this.zooKeeperConfigImpl.isOk()) {
            log.info("try to connect, create new instance");
            this.zooKeeperConfigImpl = new ZooKeeperConfigImpl(this.connectString, this.sessionTimeout);
        }
    }

    public boolean exists(String path) {
        tryToConnect();

        return this.zooKeeperConfigImpl.exists(path);
    }

    public String get(String path) throws Exception {
        tryToConnect();

        return this.zooKeeperConfigImpl.get(path);
    }

    public String set(String path, String value, ZooKeeperSetFlag flag) throws Exception {
        tryToConnect();

        return this.zooKeeperConfigImpl.set(path, value, flag);
    }

    public List<String> getChildPaths(String path) throws Exception {
        tryToConnect();

        return this.zooKeeperConfigImpl.getChildPaths(path);
    }

    // for DEBUG
    public void printMap() {
        this.zooKeeperConfigImpl.printMap();
    }
}
