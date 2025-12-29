package com.example.service;

import com.example.dto.NotificationRequest;
import com.example.model.NotificationEvent;
import com.example.model.NotificationPriority;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for creating and submitting notification events.
 * 
 * This service converts DTOs to domain models and enqueues them for async processing.
 */
@Service
public class NotificationService {
    
    private final NotificationProducer notificationProducer;
    
    /**
     * Constructs a NotificationService with the notification producer.
     * 
     * @param notificationProducer The producer for enqueueing notifications
     */
    public NotificationService(NotificationProducer notificationProducer) {
        this.notificationProducer = notificationProducer;
    }
    
    /**
     * Creates a notification event from the request and submits it for async processing.
     * 
     * This method returns immediately after enqueueing the notification. The actual
     * processing happens asynchronously by consumer workers.
     * 
     * @param request The notification request
     * @return The created NotificationEvent with generated ID
     * @throws InterruptedException If interrupted while enqueueing
     */
    public NotificationEvent submitNotification(NotificationRequest request) throws InterruptedException {
        NotificationEvent event = createNotificationEvent(request);
        notificationProducer.produce(event);
        return event;
    }
    
    /**
     * Creates a NotificationEvent from a NotificationRequest.
     * 
     * @param request The notification request
     * @return A new NotificationEvent instance
     */
    private NotificationEvent createNotificationEvent(NotificationRequest request) {
        NotificationEvent event = new NotificationEvent();
        event.setId(UUID.randomUUID());
        event.setNotificationType(request.getNotificationType());
        event.setRecipient(request.getRecipient());
        event.setSubject(request.getSubject());
        event.setBody(request.getBody());
        event.setPriority(request.getPriority() != null ? request.getPriority() : NotificationPriority.NORMAL);
        event.setCreatedAt(LocalDateTime.now());
        event.setMetadata(request.getMetadata());
        return event;
    }
}

