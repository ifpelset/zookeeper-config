package com.dfsx.library.zookeeperconfig;

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by ifpelset on 11/7/16.
 */
public class ExistsEventLoop extends EventLoop implements Watcher, AsyncCallback.StatCallback {
    private static Logger log = LoggerFactory.getLogger(ExistsEventLoop.class);

    // 四种情况：1. null, 2. 一个GetDataEventLoop, 3. 一个GetChildrenEventLoop, 4. 情况2 3合并
    //private List<EventLoop> eventLoopList;
    private GetDataEventLoop getDataEventLoop;
    private GetChildrenEventLoop getChildrenEventLoop;

    // 每个（每种EventLoop）拥有一个pathToStartedMap代表我是否正在监控某path
    // Exists负责管理GetDataEventLoop和GetChildrenEventLoop
    // 需要维护两个map用来代表两个eventLoop分别“监听过”什么path，以便在节点删除和创建之后重启监控
    // 至于目前是否在监控，并不关心
    // 原因：比如第一次去启动getDataEventLoop或者getChildrenEventLoop，但是节点不存在
    // 在调用相应的方法之前就去put各自的pathToStartedMap，就不需要下面的两个map了
//    private Map<String, String> getDataEventLoopWatchedMap;
//    private Map<String, String> getChildrenEventLoopWatchedMap;

    public ExistsEventLoop(ZookeeperOperation operation, DataMonitorListener listener) {
        super(operation, listener);

        this.getDataEventLoop = new GetDataEventLoop(operation, listener);
        this.getChildrenEventLoop = new GetChildrenEventLoop(operation, listener);
    }

    public GetDataEventLoop getGetDataEventLoop() {
        return getDataEventLoop;
    }

    public GetChildrenEventLoop getGetChildrenEventLoop() {
        return getChildrenEventLoop;
    }

    public synchronized void run(String path, FuncType funcType) {
        // 根据funcType设置 事件循环自己维护的startedMap
        // 如果是新的path，添加到map中并设置为未启动
        Map<String, Boolean> startedMap = this.getPathToStartedMap();
        Map<String, Boolean> validMap = this.getPathToValidMap();

        // 首先优先处理exists 只要map中没有 无条件添加
        if (!startedMap.containsKey(path)) {
            startedMap.put(path, false);
        }

        if (!validMap.containsKey(path)) {
            validMap.put(path, false);
        }

        // 下面根据条件处理getdata 和 getchildren
        if (funcType == FuncType.GETDATA) {
            startedMap = this.getDataEventLoop.getPathToStartedMap();
            validMap = this.getDataEventLoop.getPathToValidMap();

            if (!startedMap.containsKey(path)) {
                startedMap.put(path, false);
            }

            if (!validMap.containsKey(path)) {
                validMap.put(path, false);
            }
        } else if (funcType == FuncType.GETCHILDREN) {
            startedMap = this.getChildrenEventLoop.getPathToStartedMap();
            validMap = this.getChildrenEventLoop.getPathToValidMap();

            if (!startedMap.containsKey(path)) {
                startedMap.put(path, false);
            }

            if (!validMap.containsKey(path)) {
                validMap.put(path, false);
            }
        }

        // 启动exists的异步调用（注册watcher，在异步回调中启动其他的事件循环）
        // 肯定能确定exists 事件循环中 startedMap中包含该path
        start(path);
    }

    @Override
    public void start(String path) {
        // 自己未启动或者子事件循环应该启动但是未启动 则启动
        Map<String, Boolean> getDataEventLoopPathToStartedMap = this.getDataEventLoop.getPathToStartedMap();
        Map<String, Boolean> getChildrenEventLoopPathToStartedMap = this.getChildrenEventLoop.getPathToStartedMap();

        if (!this.pathToStartedMap.get(path)
                || (getDataEventLoopPathToStartedMap.containsKey(path)
                        && !getDataEventLoopPathToStartedMap.get(path))
                || (getChildrenEventLoopPathToStartedMap.containsKey(path)
                        && !getChildrenEventLoopPathToStartedMap.get(path))) {
            this.operation.exists(path, this, this);
        }
    }

    @Override
    public void stop(String path) {
        super.stop(path);
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        log.info("process received event:" + watchedEvent.toString());
        String path = watchedEvent.getPath();
        switch (watchedEvent.getType()) {
            case None:
                switch (watchedEvent.getState()) {
                    case Expired:
                    case Disconnected:
                        log.warn("expired or disconnected");
                        // 交给默认watcher处理
//                        this.getOperation().setDead(true);
                        break;
                    case SyncConnected: // 客户端连接到服务端
                        log.info("connected");
                        break;
                }
                break;
            case NodeDataChanged:
            case NodeCreated: // 只有exists会注册该事件触发的watcher
                this.operation.exists(path, this, this);
                break;
            case NodeDeleted:
                this.operation.exists(path, this, this);
                // 停止对应的事件循环
                // 这里不用去判断，只要之前没有启动过，这里就不用去停止
                this.getDataEventLoop.stop(path);
                this.getChildrenEventLoop.stop(path);
                break;
        }
    }

    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        log.info("processResult: rc:" + rc +", path:" + path + ", exist:" + (stat != null ? "true" : "false"));

        boolean exists;
        switch (KeeperException.Code.get(rc)) {
            case OK:
                exists = true;
                break;
            case NONODE:
                exists = false;
                break;
            // 会话超时代表断开连接
            case SESSIONEXPIRED:
            case NOAUTH:
                // exists 的callback处理
                this.getOperation().setDead(true);
                return ;
            default:
                // 其他的错误直接返回
                return;
        }

        if (!this.pathToStartedMap.get(path)) {
            this.pathToStartedMap.put(path, true);
        }

        this.listener.updateExistsMap(path, exists);

        if (!this.pathToValidMap.get(path)) {
            this.pathToValidMap.put(path, true);
        }

        // 在这里面根据条件启动另外两个事件循环
        // 要求节点要存在
        if (exists) {
            Map<String, Boolean> startedMap = this.getDataEventLoop.getPathToStartedMap();
            // exists启动getDataEventLoop的条件
            if (startedMap.containsKey(path)) {
                this.getDataEventLoop.start(path);
            }

            startedMap = this.getChildrenEventLoop.getPathToStartedMap();
            // exists启动getChildrenEventLoop的条件
            if (startedMap.containsKey(path)) {
                this.getChildrenEventLoop.start(path);
            }
        } else { // 节点不存在也设置数据为有效，只是不去启动两个事件循环
            Map<String, Boolean> startedMap = this.getDataEventLoop.getPathToStartedMap();
            // exists设置getDataEventLoop的数据有效的条件
            if (startedMap.containsKey(path)) {
                this.getDataEventLoop.getPathToValidMap().put(path, true);
            }

            startedMap = this.getChildrenEventLoop.getPathToStartedMap();
            // exists设置getChildrenEventLoop的数据有效的条件
            if (startedMap.containsKey(path)) {
                this.getChildrenEventLoop.getPathToValidMap().put(path, true);
            }
        }
    }
}
