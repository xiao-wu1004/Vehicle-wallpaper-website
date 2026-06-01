package com.vehiclewallpaper.backend.admin;

import java.time.LocalDateTime;

public class AdminFeedbackResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final String message;
    private final String status;
    private final boolean featured;
    private final String sourcePage;
    private final String userAgent;
    private final LocalDateTime createdAt;

    public AdminFeedbackResponse(Long id,
                                 String name,
                                 String email,
                                 String message,
                                 String status,
                                 boolean featured,
                                 String sourcePage,
                                 String userAgent,
                                 LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.message = message;
        this.status = status;
        this.featured = featured;
        this.sourcePage = sourcePage;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public boolean isFeatured() {
        return featured;
    }

    public String getSourcePage() {
        return sourcePage;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
