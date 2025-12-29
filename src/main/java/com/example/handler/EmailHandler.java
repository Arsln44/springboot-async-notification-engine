package com.example.handler;

import com.example.model.NotificationEvent;
import com.example.model.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of NotificationHandler for email notifications.
 * 
 * In a real system, this would integrate with an email service provider
 * (e.g., SendGrid, AWS SES, Mailgun) to send actual emails.
 */
@Component
public class EmailHandler implements NotificationHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailHandler.class);
    
    @Override
    public boolean canHandle(NotificationType notificationType) {
        return NotificationType.EMAIL == notificationType;
    }
    
    @Override
    public void handle(NotificationEvent event) throws Exception {
        logger.info("Mock Email Handler: Sending email to {} with subject: {}", 
                   event.getRecipient(), event.getSubject());
        logger.debug("Email body: {}", event.getBody());
        
        // Mock implementation: Simulate email sending
        // In a real implementation, this would:
        // 1. Validate email address format
        // 2. Construct email message (from, to, subject, body, attachments if any)
        // 3. Call email service API (e.g., SendGrid, AWS SES)
        // 4. Handle API response and errors
        // 5. Log delivery status
        
        // Simulate network delay
        Thread.sleep(150);
        
        logger.info("Mock Email Handler: Email sent successfully to {}", event.getRecipient());
    }
}

