package com.vehiclewallpaper.backend.admin;

import com.vehiclewallpaper.backend.config.AdminSecurityProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AdminApiKeyInterceptor implements HandlerInterceptor {

    private final AdminSecurityProperties adminSecurityProperties;

    public AdminApiKeyInterceptor(AdminSecurityProperties adminSecurityProperties) {
        this.adminSecurityProperties = adminSecurityProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String configuredApiKey = normalize(adminSecurityProperties.getApiKey());
        if (configuredApiKey.isEmpty()) {
            throw new AdminSecurityNotConfiguredException("Admin API key is not configured.");
        }

        String incomingApiKey = normalize(request.getHeader(adminSecurityProperties.getHeaderName()));
        if (!configuredApiKey.equals(incomingApiKey)) {
            throw new AdminUnauthorizedException("Missing or invalid admin API key.");
        }

        return true;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
