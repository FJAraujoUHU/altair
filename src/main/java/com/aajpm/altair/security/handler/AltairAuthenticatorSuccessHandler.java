package com.aajpm.altair.security.handler;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.aajpm.altair.security.account.*;

@Component
public class AltairAuthenticatorSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    @Autowired
    private AltairUserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        super.onAuthenticationSuccess(request, response, authentication);
        
        userService.resetFailedLoginAttempts(request.getParameter("username"));
    }


}
