package com.vehiclewallpaper.backend.admin;

import java.time.LocalDateTime;

public class AdminUserResponse {

    private final Long id;
    private final String publicKey;
    private final String email;
    private final String displayName;
    private final boolean active;
    private final int failedLoginAttempts;
    private final LocalDateTime lockedUntil;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime createdAt;

    public AdminUserResponse(Long id, String publicKey, String email, String displayName,
                             boolean active, int failedLoginAttempts, LocalDateTime lockedUntil,
                             LocalDateTime lastLoginAt, LocalDateTime createdAt) {
        this.id = id;
        this.publicKey = publicKey;
        this.email = email;
        this.displayName = displayName;
        this.active = active;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getPublicKey() { return publicKey; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public boolean isActive() { return active; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
