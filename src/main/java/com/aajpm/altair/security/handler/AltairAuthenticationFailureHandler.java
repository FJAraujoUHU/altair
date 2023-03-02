package com.aajpm.altair.security.handler;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.aajpm.altair.security.account.*;

public class AltairAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private AltairUserService userService;

    public AltairAuthenticationFailureHandler() {
        super("/login?error");
    }

    public AltairAuthenticationFailureHandler(String defaultFailureUrl) {
        super(defaultFailureUrl);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        userService.addFailedLoginAttempt(request.getParameter("username"));
        
        super.onAuthenticationFailure(request, response, exception);   
    }
}
