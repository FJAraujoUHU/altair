package com.aajpm.altair.utility.exception;

public class SolverException extends RuntimeException {

    public SolverException() {
        super();
    }

    public SolverException(String msg) {
        super(msg);
    }

    public SolverException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public SolverException(Throwable cause) {
        super(cause);
    }
}
