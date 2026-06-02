package com.vehiclewallpaper.backend.admin;

import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Service
public class AdminRequestContextService {

    public HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return null;
        }
        return ((ServletRequestAttributes) attributes).getRequest();
    }

    public AdminIdentity currentIdentity() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        Object attribute = request.getAttribute(AdminIdentity.class.getName());
        return attribute instanceof AdminIdentity ? (AdminIdentity) attribute : null;
    }

    public AdminRequestMetadata currentMetadata() {
        return extract(currentRequest());
    }

    public AdminRequestMetadata extract(HttpServletRequest request) {
        if (request == null) {
            return new AdminRequestMetadata("unknown", "unknown", "unknown");
        }

        String forwardedFor = normalize(request.getHeader("X-Forwarded-For"));
        String clientIp = forwardedFor;
        if (clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }
        if (clientIp.isEmpty()) {
            clientIp = normalize(request.getRemoteAddr());
        }
        if (clientIp.isEmpty()) {
            clientIp = "unknown";
        }

        String userAgent = normalize(request.getHeader("User-Agent"));
        if (userAgent.isEmpty()) {
            userAgent = "unknown";
        }

        String requestPath = normalize(request.getRequestURI());
        if (requestPath.isEmpty()) {
            requestPath = "unknown";
        }

        return new AdminRequestMetadata(requestPath, clientIp, userAgent);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
