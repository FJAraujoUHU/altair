package com.aajpm.altair.utility.exception;

public class DeviceException extends RuntimeException {
    public DeviceException() {
        super();
    }

    public DeviceException(String msg) {
        super(msg);
    }

    public DeviceException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public DeviceException(Throwable cause) {
        super(cause);
    }
}
