package com.vehiclewallpaper.backend.admin;

public class AdminActionStatusResponse {

    private final String message;
    private final int affectedCount;

    public AdminActionStatusResponse(String message, int affectedCount) {
        this.message = message;
        this.affectedCount = affectedCount;
    }

    public String getMessage() {
        return message;
    }

    public int getAffectedCount() {
        return affectedCount;
    }
}
