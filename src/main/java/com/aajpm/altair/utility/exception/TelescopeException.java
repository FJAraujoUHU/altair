package com.aajpm.altair.utility.exception;

public class TelescopeException extends RuntimeException {
    public TelescopeException() {
        super();
    }

    public TelescopeException(String msg) {
        super(msg);
    }

    public TelescopeException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public TelescopeException(Throwable cause) {
        super(cause);
    }
}
