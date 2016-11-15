package com.dfsx.library.zookeeperconfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ifpelset on 11/7/16.
 */
public abstract class EventLoop {
    protected ZookeeperOperation operation;
    protected DataMonitorListener listener;

    // path -> 是否启动事件循环（是否被监控中）
    protected Map<String, Boolean> pathToStartedMap;
    // path -> 各个事件循环修改的map中的数据是否有效
    // 只限第一次调用使用
    protected Map<String, Boolean> pathToValidMap;

    public EventLoop(ZookeeperOperation operation, DataMonitorListener listener) {
        this.operation = operation;
        this.listener = listener;

        this.pathToStartedMap = new ConcurrentHashMap<String, Boolean>(10);
        this.pathToValidMap = new ConcurrentHashMap<String, Boolean>(10);
    }

    public Map<String, Boolean> getPathToStartedMap() {
        return pathToStartedMap;
    }

    public Map<String, Boolean> getPathToValidMap() {
        return pathToValidMap;
    }

    public ZookeeperOperation getOperation() {
        return operation;
    }

    // 启动循环
    // ExistsEventLoop 调用exists
    // GetChildrenEventLopp 调用getChildren
    // GetDataEventLoop 调用getData
    public abstract void start(String path);

    // 停止循环
    public void stop(String path) {
        if (this.pathToStartedMap.containsKey(path)
                && this.pathToStartedMap.get(path)) {
            // 停止watcher不需要做任何操作，只需要在process中调用以下stop，将标志位设置为false
            this.pathToStartedMap.put(path, false);
        }
    }
}
