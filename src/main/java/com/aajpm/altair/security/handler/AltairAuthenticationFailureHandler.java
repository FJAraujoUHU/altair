package com.aajpm.altair.security.handler;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.aajpm.altair.security.account.*;

@Component
public class AltairAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private AltairUserService userService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        super.onAuthenticationFailure(request, response, exception);

        userService.addFailedLoginAttempt(request.getParameter("username"));
    }
}
