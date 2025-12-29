package com.example.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Domain model representing a notification event in the async notification system.
 * Contains only data fields with no business logic.
 */
public class NotificationEvent {
    
    private UUID id;
    private NotificationType notificationType;
    private String recipient;
    private String subject;
    private String body;
    private NotificationPriority priority;
    private LocalDateTime createdAt;
    private Map<String, Object> metadata;
    
    public NotificationEvent() {
    }
    
    public NotificationEvent(UUID id, NotificationType notificationType, String recipient, 
                            String subject, String body, NotificationPriority priority, 
                            LocalDateTime createdAt, Map<String, Object> metadata) {
        this.id = id;
        this.notificationType = notificationType;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.priority = priority;
        this.createdAt = createdAt;
        this.metadata = metadata;
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public NotificationType getNotificationType() {
        return notificationType;
    }
    
    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }
    
    public String getRecipient() {
        return recipient;
    }
    
    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    
    public NotificationPriority getPriority() {
        return priority;
    }
    
    public void setPriority(NotificationPriority priority) {
        this.priority = priority;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

