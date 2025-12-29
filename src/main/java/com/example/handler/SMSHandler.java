package com.example.handler;

import com.example.model.NotificationEvent;
import com.example.model.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of NotificationHandler for SMS notifications.
 * 
 * In a real system, this would integrate with an SMS gateway provider
 * (e.g., Twilio, AWS SNS, Vonage) to send actual SMS messages.
 */
@Component
public class SMSHandler implements NotificationHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(SMSHandler.class);
    
    @Override
    public boolean canHandle(NotificationType notificationType) {
        return NotificationType.SMS == notificationType;
    }
    
    @Override
    public void handle(NotificationEvent event) throws Exception {
        logger.info("Mock SMS Handler: Sending SMS to {} with message: {}", 
                   event.getRecipient(), event.getBody());
        
        // Mock implementation: Simulate SMS sending
        // In a real implementation, this would:
        // 1. Validate phone number format
        // 2. Check message length limits (typically 160 characters for SMS)
        // 3. Call SMS gateway API (e.g., Twilio, AWS SNS)
        // 4. Handle API response and errors
        // 5. Log delivery status and message ID
        
        // Simulate network delay (SMS is typically faster than email)
        Thread.sleep(100);
        
        logger.info("Mock SMS Handler: SMS sent successfully to {}", event.getRecipient());
    }
}

