package com.example.handler;

import com.example.model.NotificationEvent;

/**
 * Interface for notification handlers that process and deliver notifications
 * through specific channels (e.g., email, SMS, push notifications).
 * 
 * This interface provides an extensible design where new notification channels
 * can be added by implementing this interface without modifying existing code.
 */
public interface NotificationHandler {
    
    /**
     * Checks if this handler can process the given notification type.
     * 
     * @param notificationType The type of notification
     * @return true if this handler supports the notification type, false otherwise
     */
    boolean canHandle(com.example.model.NotificationType notificationType);
    
    /**
     * Processes and delivers a notification event through the handler's channel.
     * 
     * @param event The notification event to process
     * @throws Exception If processing or delivery fails (implementation-specific exceptions)
     */
    void handle(NotificationEvent event) throws Exception;
}

