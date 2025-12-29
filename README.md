# CoreNotifyEngine

## 1. Project Purpose

CoreNotifyEngine is a lightweight, self-contained asynchronous notification engine built with Spring Boot. It implements an in-memory message queue using Java's `BlockingQueue` and follows the Producer-Consumer pattern to handle notification processing asynchronously within a single JVM instance.

The engine provides a simple, dependency-free solution for applications that need to decouple notification generation from notification delivery without introducing external messaging infrastructure. It allows producers (e.g., REST endpoints, scheduled tasks, event handlers) to submit notifications to a queue, while dedicated consumer threads process these notifications independently, enabling non-blocking and scalable notification handling.

## 2. Problems It Solves

- **Decoupled Processing**: Separates notification generation from notification delivery, preventing slow notification operations (e.g., email sending, SMS delivery, push notifications) from blocking main application threads.

- **Improved Responsiveness**: By processing notifications asynchronously, the engine ensures that API endpoints and business logic remain responsive even when notification channels experience delays or failures.

- **Resource Efficiency**: Utilizes thread pools and bounded queues to control resource consumption, preventing notification backlogs from overwhelming the system.

- **Simplicity**: Eliminates the need for external message brokers (like RabbitMQ, Kafka, or Redis), reducing operational complexity, deployment overhead, and infrastructure dependencies.

- **Fail-Safe Isolation**: Notification processing failures are isolated from the main application flow, allowing graceful error handling and retry mechanisms without impacting user-facing operations.

## 3. Why No External Broker Is Used

CoreNotifyEngine deliberately avoids external message brokers for several reasons:

- **Zero Infrastructure Overhead**: No need to deploy, configure, monitor, or maintain additional infrastructure components (message brokers, Redis clusters, etc.), reducing operational complexity and costs.

- **Lower Latency**: In-memory queues provide minimal latency compared to network-based messaging systems, making it ideal for single-application notification scenarios.

- **Deployment Simplicity**: The engine runs entirely within the application process, eliminating network dependencies, connection management, and broker availability concerns during deployment and scaling.

- **Cost Efficiency**: Avoids the licensing, hosting, and maintenance costs associated with external messaging infrastructure, making it suitable for smaller applications or environments with budget constraints.

- **Development Speed**: No need to set up development environments with message brokers, simplifying local development and CI/CD pipelines.

- **Use Case Alignment**: Designed for scenarios where notifications are consumed within the same application instance or where the benefits of distributed messaging (durability across application restarts, multi-instance coordination) are not required.

## 4. Core Components

At a high level, the engine consists of the following components:

- **Notification Queue**: A thread-safe `BlockingQueue` implementation that serves as the buffer between producers and consumers, providing thread-safe enqueue and dequeue operations with optional capacity limits.

- **Notification Producer Service**: A service layer that accepts notification requests from application code and enqueues them into the blocking queue. This component handles notification creation, validation, and queue submission.

