package com.example.config;

import com.example.model.NotificationEvent;
import com.example.processor.NotificationProcessor;
import com.example.service.NotificationConsumerWorker;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Configuration class for the in-memory notification queue.
 * Creates a bounded BlockingQueue for async notification processing.
 */
@Configuration
@EnableConfigurationProperties(NotificationQueueProperties.class)
public class NotificationQueueConfig {
    
    /**
     * Creates a bounded BlockingQueue for NotificationEvent objects.
     * 
     * Uses LinkedBlockingQueue with a fixed capacity to:
     * - Prevent unbounded memory growth
     * - Provide backpressure when producers outpace consumers
     * - Ensure thread-safe operations for concurrent producers and consumers
     * 
     * @param properties Queue configuration properties
     * @return A bounded BlockingQueue for notification events
     */
    @Bean
    public BlockingQueue<NotificationEvent> notificationQueue(NotificationQueueProperties properties) {
        return new LinkedBlockingQueue<>(properties.getCapacity());
    }
    
    /**
     * Creates a NotificationConsumerWorker bean that starts consumer threads.
     * 
     * @param notificationQueue The notification queue
     * @param notificationProcessor The processor for handling notifications
     * @param properties Queue configuration properties
     * @return NotificationConsumerWorker instance
     */
    @Bean
    public NotificationConsumerWorker notificationConsumerWorker(
            BlockingQueue<NotificationEvent> notificationQueue,
            NotificationProcessor notificationProcessor,
            NotificationQueueProperties properties) {
        return new NotificationConsumerWorker(
            notificationQueue,
            notificationProcessor,
            properties.getConsumerThreads()
        );
    }
}

