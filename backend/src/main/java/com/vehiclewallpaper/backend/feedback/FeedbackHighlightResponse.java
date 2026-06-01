package com.vehiclewallpaper.backend.feedback;

import java.time.LocalDateTime;

public class FeedbackHighlightResponse {

    private final Long id;
    private final String name;
    private final String message;
    private final LocalDateTime submittedAt;

    public FeedbackHighlightResponse(Long id, String name, String message, LocalDateTime submittedAt) {
        this.id = id;
        this.name = name;
        this.message = message;
        this.submittedAt = submittedAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
}
