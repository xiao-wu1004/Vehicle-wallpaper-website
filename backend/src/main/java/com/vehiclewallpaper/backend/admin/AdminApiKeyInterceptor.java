package com.vehiclewallpaper.backend.admin;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AdminApiKeyInterceptor implements HandlerInterceptor {

    private final AdminAuthenticationService adminAuthenticationService;

    public AdminApiKeyInterceptor(AdminAuthenticationService adminAuthenticationService) {
        this.adminAuthenticationService = adminAuthenticationService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        AdminIdentity identity = adminAuthenticationService.authenticate(request);
        request.setAttribute(AdminIdentity.class.getName(), identity);
        return true;
    }
}
