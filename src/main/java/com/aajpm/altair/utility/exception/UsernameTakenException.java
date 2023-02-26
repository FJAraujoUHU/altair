package com.aajpm.altair.utility.exception;

public class UsernameTakenException extends RuntimeException {
    
    public UsernameTakenException() {
        super();
    }

    public UsernameTakenException(String msg) {
        super(msg);
    }

    public UsernameTakenException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public UsernameTakenException(Throwable cause) {
        super(cause);
    }
}
