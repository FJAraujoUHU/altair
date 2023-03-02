package com.aajpm.altair.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


import com.aajpm.altair.security.account.*;
import com.aajpm.altair.security.handler.*;

@ComponentScan(basePackages = {"com.aajpm.altair.security"})
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private AltairUserService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auths -> auths
            .requestMatchers("/index.html").permitAll()
            .requestMatchers("/").permitAll()
            .requestMatchers("/login*").permitAll() // asterisk so that error messages can be appended
            .anyRequest().denyAll()
        ).formLogin()
        .loginPage("/login")
        .failureHandler(AltairAuthFailHandler())
        .successHandler(AltairAuthSuccHandler())
        .permitAll()
        .and()
        .logout()
        .logoutUrl("/logout")
        .permitAll()
        .and()
        .build();

    }

    @Bean
    public AuthenticationFailureHandler AltairAuthFailHandler() {
        return new AltairAuthenticationFailureHandler();
    }

    @Bean
    public AuthenticationSuccessHandler AltairAuthSuccHandler() {
        return new AltairAuthenticationSuccessHandler();
    }

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

}
