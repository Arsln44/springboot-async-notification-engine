package com.example.processor;

import com.example.model.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder implementation of NotificationProcessor that logs notification events.
 * 
 * This is a simple implementation for demonstration purposes. In a real system,
 * this would be replaced with actual notification delivery logic (e.g., email sending,
 * SMS delivery, push notification APIs).
 */
@Component
public class LoggingNotificationProcessor implements NotificationProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingNotificationProcessor.class);
    
    @Override
    public void process(NotificationEvent event) throws Exception {
        logger.info("Processing notification - id: {}, type: {}, recipient: {}, subject: {}", 
                   event.getId(), event.getNotificationType(), event.getRecipient(), event.getSubject());
        
        // Placeholder: In a real implementation, this would:
        // - Call email service API for EMAIL type
        // - Call SMS gateway for SMS type
        // - Send push notification for PUSH type
        // - etc.
        
        // Simulate processing delay
        Thread.sleep(100);
    }
}

