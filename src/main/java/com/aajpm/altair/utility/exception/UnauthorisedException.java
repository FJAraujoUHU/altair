package com.aajpm.altair.utility.exception;

import com.aajpm.altair.security.account.AltairUser;

public class UnauthorisedException extends RuntimeException {
    
        private final AltairUser user;
        
        public UnauthorisedException() {
            super();
            this.user = null;
        }

        public UnauthorisedException(AltairUser user) {
            super("The user " + user.getUsername() + " is not authorised to perform this action.");
            this.user = user;
        }
    
        public UnauthorisedException(String message) {
            super(message);
            this.user = null;
        }

        public UnauthorisedException(AltairUser user, String message) {
            super(message);
            this.user = user;
        }
    
        public UnauthorisedException(Throwable cause) {
            super(cause);
            this.user = null;
        }

        public UnauthorisedException(AltairUser user, Throwable cause) {
            super(cause);
            this.user = user;
        }
    
        public UnauthorisedException(String message, Throwable cause) {
            super(message, cause);
            this.user = null;
        }

        public UnauthorisedException(AltairUser user, String message, Throwable cause) {
            super(message, cause);
            this.user = user;
        }
    
        public UnauthorisedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
            this.user = null;
        }

        public UnauthorisedException(AltairUser user, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
            this.user = user;
        }
    
        public AltairUser getUser() {
            return user;
        }
}
