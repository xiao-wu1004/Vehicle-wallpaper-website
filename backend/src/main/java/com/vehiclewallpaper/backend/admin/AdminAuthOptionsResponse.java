package com.vehiclewallpaper.backend.admin;

public class AdminAuthOptionsResponse {

    private final boolean loginEnabled;
    private final boolean apiKeyEnabled;
    private final String loginUsernameHint;

    public AdminAuthOptionsResponse(boolean loginEnabled, boolean apiKeyEnabled, String loginUsernameHint) {
        this.loginEnabled = loginEnabled;
        this.apiKeyEnabled = apiKeyEnabled;
        this.loginUsernameHint = loginUsernameHint;
    }

    public boolean isLoginEnabled() {
        return loginEnabled;
    }

    public boolean isApiKeyEnabled() {
        return apiKeyEnabled;
    }

    public String getLoginUsernameHint() {
        return loginUsernameHint;
    }
}
