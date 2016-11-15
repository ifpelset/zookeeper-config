package com.dfsx.library.zookeeperconfig;

import org.apache.zookeeper.AsyncCallback.ChildrenCallback;
import org.apache.zookeeper.AsyncCallback.DataCallback;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by ifpelset on 11/2/16.
 */
public class ZookeeperOperation implements Watcher {
    private static Logger log = LoggerFactory.getLogger(ZookeeperOperation.class);
    private String connectString;
    private int sessionTimeout;
    private boolean isDead; // 标识该zookeeper对象连接是否正常
    private ZooKeeper zooKeeper;


    public ZookeeperOperation(String connectString, int sessionTimeout) {
        this.connectString = connectString;
        this.sessionTimeout = sessionTimeout;
        this.isDead = false;

        this.zooKeeper = createZooKeeper();
    }

    public boolean isDead() {
        return isDead;
    }

    public void setDead(boolean dead) {
        isDead = dead;
    }

    private ZooKeeper createZooKeeper() {
        ZooKeeper zooKeeper = null;
        try {
            zooKeeper = new ZooKeeper(connectString, sessionTimeout, this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return zooKeeper;
    }

    public void close() {
        try {
            this.zooKeeper.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean exists(String path, Watcher watcher) throws KeeperException, InterruptedException {
        return null != this.zooKeeper.exists(path, watcher);
    }

    public void exists(String path, Watcher watcher, StatCallback cb) {
        this.zooKeeper.exists(path, watcher, cb, null);
    }

    public String getData(String path, Watcher watcher) throws KeeperException, InterruptedException {
        return new String(this.zooKeeper.getData(path, watcher, null));
    }

    public void getData(String path, Watcher watcher, DataCallback cb) {
        this.zooKeeper.getData(path, watcher, cb, null);
    }

    public String setData(String path, String value, ZooKeeperSetFlag flag) {
        String retval = path;
        try {
            switch (flag) {
                case CREATE:
                    retval = zooKeeper.create(path, value.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    break;
                case CREATE_TEMPORARY:
                    retval = zooKeeper.create(path, value.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                    break;
                case OVERWRITE: // setData 节点 必须要存在，不存在自己抛异常了
                    zooKeeper.setData(path, value.getBytes(), -1);
                    break;
                case OVERWRITE_IF_EXISTS: // 如果存在则覆盖，否则创建
                    while (true) {
                        try {
                            // 这里只是查询是否存在，不去注册watcher
                            if (zooKeeper.exists(path, false) != null) {
                                zooKeeper.setData(path, value.getBytes(), -1);
                            } else {
                                retval = zooKeeper.create(path, value.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                            }

                            return retval;
                        }catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (KeeperException e) {
                            e.printStackTrace();
                        }
                    }
                case SEQUENTIAL:
                    retval = zooKeeper.create(path, value.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
                    break;
                case SEQUENTIAL_TEMPORARY:
                    retval = zooKeeper.create(path, value.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
                    break;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }

        return retval;
    }

    public List<String> getChildren(String path, Watcher watcher) throws KeeperException, InterruptedException {
        return this.zooKeeper.getChildren(path, watcher, null);
    }

    public void getChildren(String path, Watcher watcher, ChildrenCallback cb) {
        this.zooKeeper.getChildren(path, watcher, cb, null);
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        log.info("process received event:" + watchedEvent.toString());
        switch (watchedEvent.getType()) {
            case None:
                switch (watchedEvent.getState()) {
                    case Expired:
                    case Disconnected: // 连接断开，所有watcher都会触发，这里只在默认watcher处理下就行了
                        log.warn("expired or disconnected");
                        this.close();
                        this.isDead = true;
                        break;
                    case SyncConnected: // 客户端连接到服务端
                        log.info("connected");
//                        this.isDead = false; // 一new出来就当是活的
                        break;
                }
                break;
        }
    }
}
