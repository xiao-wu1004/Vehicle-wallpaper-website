package com.vehiclewallpaper.backend.admin;

public class AdminIdentity {

    private final String username;
    private final String authMode;

    public AdminIdentity(String username, String authMode) {
        this.username = username;
        this.authMode = authMode;
    }

    public String getUsername() {
        return username;
    }

    public String getAuthMode() {
        return authMode;
    }
}
