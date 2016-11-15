package com.dfsx.library.zookeeperconfig;

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ifpelset on 11/7/16.
 */
public class GetDataEventLoop extends EventLoop implements Watcher, AsyncCallback.DataCallback {
    private static Logger log = LoggerFactory.getLogger(GetDataEventLoop.class);

    public GetDataEventLoop(ZookeeperOperation operation, DataMonitorListener listener) {
        super(operation, listener);
    }

    @Override
    public void start(String path) {
        if (!pathToStartedMap.get(path)) {
            this.operation.getData(path, this, this);
        }
    }

    @Override
    public void stop(String path) {
        super.stop(path);

        // 清理value map
        this.listener.updateValueMap(path, null);
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
            case NodeDeleted:
                // 停止监听
                this.stop(path);
                break;
            case NodeDataChanged:
                // 继续监听
                this.operation.getData(path, this, this);
                break;
        }
    }

    @Override
    public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
        log.info("processResult: rc:" + rc +", path:" + path + ", bytes:" + new String(data));

        boolean exists;
        switch (KeeperException.Code.get(rc)) {
            case OK:
                exists = true;
                break;
            case NONODE:
                exists = false;
                break;
            case SESSIONEXPIRED:
            case NOAUTH:
                // exists 的callback 会处理
//                this.getOperation().setDead(true);
                return ;
            default:
                // 其他的错误直接返回
                return;
        }

        if (exists) {
            if (!this.pathToStartedMap.get(path)) {
                this.pathToStartedMap.put(path, true);
            }

            this.listener.updateValueMap(path, data);

            if (!this.pathToValidMap.get(path)) {
                this.pathToValidMap.put(path, true);
//            listener.notify();
            }
        }
    }
}
