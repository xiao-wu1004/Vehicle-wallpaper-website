package com.vehiclewallpaper.backend.feedback;

import java.time.LocalDateTime;

public class FeedbackSubmissionResponse {

    private final Long id;
    private final String status;
    private final String message;
    private final LocalDateTime submittedAt;

    public FeedbackSubmissionResponse(Long id, String status, String message, LocalDateTime submittedAt) {
        this.id = id;
        this.status = status;
        this.message = message;
        this.submittedAt = submittedAt;
    }

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
}
