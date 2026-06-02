package com.vehiclewallpaper.backend.admin;

public class AdminRequestMetadata {

    private final String requestPath;
    private final String ipAddress;
    private final String userAgent;

    public AdminRequestMetadata(String requestPath, String ipAddress, String userAgent) {
        this.requestPath = requestPath;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
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
}
