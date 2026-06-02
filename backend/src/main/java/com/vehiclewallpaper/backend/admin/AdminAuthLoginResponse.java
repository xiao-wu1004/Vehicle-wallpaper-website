package com.vehiclewallpaper.backend.admin;

import java.time.LocalDateTime;

public class AdminAuthLoginResponse {

    private final String username;
    private final String authMode;
    private final String accessToken;
    private final LocalDateTime expiresAt;

    public AdminAuthLoginResponse(String username, String authMode, String accessToken, LocalDateTime expiresAt) {
        this.username = username;
        this.authMode = authMode;
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
    }

    public String getUsername() {
        return username;
    }

    public String getAuthMode() {
        return authMode;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
