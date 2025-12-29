package com.example.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for notification submission responses.
 */
public class NotificationResponse {
    
    private UUID id;
    private String status;
    private String message;
    private LocalDateTime submittedAt;
    
    public NotificationResponse() {
    }
    
    public NotificationResponse(UUID id, String status, String message, LocalDateTime submittedAt) {
        this.id = id;
        this.status = status;
        this.message = message;
        this.submittedAt = submittedAt;
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
    
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}

