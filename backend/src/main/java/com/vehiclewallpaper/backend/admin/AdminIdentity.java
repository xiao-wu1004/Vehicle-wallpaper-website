package com.vehiclewallpaper.backend.admin;

public class AdminIdentity {

    private final Long accountId;
    private final Long sessionId;
    private final String username;
    private final String authMode;

    public AdminIdentity(Long accountId, Long sessionId, String username, String authMode) {
        this.accountId = accountId;
        this.sessionId = sessionId;
        this.username = username;
        this.authMode = authMode;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public String getUsername() {
        return username;
    }

    public String getAuthMode() {
        return authMode;
    }
}
