package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the notification queue.
 */
@ConfigurationProperties(prefix = "notification.queue")
public class NotificationQueueProperties {
    
    /**
     * Maximum number of notifications that can be held in the queue.
     * When the queue reaches this capacity, producers will block on put operations.
     */
    private int capacity = 1000;
    
    /**
     * Number of consumer worker threads that will process notifications concurrently.
     */
    private int consumerThreads = 2;
    
    public int getCapacity() {
        return capacity;
    }
    
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    public int getConsumerThreads() {
        return consumerThreads;
    }
    
    public void setConsumerThreads(int consumerThreads) {
        this.consumerThreads = consumerThreads;
    }
}

