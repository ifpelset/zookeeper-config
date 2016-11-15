package com.dfsx.library.zookeeperconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ifpelset on 11/8/16.
 */
public class ZooKeeperConfigTest implements Runnable {
    private static Logger log = LoggerFactory.getLogger(ZooKeeperConfigTest.class);

    public static void main(String[] args) {
        SingleThreadTest();
//        MultiThreadTest();
    }

    public static void SingleThreadTest() {
        ZooKeeperConfig kv = new ZooKeeperConfig("localhost:2181", 5000);

        while (true) {
            try {
                kv.printMap();

                log.info(">> /root content is ");
                log.info(kv.get("/root"));

                log.info(">> /root is exist or not ?");
                log.info(kv.exists("/root") ? "yes" : "no");

                log.info(">> /root children paths ");
                log.info(String.valueOf(kv.getChildPaths("/root")));
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(3000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void MultiThreadTest() {
        ZooKeeperConfigTest zooKeeperTest = new ZooKeeperConfigTest();

        MyThread thread1 = new MyThread();
        MyThread thread2 = new MyThread();

        new Thread(zooKeeperTest).start();
        new Thread(thread1).start();
        new Thread(thread2).start();
    }

    @Override
    public void run() {
        ZooKeeperConfig kv = new ZooKeeperConfig("localhost:2181", 100000);
        String path = "/root";

        for (int i = 0; i < 50; i++) {
            path = "/root";
            path += i;
            try {
                log.info(">> set " + path);
                log.info(kv.set(path, "223", ZooKeeperSetFlag.OVERWRITE_IF_EXISTS));
                log.info(">> " + path + " content is ");
                log.info(kv.get(path));
                log.info(">> " + path + " children paths ");
                log.info(String.valueOf(kv.getChildPaths(path)));

                Thread.sleep(50L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            Thread.sleep(10000L);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String[] dataArray = new String[50];
        for (int i = 0; i < 50; i++) {
            path = "/root";
            path += i;

            try {
                dataArray[i] = kv.get(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < 50; i++) {
            log.info(i + ":" + dataArray[i]);
        }
    }
}

class MyThread implements Runnable {
    private static Logger log = LoggerFactory.getLogger(MyThread.class);

    @Override
    public void run() {
        ZooKeeperConfig kv = new ZooKeeperConfig("localhost:2181", 100000);
        String path = "/root";

        for (int i = 0; i < 50; i++) {
            path = "/root";
            path += i;
            try {
                log.info(">> set " + path);
                log.info(kv.set(path, "312", ZooKeeperSetFlag.OVERWRITE_IF_EXISTS));
                log.info(">> " + path + " content is ");
                log.info(kv.get(path));
                log.info(">> " + path + " children paths ");
                log.info(String.valueOf(kv.getChildPaths(path)));

                Thread.sleep(50L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            Thread.sleep(10000L);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String[] dataArray = new String[50];
        for (int i = 0; i < 50; i++) {
            path = "/root";
            path += i;

            try {
                dataArray[i] = kv.get(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < 50; i++) {
            log.info(i + ":" + dataArray[i]);
        }
    }
}
