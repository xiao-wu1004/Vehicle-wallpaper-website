package com.vehiclewallpaper.backend.user;

import java.time.LocalDateTime;

public class UserAuthStatusResponse {

    private final boolean authenticated;
    private final String displayName;
    private final String email;
    private final LocalDateTime expiresAt;

    public UserAuthStatusResponse(boolean authenticated,
                                  String displayName,
                                  String email,
                                  LocalDateTime expiresAt) {
        this.authenticated = authenticated;
        this.displayName = displayName;
        this.email = email;
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

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
