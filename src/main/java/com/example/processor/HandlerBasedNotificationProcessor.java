package com.example.processor;

import com.example.handler.NotificationHandler;
import com.example.handler.NotificationHandlerRegistry;
import com.example.model.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Implementation of NotificationProcessor that uses NotificationHandlerRegistry
 * to delegate notification processing to appropriate handlers based on notification type.
 * 
 * This processor acts as a coordinator, selecting the correct handler for each
 * notification type and delegating the actual processing to that handler.
 */
@Component
@Primary
public class HandlerBasedNotificationProcessor implements NotificationProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(HandlerBasedNotificationProcessor.class);
    
    private final NotificationHandlerRegistry handlerRegistry;
    
    /**
     * Constructs a HandlerBasedNotificationProcessor with the handler registry.
     * 
     * @param handlerRegistry The registry for selecting notification handlers
     */
    public HandlerBasedNotificationProcessor(NotificationHandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }
    
    @Override
    public void process(NotificationEvent event) throws Exception {
        logger.debug("Processing notification event: id={}, type={}", 
                    event.getId(), event.getNotificationType());
        
        try {
            // Select the appropriate handler for the notification type
            NotificationHandler handler = handlerRegistry.getHandler(event.getNotificationType());
            
            // Delegate processing to the selected handler
            handler.handle(event);
            
            logger.debug("Successfully processed notification event: id={}", event.getId());
        } catch (IllegalArgumentException e) {
            logger.error("Failed to find handler for notification type: {}, event id: {}", 
                        event.getNotificationType(), event.getId(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error processing notification event: id={}, type={}", 
                        event.getId(), event.getNotificationType(), e);
            throw e;
        }
    }
}

