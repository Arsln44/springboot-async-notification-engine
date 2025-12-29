package com.example.processor;

import com.example.model.NotificationEvent;

/**
 * Interface for processing notification events.
 * 
 * Implementations of this interface handle the actual delivery of notifications
 * (e.g., sending emails, SMS, push notifications, webhooks).
 */
public interface NotificationProcessor {
    
    /**
     * Processes a notification event and delivers it through the appropriate channel.
     * 
     * @param event The notification event to process
     * @throws Exception If processing fails (implementation-specific exceptions)
     */
    void process(NotificationEvent event) throws Exception;
}

