package com.aajpm.altair.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;

import com.aajpm.altair.security.account.*;

@ComponentScan(basePackages = {"com.aajpm.altair.security"})
@Configuration
@EnableWebSecurity
//@EnableMethodSecurity
public class SecurityConfig {
    
    @Autowired
    private AltairUserService userDetailsService;

    @Bean
    PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }


    // Authentication config
    @Bean
    DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(encoder());
        return authProvider;
    }

    @Bean
    AuthenticationManager authManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
        .authenticationProvider(authProvider())
        .build();
    }

    // Role hierarchy config
    @Bean
    RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        String hierarchy = "ROLE_ADMIN > ROLE_ADVANCED_USER \n ROLE_ADVANCED_USER > ROLE_BASIC_USER";
        roleHierarchy.setHierarchy(hierarchy);
        return roleHierarchy;
    }

    // Throws error bc it's overriding org/springframework/security/config/annotation/web/configuration/WebSecurityConfiguration.class -> check if implementing filterchain() fixes it.
    @Bean
    DefaultWebSecurityExpressionHandler webSecurityExpressionHandler() {
        DefaultWebSecurityExpressionHandler expressionHandler = new DefaultWebSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy());
        return expressionHandler;
    }

}
