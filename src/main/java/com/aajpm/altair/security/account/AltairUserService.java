package com.aajpm.altair.security.account;

import org.springframework.security.core.userdetails.*;

import java.time.LocalDateTime;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.aajpm.altair.config.AltairSecurityConfig;
import com.aajpm.altair.utility.exception.UsernameTakenException;

import jakarta.transaction.Transactional;

@Service
public class AltairUserService implements UserDetailsService {

    @Autowired
    private AltairUserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private AltairSecurityConfig securityConfig;

    @Autowired
    public AltairUserService(AltairSecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AltairUser user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User " + username + " not found");
        }
        return user;
    }

    // Registers a new basic AltairUser from a list of attributes.
    @Transactional
    public AltairUser registerNewAccount(String username, String password) throws UsernameTakenException {
        return registerNewAccount(username, password, true);
    }

    @Transactional
    public AltairUser registerNewAccount(String username, String password, boolean enabled) throws UsernameTakenException {
        if (doUsernameExist(username)) {
            throw new UsernameTakenException("Username " + username + " is already taken");
        }
        AltairUser user = new AltairUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setEnabled(enabled);
        user.setFailedLoginAttempts(0);
        user.addRole(roleRepository.findByName("BASIC_USER"));

        return userRepository.save(user);  
    }
    

    public boolean doUsernameExist(String username) {
        return userRepository.findByUsername(username) != null;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void resetFailedLoginAttempts() {
        userRepository.findTimeoutDoneUsers(LocalDateTime.now().minusSeconds(securityConfig.getLockoutTime()))
                .forEach(user -> {
                    user.setFailedLoginAttempts(0);
                    user.setLocked(false);
                    userRepository.save(user);
                });
    }

    @Transactional
    public boolean addFailedLoginAttempt(String username) {
        AltairUser user = userRepository.findByUsername(username);
        if (user == null) {
            return false;
        }

        int attempts = user.getFailedLoginAttempts() + 1;

        user.setFailedLoginAttempts(attempts);
        if (user.isAccountNonLocked()) {    // so that timeouts don't stack
            user.setLastLoginAttempt(LocalDateTime.now());
        }
        if (attempts >= securityConfig.getMaxAttempts()) {
            user.setLocked(true);
        }
        userRepository.save(user);

        return true;
    }

    @Transactional
    public boolean resetFailedLoginAttempts(String username) {
        AltairUser user = userRepository.findByUsername(username);
        if (user == null) {
            return false;
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAttempt(LocalDateTime.now());
        userRepository.save(user);

        return true;
    }

}
