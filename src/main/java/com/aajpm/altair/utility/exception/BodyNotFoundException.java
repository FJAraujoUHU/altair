package com.aajpm.altair.utility.exception;

public class BodyNotFoundException extends SolverException {
    
        public BodyNotFoundException(String body) {
            super("Body " + body + " not found");
        }

}
