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

## 5. Non-Goals

The following are explicitly out of scope for this project:

- **Distributed Messaging**: This engine is not designed for multi-instance coordination or distributed message queuing across multiple application servers. Notifications are not shared or load-balanced across application instances.

- **Message Persistence**: Notifications are not persisted to disk or database. They exist only in memory and will be lost if the application is restarted or crashes.

- **Message Durability**: Unlike traditional message brokers, there is no guarantee that notifications will survive application restarts. The engine prioritizes simplicity and performance over durability guarantees.

- **Advanced Queue Features**: Features such as message priorities, delayed delivery, dead-letter queues, message routing, and topic-based subscriptions are not part of the core design. The engine focuses on simple FIFO (First-In-First-Out) processing.

- **External System Integration**: While notifications may call external services (e.g., email providers), the queue infrastructure itself does not integrate with external messaging systems like RabbitMQ, Kafka, or cloud message queues.

- **Transaction Management**: The engine does not provide transactional guarantees between notification production and consumption, or integration with distributed transaction managers.

- **Message Acknowledgment**: Unlike enterprise message brokers, there is no explicit acknowledgment mechanism. Messages are removed from the queue upon consumption.

- **Load Balancing Across Instances**: Each application instance maintains its own independent queue. There is no coordination or load balancing of notifications across multiple running instances of the application.

