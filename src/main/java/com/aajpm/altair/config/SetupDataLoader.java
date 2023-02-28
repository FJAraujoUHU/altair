package com.aajpm.altair.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.aajpm.altair.security.account.*;

import jakarta.transaction.Transactional;

@Component
public class SetupDataLoader implements ApplicationListener<ContextRefreshedEvent>{

    private boolean alreadySetup = false;

    @Autowired
    private AltairUserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;


    // Makes sure that the database is populated with the right roles and
    // creates a default Admin account.
    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {
        
        // This might get called several times, so it makes sure to only run once.
        if (alreadySetup)
            return;

        // Creates the roles if they don't exist.
        Role adminRole = createRoleIfNotFound("ADMIN");
        Role basicUserRole = createRoleIfNotFound("BASIC_USER");
        Role advUserRole = createRoleIfNotFound("ADVANCED_USER");

        // Creates the default admin account if it doesn't exist.
        AltairUser adminUser = userRepository.findByUsername("admin");
        if (adminUser == null) {
            adminUser = new AltairUser();
            adminUser.setUsername("admin");
            adminUser.setEnabled(true);
            // Please, change this default password before deploying to production,
            // as it is a major security risk.
            adminUser.setPassword(encoder.encode("hikoboshi"));
            adminUser.addRole(adminRole);
            adminUser.addRole(advUserRole);
            adminUser.addRole(basicUserRole);
            userRepository.save(adminUser);
        } 
    }

    @Transactional
    protected Role createRoleIfNotFound(String name) {
        Role role = roleRepository.findByName(name);
        if (role == null) {
            role = new Role(name);
            roleRepository.save(role);
        }
        return role;
    }
    
}
