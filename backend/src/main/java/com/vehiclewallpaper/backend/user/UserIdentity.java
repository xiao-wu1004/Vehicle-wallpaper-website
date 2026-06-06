package com.vehiclewallpaper.backend.user;

import java.time.LocalDateTime;

public class UserIdentity {

    private final Long accountId;
    private final Long sessionId;
    private final String publicKey;
    private final String displayName;
    private final String email;
    private final LocalDateTime expiresAt;

    public UserIdentity(Long accountId,
                        Long sessionId,
                        String publicKey,
                        String displayName,
                        String email,
                        LocalDateTime expiresAt) {
        this.accountId = accountId;
        this.sessionId = sessionId;
        this.publicKey = publicKey;
        this.displayName = displayName;
        this.email = email;
        this.expiresAt = expiresAt;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public String getPublicKey() {
        return publicKey;
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
