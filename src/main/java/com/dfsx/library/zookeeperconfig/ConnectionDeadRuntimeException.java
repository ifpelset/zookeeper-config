package com.dfsx.library.zookeeperconfig;

/**
 * Created by ifpelset on 11/11/16.
 */
public class ConnectionDeadRuntimeException extends RuntimeException {
    public ConnectionDeadRuntimeException() {
        super();
    }

    public ConnectionDeadRuntimeException(String message) {
        super(message);
    }

    public ConnectionDeadRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionDeadRuntimeException(Throwable cause) {
        super(cause);
    }

    protected ConnectionDeadRuntimeException(String message, Throwable cause,
                                           boolean enableSuppression,
                                           boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
