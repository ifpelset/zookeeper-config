package com.dfsx.library.zookeeperconfig;

/**
 * Created by ifpelset on 11/11/16.
 */
public class ZNodeNotExistException extends Exception {
    public ZNodeNotExistException() {
        super();
    }

    public ZNodeNotExistException(String message) {
        super(message);
    }

    public ZNodeNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

    public ZNodeNotExistException(Throwable cause) {
        super(cause);
    }

    protected ZNodeNotExistException(String message, Throwable cause,
                                     boolean enableSuppression,
                                     boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
