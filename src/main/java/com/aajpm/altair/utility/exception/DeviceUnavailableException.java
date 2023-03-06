package com.aajpm.altair.utility.exception;

public class DeviceUnavailableException extends DeviceException {
    public DeviceUnavailableException() {
        super();
    }

    public DeviceUnavailableException(String msg) {
        super(msg);
    }

    public DeviceUnavailableException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public DeviceUnavailableException(Throwable cause) {
        super(cause);
    }
    
}
