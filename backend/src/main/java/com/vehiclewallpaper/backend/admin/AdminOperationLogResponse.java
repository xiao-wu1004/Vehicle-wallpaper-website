package com.vehiclewallpaper.backend.admin;

import java.time.LocalDateTime;

public class AdminOperationLogResponse {

    private final Long id;
    private final String actorUsername;
    private final String authMode;
    private final String action;
    private final String targetType;
    private final String targetId;
    private final String detail;
    private final String requestPath;
    private final String ipAddress;
    private final String userAgent;
    private final LocalDateTime createdAt;

    public AdminOperationLogResponse(Long id,
                                     String actorUsername,
                                     String authMode,
                                     String action,
                                     String targetType,
                                     String targetId,
                                     String detail,
                                     String requestPath,
                                     String ipAddress,
                                     String userAgent,
                                     LocalDateTime createdAt) {
        this.id = id;
        this.actorUsername = actorUsername;
        this.authMode = authMode;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.detail = detail;
        this.requestPath = requestPath;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getAuthMode() {
        return authMode;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getDetail() {
        return detail;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
