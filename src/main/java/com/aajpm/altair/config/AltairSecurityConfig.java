package com.aajpm.altair.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "altair.security")
public class AltairSecurityConfig {

    // Maximum number of failed login attempts before lockout
    private int maxAttempts = 3;
    // Time in seconds to lockout after maxAttempts
    private int lockoutTime = 300;
    // This is the default password for the admin. Should be changed before rollout.
    private String defaultPassword = "hikoboshi";

    /////////////////////////// Getters and Setters ///////////////////////////
    //#region Getters and Setters
    // These are used by Spring to set the values from the application.properties file.
    
    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getLockoutTime() {
        return lockoutTime;
    }

    public void setLockoutTime(int lockoutTime) {
        this.lockoutTime = lockoutTime;
    }

    public String getDefaultPassword() {
        return defaultPassword;
    }

    public void setDefaultPassword(String defaultPassword) {
        this.defaultPassword = defaultPassword;
    }

    //#endregion
}
