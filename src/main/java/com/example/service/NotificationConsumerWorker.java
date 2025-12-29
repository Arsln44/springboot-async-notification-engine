package com.example.service;

import com.example.model.NotificationEvent;
import com.example.processor.NotificationProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker component that continuously polls the notification queue and processes events.
 * 
 * This component runs multiple consumer threads in the background, each continuously
 * polling the BlockingQueue for notification events and processing them through the
 * NotificationProcessor. The workers run in an infinite loop until shutdown is initiated.
 */
public class NotificationConsumerWorker {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationConsumerWorker.class);
    
    private final BlockingQueue<NotificationEvent> notificationQueue;
    private final NotificationProcessor notificationProcessor;
    private final int consumerThreads;
    private final ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    /**
     * Constructs a NotificationConsumerWorker with the queue, processor, and thread count.
     * 
     * @param notificationQueue The BlockingQueue containing notification events
     * @param notificationProcessor The processor for handling notification events
     * @param consumerThreads Number of consumer threads to run
     */
    public NotificationConsumerWorker(
            BlockingQueue<NotificationEvent> notificationQueue,
            NotificationProcessor notificationProcessor,
            int consumerThreads) {
        this.notificationQueue = notificationQueue;
        this.notificationProcessor = notificationProcessor;
        this.consumerThreads = consumerThreads;
        this.executorService = Executors.newFixedThreadPool(consumerThreads, r -> {
            Thread thread = new Thread(r, "notification-consumer");
            thread.setDaemon(false);
            return thread;
        });
        
        startConsumers();
    }
    
    /**
     * Starts all consumer worker threads.
     */
    private void startConsumers() {
        running.set(true);
        for (int i = 0; i < consumerThreads; i++) {
            final int workerId = i + 1;
            executorService.submit(() -> {
                logger.info("Consumer worker {} started", workerId);
                consumeLoop(workerId);
            });
        }
        logger.info("Started {} notification consumer worker threads", consumerThreads);
    }
    
    /**
     * Main consumption loop that continuously polls the queue and processes events.
     * 
     * This method runs in an infinite loop until the running flag is set to false
     * or the thread is interrupted. The loop uses blocking take() operation which
     * efficiently waits for events without consuming CPU resources.
     * 
     * @param workerId Identifier for this worker thread (for logging)
     */
    private void consumeLoop(int workerId) {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                // Blocking operation: waits until an event is available
                NotificationEvent event = notificationQueue.take();
                
                logger.debug("Consumer worker {} processing event: id={}, type={}", 
                           workerId, event.getId(), event.getNotificationType());
                
                try {
                    notificationProcessor.process(event);
                    logger.debug("Consumer worker {} successfully processed event: id={}", 
                               workerId, event.getId());
                } catch (Exception e) {
                    logger.error("Consumer worker {} failed to process event: id={}", 
                               workerId, event.getId(), e);
                    // Error is logged but processing continues for next event
                }
            } catch (InterruptedException e) {
                logger.info("Consumer worker {} interrupted, shutting down", workerId);
                Thread.currentThread().interrupt();
                break;
            }
        }
        logger.info("Consumer worker {} stopped", workerId);
    }
    
    /**
     * Gracefully shuts down all consumer workers.
     * 
     * This method:
     * 1. Sets the running flag to false to stop the consumption loop
     * 2. Shuts down the executor service
     * 3. Waits for in-progress tasks to complete (with timeout)
     * 4. Forcefully terminates if timeout is exceeded
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down notification consumer workers...");
        running.set(false);
        
        executorService.shutdown();
        try {
            // Wait for in-progress tasks to complete, with timeout
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("Consumer threads did not terminate within timeout, forcing shutdown");
                executorService.shutdownNow();
                // Wait again after shutdownNow
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.error("Consumer threads did not terminate after force shutdown");
                }
            }
            logger.info("All consumer workers stopped successfully");
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for consumer threads to terminate", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