- **Notification Consumer Workers**: Background thread workers (typically managed via Spring's `@Async` or `ExecutorService`) that continuously poll the queue for notifications and process them. Multiple consumer threads can operate in parallel to increase throughput.

- **Notification Processor**: The core business logic component responsible for executing the actual notification delivery (e.g., calling email services, SMS gateways, push notification APIs). This is where notification-specific processing and retry logic reside.

- **Configuration Management**: Spring Boot configuration for queue capacity, thread pool sizing, consumer thread count, and other tuning parameters, allowing the engine to be customized based on workload requirements.

- **Error Handling & Monitoring**: Mechanisms for handling processing failures, logging notification lifecycle events, and optionally integrating with monitoring systems to track queue depth, processing rates, and error rates.

## 6. NotificationEvent Domain Model

The `NotificationEvent` is the core domain model that represents a notification message as it flows through the async notification system. It is a simple data transfer object (DTO) containing only fields and enums, with no business logic.

### Field Definitions

The `NotificationEvent` model consists of the following fields:

- **`id` (UUID)**: A unique identifier for each notification event. This field enables tracking, logging, debugging, and correlation of notification lifecycle events across the system. Each notification receives a unique ID at creation time, allowing traceability from production through consumption.

- **`notificationType` (NotificationType enum)**: Specifies the channel through which the notification should be delivered. The enum supports multiple notification channels: `EMAIL`, `SMS`, `PUSH`, `IN_APP`, and `WEBHOOK`. This field determines which processor implementation will handle the notification, enabling the system to route notifications to appropriate delivery mechanisms.

- **`recipient` (String)**: The destination address or identifier for the notification. This can represent an email address, phone number, user ID, device token, webhook URL, or any other recipient identifier depending on the `notificationType`. This field provides the target destination for notification delivery.

- **`subject` (String)**: The title or subject line of the notification. Primarily used for email notifications and some push notification systems that support titles. This field enhances notification clarity and helps recipients quickly understand the notification's purpose.

- **`body` (String)**: The main content or message body of the notification. This contains the actual notification message text that will be displayed or sent to the recipient. This field carries the core informational payload of the notification.

- **`priority` (NotificationPriority enum)**: Indicates the importance level of the notification, with values `LOW`, `NORMAL`, `HIGH`, and `URGENT`. While the engine processes notifications in FIFO order, this field can be used for future prioritization features, monitoring, logging, and business logic decisions about notification handling.

- **`createdAt` (LocalDateTime)**: Timestamp indicating when the notification event was created. This field enables latency tracking, monitoring notification age in the queue, debugging timing issues, and implementing time-based business rules (e.g., notifications that are too old may be discarded).

- **`metadata` (Map<String, Object>)**: A flexible map for storing additional context-specific data that doesn't fit into the standard fields. This can include template variables, custom headers, retry counts, correlation IDs, user preferences, or any other extensible data needed by processors or business logic. This field provides extensibility without modifying the core model structure.

### System Flow

The `NotificationEvent` flows through the notification engine in the following stages:

1. **Creation**: Application code creates a `NotificationEvent` instance with all required fields populated (id, notificationType, recipient, subject, body, priority, createdAt, and optional metadata). The event is instantiated by the producer layer, typically triggered by business events (user registration, order confirmation, system alerts, etc.).

2. **Enqueue**: The populated `NotificationEvent` is submitted to the `NotificationProducerService`, which validates the event (if validation logic exists) and enqueues it into the `BlockingQueue<NotificationEvent>`. At this point, the event enters the async processing pipeline, and control returns immediately to the calling code.

3. **Queue Storage**: The event waits in the in-memory `BlockingQueue` until a consumer thread becomes available. The queue provides thread-safe storage and ensures that events are held until processing begins. Multiple events can be queued simultaneously, with the queue size controlled by configuration.

4. **Dequeue**: A `NotificationConsumerWorker` thread polls the queue and retrieves a `NotificationEvent` when available. This blocking operation ensures that consumer threads wait efficiently when the queue is empty, consuming no CPU resources until events arrive.

5. **Processing**: The consumer passes the `NotificationEvent` to the appropriate `NotificationProcessor` based on the `notificationType` field. The processor extracts relevant fields (recipient, subject, body, metadata) and executes the actual notification delivery (e.g., calling an email service API, SMS gateway, or push notification service).

6. **Completion/Discard**: After processing (successful or failed), the `NotificationEvent` is no longer referenced and becomes eligible for garbage collection. The in-memory nature of the system means events are not persisted, and their lifecycle ends after consumption.

Throughout this flow, the `NotificationEvent` remains immutable from the perspective of the queue system—it is created once, passed through the queue, and consumed once. Any modifications or enrichments would occur in the producer or processor layers, not within the queue infrastructure itself.

## 7. In-Memory Queue Configuration

The notification engine uses a bounded `BlockingQueue<NotificationEvent>` implemented via `LinkedBlockingQueue` as the core data structure for in-memory notification buffering. This configuration provides the foundation for asynchronous notification processing.

### Why Bounded Queue?

A bounded queue (with a fixed maximum capacity) is chosen over an unbounded queue for several critical reasons:

- **Memory Protection**: Prevents unbounded memory growth that could lead to `OutOfMemoryError` when notification production outpaces consumption. The fixed capacity provides a predictable upper bound on memory usage for queued notifications.

- **Backpressure Mechanism**: When the queue reaches its capacity, producer threads attempting to enqueue notifications will block, naturally applying backpressure. This prevents producers from overwhelming the system and gives consumers time to catch up, maintaining system stability.

- **Resource Awareness**: Forces system designers to consider queue sizing relative to expected workload and processing capacity, promoting conscious resource planning and capacity management.

- **Fail-Fast Behavior**: By blocking producers when the queue is full (rather than allowing unbounded growth), the system exposes capacity issues early, making problems visible rather than silently consuming memory until exhaustion.

- **Performance Optimization**: Bounded queues with appropriate sizing can improve throughput by maintaining optimal queue depth—too small causes excessive blocking, too large increases memory overhead without benefit. A well-sized bounded queue strikes a balance between producer and consumer efficiency.

### Queue Lifecycle

The queue lifecycle follows these stages:

1. **Initialization**: During Spring application context startup, the `NotificationQueueConfig` configuration class creates a `LinkedBlockingQueue<NotificationEvent>` instance with the configured capacity (default: 1000). The queue is registered as a Spring bean, making it available for dependency injection throughout the application.

2. **Active Operation**: Once initialized, the queue operates continuously throughout the application's lifetime. Producers enqueue `NotificationEvent` objects, and consumers dequeue them for processing. The queue maintains a FIFO (First-In-First-Out) ordering of notifications.

3. **Runtime State**: The queue can exist in three states:
   - **Empty**: No notifications are queued; consumers will block when attempting to dequeue.
   - **Partially Filled**: Contains some notifications but has remaining capacity; both producers and consumers can operate without blocking (assuming concurrent access).
   - **Full**: Reached maximum capacity; producers will block on enqueue operations until space becomes available.

4. **Shutdown**: When the application shuts down, the queue instance is discarded along with any remaining queued notifications. There is no persistence or graceful shutdown mechanism—all queued notifications are lost during application termination.

### What Happens When Queue is Full?

When the queue reaches its maximum capacity and a producer attempts to enqueue a notification, the behavior depends on the operation used:

- **Blocking Operations (`put()`)**: The producer thread blocks indefinitely until space becomes available in the queue. This is the default behavior that provides natural backpressure—producers wait, consuming no CPU resources, until a consumer removes a notification and frees space.

- **Non-Blocking Operations (`offer()`)**: If producers use `offer()` instead of `put()`, the operation returns `false` immediately when the queue is full, allowing producers to implement alternative strategies (e.g., logging, metrics, fallback handling, or retry logic) rather than blocking.

- **Timeout Operations (`offer(timeout)`)**: Producers can use `offer()` with a timeout, which will block for the specified duration. If space becomes available within the timeout, the notification is enqueued and `true` is returned. If the timeout expires, `false` is returned, allowing producers to handle the failure scenario.

The choice of operation (`put()` vs `offer()`) determines how backpressure is handled. Blocking operations (`put()`) ensure no notifications are lost but may slow down producers, while non-blocking operations allow producers to continue but may require explicit handling of rejected notifications.

### Thread-Safety Guarantees

The `LinkedBlockingQueue` implementation provides strong thread-safety guarantees:

- **Concurrent Access**: Multiple producer threads can safely enqueue notifications simultaneously without synchronization issues. Similarly, multiple consumer threads can safely dequeue notifications concurrently. The queue internally handles all synchronization.

- **Atomic Operations**: All queue operations (enqueue, dequeue, size checks) are atomic. A single notification cannot be partially enqueued or dequeued, preventing corruption or data inconsistency.

- **Visibility Guarantees**: The queue uses `volatile` variables and `java.util.concurrent.locks` internally to ensure that changes made by one thread are immediately visible to other threads, satisfying Java Memory Model requirements for concurrent access.

- **Happens-Before Relationships**: Enqueue and dequeue operations establish proper happens-before relationships, ensuring that if a producer enqueues a notification before a consumer starts dequeuing, the consumer will see that notification.

- **No External Synchronization Required**: Application code using the queue does not need to add external synchronization (e.g., `synchronized` blocks, explicit locks) when performing queue operations. The queue handles all thread-safety internally.

- **Lock-Free Where Possible**: `LinkedBlockingQueue` uses separate locks for enqueue and dequeue operations (two-lock queue algorithm), allowing producers and consumers to operate in parallel without contention, improving throughput in high-concurrency scenarios.

These guarantees ensure that the queue can be safely used in a multi-threaded environment where multiple producers and consumers operate concurrently without race conditions, data corruption, or other concurrency-related issues.

## 8. NotificationProducer Service

The `NotificationProducer` service is the entry point for submitting notifications into the async processing pipeline. It accepts `NotificationEvent` objects and enqueues them into the `BlockingQueue` for later consumption by consumer workers. The producer's responsibility is intentionally limited to queueing operations only—it does not process, validate, or deliver notifications.

### Why Producer Must Be Lightweight

The producer service must remain lightweight for several critical reasons:

- **Non-Blocking API Design**: The producer is typically called from user-facing code paths (e.g., REST controllers, event handlers, scheduled tasks). A lightweight producer that quickly enqueues notifications and returns ensures that these code paths remain responsive and do not block waiting for slow notification operations.

- **Scalability**: A lightweight producer can handle high throughput of notification submissions. If the producer were heavy (e.g., performing validation, transformation, or external service calls), it would become a bottleneck, limiting the system's ability to accept notifications at a high rate.

- **Separation of Concerns**: By keeping the producer simple and focused solely on queueing, the system maintains clear separation between notification submission (producer's responsibility) and notification processing (consumer's responsibility). This separation allows each component to be optimized and scaled independently.

- **Queue Backpressure Handling**: When the queue is full, the producer will block (if using `put()` operation). A lightweight producer minimizes the impact of this blocking—the thread is simply waiting, consuming minimal resources. A heavy producer with additional processing would waste more resources during blocking periods.

- **Low Latency**: User-facing operations that trigger notifications (e.g., user registration, order placement) should complete quickly. A lightweight producer ensures that the notification submission step adds minimal latency to these operations.

- **Resource Efficiency**: Heavy processing in the producer would consume resources (CPU, memory, thread time) that should be reserved for consumers. By keeping producers lightweight, resources are allocated where they matter most—in processing and delivering notifications.

### Error Handling Strategy

The `NotificationProducer` implements a focused error handling strategy:

- **Null Validation**: The producer validates that the `NotificationEvent` is not null before attempting to enqueue it, throwing an `IllegalArgumentException` immediately. This fails-fast approach prevents null events from propagating through the system and provides clear feedback to callers.

- **InterruptedException Handling**: When using the blocking `put()` operation, the producer properly handles `InterruptedException` by:
  - Logging the interruption for debugging and monitoring
  - Restoring the interrupt status on the thread (`Thread.currentThread().interrupt()`) to preserve the interrupted state for upper layers
  - Re-throwing the exception to allow callers to handle the interruption appropriately
  
  This ensures that thread interruption signals are not lost and can be handled by upstream code (e.g., application shutdown scenarios).

- **Unexpected Exception Wrapping**: For any other unexpected exceptions during enqueueing (which should be rare given the simplicity of the operation), the producer:
  - Logs the error with contextual information (event ID) for troubleshooting
  - Wraps the exception in a `RuntimeException` with a descriptive message
  - Allows the exception to propagate to callers for appropriate handling

- **No Retry Logic**: The producer does not implement retry logic for failed enqueue operations. Retries should be handled at a higher level (e.g., by the caller or through application-level retry mechanisms) if needed. This keeps the producer simple and avoids coupling it to retry policies.

- **Logging Strategy**: The producer uses appropriate log levels:
  - `DEBUG` for successful enqueueing (to avoid log noise in production)
  - `ERROR` for exceptions (to ensure failures are visible and actionable)

This error handling strategy balances simplicity with robustness, ensuring that errors are properly surfaced without adding complexity to the producer's core responsibility.

### Why This Layer Should Not Know Consumers

The `NotificationProducer` is deliberately designed to be unaware of consumers for several architectural reasons:

- **Loose Coupling**: By not knowing about consumers, the producer remains decoupled from consumption logic. This allows consumers to be added, removed, or modified without requiring changes to the producer code, promoting system flexibility and maintainability.

- **Single Responsibility**: The producer's single responsibility is accepting and enqueueing notifications. Adding knowledge of consumers would introduce additional concerns (e.g., consumer availability, processing status, consumer configuration) that belong in other layers of the system.

- **Queue as Abstraction**: The `BlockingQueue` serves as an abstraction boundary between producers and consumers. Producers interact only with the queue interface, not with consumer implementations. This abstraction allows the system to evolve—consumers can be refactored, replaced, or scaled without affecting producers.

- **Scalability Independence**: Producers and consumers can be scaled independently when they are decoupled. The number of producers can grow based on incoming request volume, while the number of consumers can be adjusted based on processing capacity needs, without either side needing to know about the other.

- **Testing Simplicity**: A producer that doesn't know about consumers is easier to test in isolation. Unit tests can focus solely on queueing behavior without needing to mock or configure consumer dependencies, leading to simpler and more maintainable test suites.

- **Flexibility in Consumer Implementation**: The system can support multiple consumer implementations (e.g., different processors for different notification types, parallel consumers, priority-based consumers) without the producer needing to be aware of these details. The queue handles the routing and buffering, allowing consumers to be implemented and evolved independently.

- **Future Extensibility**: Decoupling producers from consumers allows the system to evolve in ways that would be difficult if they were tightly coupled. For example, the system could later add multiple queues, queue routing logic, or even move to a distributed queue implementation without requiring producer changes.

This design follows the Producer-Consumer pattern's core principle: producers and consumers communicate only through the shared queue, never directly with each other. This principle ensures a clean separation of concerns and promotes a more maintainable and scalable architecture.

## 9. NotificationConsumer Worker

The `NotificationConsumerWorker` is responsible for continuously polling the `BlockingQueue` for notification events and processing them through the `NotificationProcessor`. Multiple consumer worker threads run concurrently in the background, each executing an infinite loop that polls the queue, retrieves events, and processes them asynchronously.

### Thread Model

The consumer worker uses a fixed-size thread pool (`ExecutorService`) to manage multiple consumer threads:

- **Thread Pool Creation**: An `ExecutorService` with a fixed number of threads (configurable via `notification.queue.consumer-threads`, default: 2) is created during initialization. Each thread in the pool runs a separate consumer worker loop.

- **Worker Thread Naming**: Consumer threads are named "notification-consumer" for easy identification in thread dumps, logging, and debugging. Threads are created as non-daemon threads to ensure they keep the JVM alive and can be properly shut down.

- **Independent Execution**: Each consumer thread operates independently, polling the queue and processing events concurrently. This parallel processing increases throughput, allowing multiple notifications to be processed simultaneously.

- **Blocking Queue Interaction**: All consumer threads share the same `BlockingQueue` instance. The queue's thread-safe implementation ensures that multiple threads can safely poll the queue concurrently without race conditions or data corruption. When a thread calls `take()`, it blocks until an event is available, efficiently waiting without consuming CPU resources.

- **Thread Lifecycle**: Consumer threads are started during Spring bean initialization (when the `NotificationConsumerWorker` bean is created) and continue running until the application shuts down. The threads are managed by the `ExecutorService`, which handles thread creation, lifecycle, and termination.

- **Resource Allocation**: Each consumer thread consumes minimal resources when waiting (blocked on `take()` operation). CPU usage is only consumed when actively processing notifications. This efficient resource usage allows multiple consumer threads to run without significant overhead.

### Why Infinite Loop is Safe Here

The consumer workers use an infinite `while` loop (`while (running.get() && !Thread.currentThread().isInterrupted())`) which is safe and appropriate for this use case:

- **Blocking Operations**: The loop contains a blocking operation (`notificationQueue.take()`) that suspends thread execution when the queue is empty. The thread consumes no CPU cycles while blocked, waiting efficiently for events to arrive. The infinite loop is not a busy-wait loop that would consume CPU resources.

- **Controlled Termination**: The loop is controlled by two conditions:
  - `running.get()`: An `AtomicBoolean` flag that can be set to `false` during graceful shutdown
  - `Thread.currentThread().isInterrupted()`: A check for thread interruption, which allows external termination signals (e.g., during application shutdown) to break the loop

- **Exception Handling**: The loop properly handles `InterruptedException`, which is thrown when the thread is interrupted while blocked on `take()`. When interrupted, the loop breaks, allowing the thread to exit cleanly. This ensures that shutdown signals (interruptions) are properly handled.

- **Application Lifetime Alignment**: The infinite loop aligns with the application's lifetime—consumer threads should run continuously while the application is running, processing notifications as they arrive. The loop naturally terminates when the application shuts down (via the shutdown mechanism), making an infinite loop the correct design pattern.

- **No Resource Leakage**: Unlike unbounded loops in other contexts, this loop does not cause resource leaks because:
  - Blocking operations release CPU resources
  - Threads are managed by `ExecutorService`, which can be shut down
  - The loop checks for termination conditions on each iteration
  - InterruptedException is properly handled, allowing clean exit

- **Standard Pattern**: Infinite loops with blocking operations are a standard pattern in producer-consumer systems, thread pools, and server applications. This is the idiomatic way to implement long-running worker threads that process items from a queue.

The infinite loop is safe because it blocks efficiently, checks termination conditions, handles interruptions properly, and aligns with the application's operational model of continuous processing.

### How Graceful Shutdown Should Work

Graceful shutdown ensures that consumer workers stop processing new events and complete in-progress work before the application terminates. The `NotificationConsumerWorker` implements graceful shutdown using Spring's `@PreDestroy` lifecycle hook:

1. **Shutdown Signal**: When Spring detects application shutdown (e.g., via SIGTERM signal, shutdown endpoint, or application context close), it calls the `@PreDestroy` annotated `shutdown()` method on the `NotificationConsumerWorker` bean.

2. **Running Flag Update**: The shutdown process first sets the `running` flag to `false` using `running.set(false)`. This causes consumer threads to exit their loops after completing the current iteration (once they check the condition `while (running.get() && ...)`).

3. **ExecutorService Shutdown**: The `ExecutorService.shutdown()` method is called, which:
   - Prevents new tasks from being submitted
   - Allows in-progress tasks (consumer loops) to complete
   - Does not forcibly terminate threads

4. **Graceful Wait**: The code waits for threads to terminate using `executorService.awaitTermination(30, TimeUnit.SECONDS)`. This gives consumer threads up to 30 seconds to:
   - Check the `running` flag
   - Complete any in-progress notification processing
   - Exit their loops and terminate

5. **Timeout Handling**: If threads do not terminate within the timeout:
   - `shutdownNow()` is called, which interrupts all running threads
   - This causes `InterruptedException` in threads blocked on `take()`, allowing them to exit
   - Another await with a shorter timeout (10 seconds) provides a final opportunity for threads to terminate

6. **Force Termination**: If threads still do not terminate after the force shutdown, an error is logged, but the shutdown process continues. In practice, threads should terminate quickly after interruption.

7. **Interruption Handling**: If the shutdown process itself is interrupted (unlikely but possible), it calls `shutdownNow()` to ensure threads are interrupted and restores the interrupt status on the current thread.

**Important Considerations**:

- **In-Progress Processing**: Notifications that are being processed when shutdown begins will complete their processing (assuming the processor logic handles the work quickly). Notifications waiting in the queue when shutdown begins will not be processed (they are lost, consistent with the in-memory, non-persistent design).

- **Shutdown Timeout**: The 30-second timeout should be sufficient for most notification processing, but may need adjustment based on typical processing times. Very long-running processor operations might be interrupted.

- **Thread Interruption**: The shutdown mechanism relies on thread interruption to wake threads blocked on `take()`. Processor implementations should avoid swallowing `InterruptedException` to ensure shutdown works correctly.

- **Application Context**: The graceful shutdown works in conjunction with Spring's application context lifecycle. When the context is closed, `@PreDestroy` methods are called, triggering the shutdown sequence.

This graceful shutdown mechanism ensures that the application can shut down cleanly, without leaving threads running or forcing immediate termination that might corrupt state or leave resources in an inconsistent condition.

## 10. Notification Handlers

The notification engine uses an interface-based handler architecture to process different types of notifications (Email, SMS, Push, etc.). This design provides extensibility, allowing new notification channels to be added without modifying existing code.

### Architecture Overview

The handler system consists of three key components:

- **`NotificationHandler` Interface**: Defines the contract for all notification handlers with two methods:
  - `canHandle(NotificationType)`: Determines if the handler supports a specific notification type
  - `handle(NotificationEvent)`: Processes and delivers the notification

- **Handler Implementations**: Concrete implementations for each notification channel:
  - `EmailHandler`: Handles email notifications (mock implementation)
  - `SMSHandler`: Handles SMS notifications (mock implementation)
  - `PushHandler`: Handles push notifications (mock implementation)
  - Additional handlers can be added by implementing the `NotificationHandler` interface

- **`NotificationHandlerRegistry`**: Manages handler registration and selection, providing handler lookup based on notification type

- **`HandlerBasedNotificationProcessor`**: Coordinates notification processing by selecting the appropriate handler via the registry and delegating processing to it

### Handler Selection Logic

The handler selection logic is implemented in the `NotificationHandlerRegistry.getHandler()` method and follows these steps:

1. **Cache Lookup**: First checks an in-memory cache (`ConcurrentHashMap`) for previously resolved handlers. This provides O(1) lookup performance for known notification types after the initial selection.

2. **Handler Iteration**: If not cached, iterates through all registered handlers (injected by Spring as a list of `NotificationHandler` beans). For each handler, calls `canHandle(notificationType)` to check if it supports the requested notification type.

3. **First Match Selection**: Returns the first handler that returns `true` from `canHandle()`. This allows handlers to be registered in priority order if needed, though typically each handler supports exactly one notification type.

4. **Cache Storage**: Once a handler is found, it is stored in the cache for future lookups, eliminating the need for iteration on subsequent requests for the same notification type.

5. **Error Handling**: If no handler is found for a given notification type, throws an `IllegalArgumentException` with a descriptive error message. This fails-fast approach ensures that unsupported notification types are detected immediately.

**Selection Algorithm Pseudocode**:
```
getHandler(notificationType):
  if cached: return cached handler
  for each registered handler:
    if handler.canHandle(notificationType):
      cache handler
      return handler
  throw IllegalArgumentException("No handler found")
```

### Design Principles

The handler architecture follows several key design principles:

- **Open/Closed Principle**: The system is open for extension (new handlers can be added) but closed for modification (existing handlers and the registry don't need changes when adding new handlers).

- **Single Responsibility**: Each handler is responsible for one notification channel, keeping implementations focused and maintainable.

- **Strategy Pattern**: The handler selection mechanism uses the Strategy pattern, allowing the algorithm for selecting handlers to vary independently from the code that uses them.

- **Dependency Injection**: Handlers are automatically discovered and registered via Spring's dependency injection. New handlers are automatically included in the registry without configuration changes.

- **Loose Coupling**: Handlers don't know about each other or about the registry's implementation details. They only need to implement the `NotificationHandler` interface.

### Extensibility

Adding a new notification handler is straightforward and requires no changes to existing code:

1. **Create Handler Implementation**: Implement the `NotificationHandler` interface:
   ```java
   @Component
   public class NewChannelHandler implements NotificationHandler {
       @Override
       public boolean canHandle(NotificationType type) {
           return NotificationType.NEW_CHANNEL == type;
       }
       
       @Override
       public void handle(NotificationEvent event) throws Exception {
           // Implementation logic
       }
   }
   ```

2. **Add Notification Type**: If needed, add a new value to the `NotificationType` enum.

3. **Automatic Registration**: Spring automatically discovers the new handler bean and includes it in the registry. No configuration changes are required.

4. **Handler Selection**: The registry automatically includes the new handler in its selection logic. Notifications of the new type will be routed to the new handler.

This extensibility makes it easy to add support for new notification channels (e.g., Slack, Discord, Microsoft Teams) without modifying existing handlers, the registry, or the processor.

### Mock Implementations

The current handler implementations (EmailHandler, SMSHandler, PushHandler) are mock implementations designed for demonstration and testing:

- **EmailHandler**: Simulates email sending with logging and a simulated delay (150ms). In production, this would integrate with email service providers like SendGrid, AWS SES, or Mailgun.

- **SMSHandler**: Simulates SMS sending with logging and a simulated delay (100ms). In production, this would integrate with SMS gateway providers like Twilio, AWS SNS, or Vonage.

- **PushHandler**: Simulates push notification sending with logging and a simulated delay (120ms). In production, this would integrate with push notification services like Firebase Cloud Messaging, Apple Push Notification Service, or OneSignal.

These mock implementations allow the system to be tested and demonstrated without requiring external service integrations, API keys, or network connectivity. Replacing them with real implementations is straightforward: update the `handle()` method to call the actual service API while maintaining the same interface contract.

### Error Handling

Each handler is responsible for handling errors specific to its notification channel:

- **Handler-Level Errors**: If a handler's `handle()` method throws an exception, the exception propagates to the `NotificationConsumerWorker`, which logs the error and continues processing the next notification. This ensures that one failed notification doesn't stop the entire consumer thread.

- **Selection Errors**: If no handler is found for a notification type, an `IllegalArgumentException` is thrown, which is logged as an error. This typically indicates a configuration issue (e.g., a notification type was added but no handler was implemented).

- **Channel-Specific Errors**: Handlers can throw channel-specific exceptions (e.g., `EmailDeliveryException`, `SMSGatewayException`). The processor and consumer layers handle these generically, logging them appropriately. For production systems, handlers might implement retry logic, dead-letter queues, or fallback mechanisms.

## 11. Non-Goals

The following are explicitly out of scope for this project:

- **Distributed Messaging**: This engine is not designed for multi-instance coordination or distributed message queuing across multiple application servers. Notifications are not shared or load-balanced across application instances.

- **Message Persistence**: Notifications are not persisted to disk or database. They exist only in memory and will be lost if the application is restarted or crashes.

- **Message Durability**: Unlike traditional message brokers, there is no guarantee that notifications will survive application restarts. The engine prioritizes simplicity and performance over durability guarantees.

- **Advanced Queue Features**: Features such as message priorities, delayed delivery, dead-letter queues, message routing, and topic-based subscriptions are not part of the core design. The engine focuses on simple FIFO (First-In-First-Out) processing.

- **External System Integration**: While notifications may call external services (e.g., email providers), the queue infrastructure itself does not integrate with external messaging systems like RabbitMQ, Kafka, or cloud message queues.

- **Transaction Management**: The engine does not provide transactional guarantees between notification production and consumption, or integration with distributed transaction managers.

- **Message Acknowledgment**: Unlike enterprise message brokers, there is no explicit acknowledgment mechanism. Messages are removed from the queue upon consumption.

- **Load Balancing Across Instances**: Each application instance maintains its own independent queue. There is no coordination or load balancing of notifications across multiple running instances of the application.

