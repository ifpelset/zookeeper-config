package com.dfsx.library.zookeeperconfig;

/**
 * Created by ifpelset on 11/11/16.
 */
public class WaitTimedOutRuntimeException extends RuntimeException {
    public WaitTimedOutRuntimeException() {
        super();
    }

    public WaitTimedOutRuntimeException(String message) {
        super(message);
    }

    public WaitTimedOutRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public WaitTimedOutRuntimeException(Throwable cause) {
        super(cause);
    }

    protected WaitTimedOutRuntimeException(String message, Throwable cause,
                               boolean enableSuppression,
                               boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
