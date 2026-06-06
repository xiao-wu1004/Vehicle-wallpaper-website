package com.vehiclewallpaper.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.user.auth")
public class UserAuthProperties {

    private String tokenSecret = "";

    private long tokenTtlHours = 168;

    public String getTokenSecret() {
        return tokenSecret;
    }

    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    public long getTokenTtlHours() {
        return tokenTtlHours;
    }

    public void setTokenTtlHours(long tokenTtlHours) {
        this.tokenTtlHours = tokenTtlHours;
    }
}
