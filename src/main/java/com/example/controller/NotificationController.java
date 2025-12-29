package com.example.controller;

import com.example.dto.NotificationRequest;
import com.example.dto.NotificationResponse;
import com.example.model.NotificationEvent;
import com.example.service.NotificationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * REST controller for submitting notification requests.
 * 
 * This controller accepts notification requests and returns immediately without
 * waiting for processing. Notifications are queued for asynchronous processing
 * by consumer workers.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    
    private final NotificationService notificationService;
    
    /**
     * Constructs a NotificationController with the notification service.
     * 
     * @param notificationService The service for submitting notifications
     */
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    /**
     * Submits a notification request for async processing.
     * 
     * This endpoint:
     * - Validates the incoming request
     * - Creates a notification event with a unique ID
     * - Enqueues the event for async processing
     * - Returns immediately with HTTP 202 Accepted
     * 
     * The actual notification delivery happens asynchronously by consumer workers.
     * The response includes the notification ID which can be used for tracking/logging.
     * 
     * @param request The notification request (validated automatically)
     * @return HTTP 202 Accepted with notification ID and status
     */
    @PostMapping
    public ResponseEntity<NotificationResponse> submitNotification(
            @Valid @RequestBody NotificationRequest request) {
        
        logger.debug("Received notification request: type={}, recipient={}", 
                    request.getNotificationType(), request.getRecipient());
        
        try {
            NotificationEvent event = notificationService.submitNotification(request);
            
            NotificationResponse response = new NotificationResponse(
                event.getId(),
                "ACCEPTED",
                "Notification submitted for processing",
                LocalDateTime.now()
            );
            
            logger.info("Notification submitted successfully: id={}, type={}, recipient={}", 
                       event.getId(), event.getNotificationType(), event.getRecipient());
            
            return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
                
        } catch (InterruptedException e) {
            logger.error("Interrupted while submitting notification", e);
            Thread.currentThread().interrupt();
            
            NotificationResponse errorResponse = new NotificationResponse(
                null,
                "ERROR",
                "Notification submission interrupted",
                LocalDateTime.now()
            );
            
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse);
        } catch (Exception e) {
            logger.error("Error submitting notification", e);
            
            NotificationResponse errorResponse = new NotificationResponse(
                null,
                "ERROR",
                "Failed to submit notification: " + e.getMessage(),
                LocalDateTime.now()
            );
            
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }
}

