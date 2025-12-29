package com.example.dto;

import com.example.model.NotificationPriority;
import com.example.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * DTO for notification creation requests.
 */
public class NotificationRequest {
    
    @NotNull(message = "Notification type is required")
    private NotificationType notificationType;
    
    @NotBlank(message = "Recipient is required")
    private String recipient;
    
    private String subject;
    
    @NotBlank(message = "Body is required")
    private String body;
    
    private NotificationPriority priority;
    
    private Map<String, Object> metadata;
    
    public NotificationRequest() {
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
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

