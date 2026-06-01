package com.vehiclewallpaper.backend.admin;

public class AdminFeedbackUpdateRequest {

    private String status;

    private Boolean featured;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getFeatured() {
        return featured;
    }

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }
}
