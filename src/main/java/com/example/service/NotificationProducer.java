package com.example.service;

import com.example.model.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;

/**
 * Service responsible for producing and enqueueing notification events.
 * 
 * This service acts as the entry point for notifications into the async processing pipeline.
 * It accepts NotificationEvent objects and places them into the BlockingQueue for later
 * consumption by consumer workers. The producer does not process or deliver notifications;
 * it only handles the queueing operation.
 */
@Service
public class NotificationProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationProducer.class);
    
    private final BlockingQueue<NotificationEvent> notificationQueue;
    
    /**
     * Constructs a NotificationProducer with the notification queue.
     * 
     * @param notificationQueue The BlockingQueue for notification events
     */
    public NotificationProducer(BlockingQueue<NotificationEvent> notificationQueue) {
        this.notificationQueue = notificationQueue;
    }
    
    /**
     * Enqueues a notification event into the blocking queue for async processing.
     * 
     * This method uses the blocking put() operation, which will wait if the queue is full
     * until space becomes available. This provides natural backpressure when consumers
     * cannot keep up with producers.
     * 
     * @param event The notification event to enqueue
     * @throws InterruptedException If the current thread is interrupted while waiting
     *                              for space in the queue
     * @throws IllegalArgumentException If the event is null
     */
    public void produce(NotificationEvent event) throws InterruptedException {
        if (event == null) {
            throw new IllegalArgumentException("NotificationEvent cannot be null");
        }
        
        try {
            notificationQueue.put(event);
            logger.debug("Enqueued notification event: id={}, type={}, recipient={}", 
                        event.getId(), event.getNotificationType(), event.getRecipient());
        } catch (InterruptedException e) {
            logger.error("Interrupted while enqueueing notification event: id={}", event.getId(), e);
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while enqueueing notification event: id={}", 
                        event.getId(), e);
            throw new RuntimeException("Failed to enqueue notification event", e);
        }
    }
}

