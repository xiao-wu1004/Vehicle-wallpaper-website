package com.vehiclewallpaper.backend.user;

import java.time.LocalDateTime;

public class UserAuthSessionResponse {

    private final boolean authenticated;
    private final String displayName;
    private final String email;
    private final String accessToken;
    private final LocalDateTime expiresAt;

    public UserAuthSessionResponse(boolean authenticated,
                                   String displayName,
                                   String email,
                                   String accessToken,
                                   LocalDateTime expiresAt) {
        this.authenticated = authenticated;
        this.displayName = displayName;
        this.email = email;
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
