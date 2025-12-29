package com.example.handler;

import com.example.model.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing and selecting notification handlers based on notification type.
 * 
 * This component provides handler selection logic using a strategy pattern approach.
 * It maintains a registry of available handlers and selects the appropriate handler
 * for each notification type at runtime.
 */
@Component
public class NotificationHandlerRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationHandlerRegistry.class);
    
    private final List<NotificationHandler> handlers;
    private final Map<NotificationType, NotificationHandler> handlerCache;
    
    /**
     * Constructs a NotificationHandlerRegistry with the list of available handlers.
     * 
     * Spring will automatically inject all NotificationHandler implementations
     * as a list, allowing new handlers to be added without modifying this class.
     * 
     * @param handlers List of all available notification handlers
     */
    public NotificationHandlerRegistry(List<NotificationHandler> handlers) {
        this.handlers = handlers != null ? new ArrayList<>(handlers) : new ArrayList<>();
        this.handlerCache = new ConcurrentHashMap<>();
        logger.info("Initialized NotificationHandlerRegistry with {} handler(s)", this.handlers.size());
    }
    
    /**
     * Selects the appropriate handler for the given notification type.
     * 
     * Selection logic:
     * 1. First checks the handler cache for fast lookup
     * 2. If not cached, iterates through all registered handlers
     * 3. Returns the first handler that can handle the notification type
     * 4. Caches the result for future lookups
     * 
     * @param notificationType The type of notification
     * @return The handler that can process the notification type
     * @throws IllegalArgumentException If no handler is found for the notification type
     */
    public NotificationHandler getHandler(NotificationType notificationType) {
        if (notificationType == null) {
            throw new IllegalArgumentException("NotificationType cannot be null");
        }
        
        // Fast path: Check cache first
        NotificationHandler cachedHandler = handlerCache.get(notificationType);
        if (cachedHandler != null) {
            return cachedHandler;
        }
        
        // Slow path: Find handler by iterating through registered handlers
        for (NotificationHandler handler : handlers) {
            if (handler.canHandle(notificationType)) {
                // Cache the handler for future lookups
                handlerCache.put(notificationType, handler);
                logger.debug("Selected handler {} for notification type {}", 
                           handler.getClass().getSimpleName(), notificationType);
                return handler;
            }
        }
        
        // No handler found
        throw new IllegalArgumentException(
            String.format("No handler found for notification type: %s", notificationType)
        );
    }
}

