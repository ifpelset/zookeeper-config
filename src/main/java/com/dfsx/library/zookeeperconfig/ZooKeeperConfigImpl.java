package com.dfsx.library.zookeeperconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ifpelset on 11/2/16.
 */
public class ZooKeeperConfigImpl implements DataMonitorListener {
    private static Logger log = LoggerFactory.getLogger(ZooKeeperConfigImpl.class);
    private Map<String, Boolean> pathToExistsMap; // 受监控过的map
    private Map<String, String> pathToValueMap;
    private Map<String, List<String>> pathToChildPathsMap;

    private ExistsEventLoop eventLoop;


    public ZooKeeperConfigImpl(String connectString, int sessionTimeout) {
        this.pathToExistsMap = new ConcurrentHashMap<String, Boolean>(10);
        this.pathToValueMap = new ConcurrentHashMap<String, String>(10);
        this.pathToChildPathsMap = new ConcurrentHashMap<String, List<String>>(10);

        this.eventLoop = new ExistsEventLoop(
                new ZookeeperOperation(connectString, sessionTimeout),
                this);
    }

    // 标志是否可以进行操作了，该代表zk是否连接成功
    public boolean isOk() {
        return !this.eventLoop.getOperation().isDead();
    }

    public void close() {
        this.eventLoop.getOperation().close();
    }

    public boolean exists(String path) {
        // 调用之前要判断zookeeper连接是否存活
        if (!isOk()) {
            throw new ConnectionDeadRuntimeException("ZooKeeper connection is dead");
        }

        this.eventLoop.run(path, FuncType.EXISTS); // 每次调用最开始都去启动exists的事件循环

        Map<String, Boolean> validMap = this.eventLoop.getPathToValidMap();

        // 等待数据有效
        int count = 0;
        while (!validMap.get(path) || !isOk()) {
            try {
                Thread.sleep(10L);

                // time out
                if (++count >= 500 || !isOk()) {
                    throw new WaitTimedOutRuntimeException("Waiting for exists map valid timed out");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 数据有效但是“节点存在” map里面没有这个path的数据，返回节点不存在
        if (!this.pathToExistsMap.containsKey(path)) {
            return false;
        }

        return this.pathToExistsMap.get(path);
    }

    public String get(String path) throws Exception {
        // 调用之前要判断zookeeper连接是否存活
        if (!isOk()) {
            throw new ConnectionDeadRuntimeException("ZooKeeper connection is dead");
        }

        this.eventLoop.run(path, FuncType.GETDATA); // 每次调用最开始都去启动exists+getData的事件循环

        Map<String, Boolean> validMap = this.eventLoop.getGetDataEventLoop().getPathToValidMap();

        // 等待数据有效
        int count = 0;
        while (!validMap.get(path) || !isOk()) {
            try {
                Thread.sleep(10L);

                // time out
                if (++count >= 500 || !isOk()) {
                    throw new WaitTimedOutRuntimeException("Waiting for value map valid timed out");

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 数据有效但是数据map里面没有这个path的数据，抛出节点不存在的异常
        if (!this.pathToValueMap.containsKey(path)) {
            throw new ZNodeNotExistException("znode: " + path + " is not exist");
        }

        return this.pathToValueMap.get(path);
    }

    public String set(String path, String value, ZooKeeperSetFlag flag) {
        // 调用之前要判断zookeeper连接是否存活
        if (!isOk()) {
            throw new ConnectionDeadRuntimeException("ZooKeeper connection is dead");
        }

        return this.eventLoop.getOperation().setData(path, value, flag);
    }

    public List<String> getChildPaths(String path) throws Exception {
        // 调用之前要判断zookeeper连接是否存活
        if (!isOk()) {
            throw new ConnectionDeadRuntimeException("ZooKeeper connection is dead");
        }

        this.eventLoop.run(path, FuncType.GETCHILDREN); // 每次调用最开始都去启动exists+getChildren的事件循环

        Map<String, Boolean> validMap = this.eventLoop.getGetChildrenEventLoop().getPathToValidMap();

        // 等待数据有效
        int count = 0;
        while (!validMap.get(path) || !isOk()) {
            try {
                Thread.sleep(10L);

                // time out
                if (++count >= 500 || !isOk()) {
                    throw new WaitTimedOutRuntimeException("Waiting for children map valid timed out");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 数据有效但是孩子节点map里面没有这个path的数据，抛出节点不存在的异常
        if (!this.pathToChildPathsMap.containsKey(path)) {
            throw new ZNodeNotExistException("znode: " + path + " is not exist");
        }

        return this.pathToChildPathsMap.get(path);
    }

    @Override
    public void updateExistsMap(String path, boolean exist) {
        this.pathToExistsMap.put(path, exist);
    }

    @Override
    public void updateValueMap(String path, byte[] data) {
        if (data == null) {
            if (this.pathToValueMap.containsKey(path)) {
                this.pathToValueMap.remove(path);
            }
        } else {
            this.pathToValueMap.put(path, new String(data));
        }
    }

    @Override
    public void updateChildrenMap(String path, List<String> children) {
        if (children == null) {
            if (this.pathToChildPathsMap.containsKey(path)) {
                this.pathToChildPathsMap.remove(path);
            }
        } else {
            this.pathToChildPathsMap.put(path, children);
        }
    }

    // for DEBUG
    public void printMap() {
        log.debug("data->started:" + this.eventLoop.getGetDataEventLoop().getPathToStartedMap());
        log.debug("child->started:" + this.eventLoop.getGetChildrenEventLoop().getPathToStartedMap());

        log.debug("path->exists:" + this.pathToExistsMap);
        log.debug("path->value:" + this.pathToValueMap);
        log.debug("path->children:" + this.pathToChildPathsMap);
    }
}

enum FuncType {
    EXISTS,
    GETDATA,
    GETCHILDREN
}

