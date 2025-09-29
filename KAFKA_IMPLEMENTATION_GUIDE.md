# Kafka Implementation Guide for Spring Boot Chat Application

## Overview and Current State

**Important Note:** This chat application currently uses **Redis pub-sub** for real-time messaging but has been prepared for **Kafka integration**. The Kafka configuration exists but is not actively implemented in the codebase yet. This guide explains both the current state and how to implement Kafka.

---

## Part 1: Overview - Why Kafka in Chat Applications

### What is Kafka?

Think of Kafka as a **super-powered message queue** that can handle millions of messages per second. Unlike Redis pub-sub which is great for simple real-time messaging, Kafka is designed for:

1. **High-throughput messaging** (millions of messages/second)
2. **Persistent message storage** (messages are stored on disk)
3. **Horizontal scalability** (can add more servers)
4. **Fault tolerance** (data replication across servers)

### Why Kafka for Chat Applications?

```
Traditional Approach (Current - Redis):
User A → Spring Boot App → Redis Pub-Sub → Spring Boot App → User B

With Kafka:
User A → Spring Boot App → Kafka Topic → Multiple Spring Boot Instances → Users B, C, D...
```

**Benefits:**

1. **Scalability**: Handle thousands of concurrent users across multiple server instances
2. **Durability**: Messages are persisted to disk (Redis pub-sub messages are lost if Redis goes down)
3. **Multi-server messaging**: Perfect for load-balanced environments
4. **Message replay**: Can replay messages from any point in time
5. **Analytics**: Easy to add analytics consumers without affecting chat performance

### Kafka vs Redis Pub-Sub Comparison

| Feature                   | Redis Pub-Sub              | Kafka                       |
| ------------------------- | -------------------------- | --------------------------- |
| **Speed**                 | Very fast (memory-based)   | Fast (disk + memory)        |
| **Persistence**           | No persistence             | Persistent storage          |
| **Scalability**           | Limited to single instance | Horizontal scaling          |
| **Message Delivery**      | At most once               | At least once, exactly once |
| **Backpressure Handling** | Limited                    | Excellent                   |
| **Learning Curve**        | Simple                     | Moderate                    |

---

## Part 2: Current Kafka Configuration Analysis

### 2.1 Configuration Location

The Kafka configuration is in `application.properties`:

```properties
# ===== KAFKA CONFIGURATION =====
# Note: Currently using Redis pub-sub, Kafka config for future scalability
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=chatapp-group
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer
```

### 2.2 Configuration Properties Explained

| Property             | What It Does         | Beginner Explanation                                          |
| -------------------- | -------------------- | ------------------------------------------------------------- |
| `bootstrap-servers`  | `localhost:9092`     | Like Redis host:port, tells Spring where to find Kafka        |
| `consumer.group-id`  | `chatapp-group`      | Like a team name - consumers with same group-id work together |
| `auto-offset-reset`  | `earliest`           | When starting fresh, read messages from the beginning         |
| `key-deserializer`   | `StringDeserializer` | How to convert message keys from bytes to String              |
| `value-deserializer` | `StringDeserializer` | How to convert message content from bytes to String           |
| `key-serializer`     | `StringSerializer`   | How to convert String keys to bytes for sending               |
| `value-serializer`   | `StringSerializer`   | How to convert String content to bytes for sending            |

### 2.3 Maven Dependencies

The project already includes Kafka dependency:

```xml
<!-- Spring Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

---

## Part 3: Implementing Kafka Producer (Sending Messages)

### 3.1 Create Kafka Configuration Class

First, create a proper Kafka configuration class:

```java
// src/main/java/org/arkadipta/chatapp/config/KafkaConfig.java
package org.arkadipta.chatapp.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@ConditionalOnProperty(name = "messaging.type", havingValue = "kafka")
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // Topic names as constants
    public static final String CHAT_MESSAGES_TOPIC = "chat-messages";
    public static final String USER_STATUS_TOPIC = "user-status";
    public static final String TYPING_INDICATOR_TOPIC = "typing-indicator";

    // ===== PRODUCER CONFIGURATION =====

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Performance optimizations
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);  // 16KB batches
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);       // Wait 5ms for batching
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); // Compression

        // Reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, "1");          // Wait for leader ack
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);         // Retry failed sends

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ===== CONSUMER CONFIGURATION =====

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Performance settings
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);    // Process 100 messages at once
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);    // Wait for 1KB before fetch

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // Concurrency: number of consumer threads
        factory.setConcurrency(3); // 3 threads processing messages

        // Enable manual acknowledgment for better control
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }

    // ===== TOPIC CREATION =====

    @Bean
    public NewTopic chatMessagesTopic() {
        return new NewTopic(CHAT_MESSAGES_TOPIC, 3, (short) 1);  // 3 partitions, 1 replica
    }

    @Bean
    public NewTopic userStatusTopic() {
        return new NewTopic(USER_STATUS_TOPIC, 2, (short) 1);    // 2 partitions, 1 replica
    }

    @Bean
    public NewTopic typingIndicatorTopic() {
        return new NewTopic(TYPING_INDICATOR_TOPIC, 2, (short) 1); // 2 partitions, 1 replica
    }
}
```

### 3.2 Create Kafka Message Producer Service

```java
// src/main/java/org/arkadipta/chatapp/service/KafkaMessageProducer.java
package org.arkadipta.chatapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arkadipta.chatapp.config.KafkaConfig;
import org.arkadipta.chatapp.dto.chat.MessageResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka Message Producer Service
 * Handles sending messages to Kafka topics for distributed chat messaging
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "messaging.type", havingValue = "kafka")
public class KafkaMessageProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Send chat message to Kafka topic
     *
     * @param chatRoomId - Used as message key for partitioning
     * @param message - The chat message to send
     */
    public void sendChatMessage(String chatRoomId, MessageResponse message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);

            // Use chatRoomId as key - ensures all messages for same room go to same partition
            // This maintains message ordering per chat room
            CompletableFuture<SendResult<String, String>> future =
                kafkaTemplate.send(KafkaConfig.CHAT_MESSAGES_TOPIC, chatRoomId, messageJson);

            // Handle success/failure asynchronously
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    log.debug("Message sent successfully to topic {} with offset {}",
                        KafkaConfig.CHAT_MESSAGES_TOPIC, result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send message to topic {}: {}",
                        KafkaConfig.CHAT_MESSAGES_TOPIC, exception.getMessage());

                    // TODO: Implement retry logic or dead letter queue
                    handleSendFailure(chatRoomId, message, exception);
                }
            });

        } catch (Exception e) {
            log.error("Error serializing message for room {}: {}", chatRoomId, e.getMessage());
        }
    }

    /**
     * Send user status update (online/offline)
     */
    public void sendUserStatusUpdate(String userId, String status) {
        try {
            String statusMessage = String.format("{\"userId\":\"%s\",\"status\":\"%s\",\"timestamp\":%d}",
                userId, status, System.currentTimeMillis());

            kafkaTemplate.send(KafkaConfig.USER_STATUS_TOPIC, userId, statusMessage);
            log.debug("User status update sent for user: {}, status: {}", userId, status);

        } catch (Exception e) {
            log.error("Error sending user status update: {}", e.getMessage());
        }
    }

    /**
     * Send typing indicator
     */
    public void sendTypingIndicator(String chatRoomId, String userId, boolean isTyping) {
        try {
            String typingMessage = String.format(
                "{\"chatRoomId\":\"%s\",\"userId\":\"%s\",\"isTyping\":%b,\"timestamp\":%d}",
                chatRoomId, userId, isTyping, System.currentTimeMillis());

            kafkaTemplate.send(KafkaConfig.TYPING_INDICATOR_TOPIC, chatRoomId, typingMessage);

        } catch (Exception e) {
            log.error("Error sending typing indicator: {}", e.getMessage());
        }
    }

    /**
     * Handle message send failures
     * In production, you might want to:
     * 1. Retry with exponential backoff
     * 2. Send to dead letter queue
     * 3. Alert monitoring system
     */
    private void handleSendFailure(String chatRoomId, MessageResponse message, Throwable exception) {
        // For now, just log. In production, implement retry logic
        log.error("Message send failed for room {}, message ID {}: {}",
            chatRoomId, message.getId(), exception.getMessage());

        // TODO: Implement retry mechanism
        // TODO: Store failed messages for later processing
        // TODO: Alert monitoring system
    }
}
```

---

## Part 4: Implementing Kafka Consumer (Receiving Messages)

### 4.1 Create Kafka Message Consumer Service

```java
// src/main/java/org/arkadipta/chatapp/service/KafkaMessageConsumer.java
package org.arkadipta.chatapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arkadipta.chatapp.config.KafkaConfig;
import org.arkadipta.chatapp.dto.chat.MessageResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka Message Consumer Service
 * Listens to Kafka topics and forwards messages to WebSocket clients
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "messaging.type", havingValue = "kafka")
public class KafkaMessageConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Listen to chat messages from Kafka and forward to WebSocket clients
     *
     * How this works:
     * 1. Kafka consumer receives message from topic
     * 2. Message is deserialized from JSON
     * 3. Message is sent to WebSocket topic for that chat room
     * 4. All clients subscribed to that room receive the message
     */
    @KafkaListener(
        topics = KafkaConfig.CHAT_MESSAGES_TOPIC,
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeChatMessage(
            @Payload String messageJson,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String chatRoomId,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received message from topic: {}, partition: {}, offset: {}, key: {}",
                topic, partition, offset, chatRoomId);

            // Deserialize the message
            MessageResponse message = objectMapper.readValue(messageJson, MessageResponse.class);

            // Forward to WebSocket clients subscribed to this chat room
            String destination = "/topic/chatroom/" + chatRoomId;
            messagingTemplate.convertAndSend(destination, message);

            log.debug("Message forwarded to WebSocket destination: {}", destination);

            // Manually acknowledge message processing
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing chat message: {}", e.getMessage());

            // In production, you might want to:
            // 1. Send to dead letter queue
            // 2. Retry with exponential backoff
            // 3. Alert monitoring system

            // For now, acknowledge to prevent infinite retries
            acknowledgment.acknowledge();
        }
    }

    /**
     * Listen to user status updates
     */
    @KafkaListener(
        topics = KafkaConfig.USER_STATUS_TOPIC,
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeUserStatusUpdate(
            @Payload String statusJson,
            @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String userId,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received user status update for user: {}", userId);

            // Forward user status to all connected clients
            messagingTemplate.convertAndSend("/topic/user-status", statusJson);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing user status update: {}", e.getMessage());
            acknowledgment.acknowledge();
        }
    }

    /**
     * Listen to typing indicators
     */
    @KafkaListener(
        topics = KafkaConfig.TYPING_INDICATOR_TOPIC,
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeTypingIndicator(
            @Payload String typingJson,
            @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String chatRoomId,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received typing indicator for room: {}", chatRoomId);

            // Forward typing indicator to room subscribers
            String destination = "/topic/chatroom/" + chatRoomId + "/typing";
            messagingTemplate.convertAndSend(destination, typingJson);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing typing indicator: {}", e.getMessage());
            acknowledgment.acknowledge();
        }
    }
}
```

---

## Part 5: Integration with Existing Chat Application

### 5.1 Modify WebSocketChatController to Use Kafka

Update the existing `WebSocketChatController` to optionally use Kafka:

```java
// Add to src/main/java/org/arkadipta/chatapp/controller/WebSocketChatController.java

@Autowired(required = false)  // Optional dependency
private KafkaMessageProducer kafkaMessageProducer;

@Autowired(required = false)  // Optional dependency
private RedisMessagePublisher redisMessagePublisher;

// In your existing sendMessage method, replace the Redis call with:
private void broadcastMessage(MessageResponse messageResponse) {
    if (kafkaMessageProducer != null) {
        // Use Kafka for broadcasting
        kafkaMessageProducer.sendChatMessage(
            messageResponse.getChatRoomId().toString(),
            messageResponse
        );
    } else if (redisMessagePublisher != null) {
        // Fallback to Redis
        redisMessagePublisher.publishMessage(messageResponse);
    } else {
        // Direct WebSocket send (single instance only)
        messagingTemplate.convertAndSend(
            "/topic/chatroom/" + messageResponse.getChatRoomId(),
            messageResponse
        );
    }
}

// For typing indicators
private void broadcastTypingIndicator(String chatRoomId, String userId, boolean isTyping) {
    if (kafkaMessageProducer != null) {
        kafkaMessageProducer.sendTypingIndicator(chatRoomId, userId, isTyping);
    } else {
        // Direct WebSocket send
        TypingIndicator indicator = new TypingIndicator(userId, isTyping);
        messagingTemplate.convertAndSend("/topic/chatroom/" + chatRoomId + "/typing", indicator);
    }
}
```

### 5.2 Configuration Switch

Add to `application.properties` to control which messaging system to use:

```properties
# Choose messaging system: "redis" or "kafka"
messaging.type=redis

# For Kafka mode, set to "kafka":
# messaging.type=kafka
```

### 5.3 End-to-End Message Flow with Kafka

```
1. User sends message via WebSocket
   ↓
2. WebSocketChatController receives message
   ↓
3. Message is saved to database (PostgreSQL)
   ↓
4. Message is sent to Kafka topic (chat-messages)
   ↓
5. Kafka distributes message to all consumer instances
   ↓
6. Each KafkaMessageConsumer receives the message
   ↓
7. Each consumer forwards message to its WebSocket clients
   ↓
8. Users connected to that instance receive the message
```

**Key Benefits:**

- **Horizontal Scaling**: Add more Spring Boot instances, they automatically join the Kafka consumer group
- **Load Distribution**: Kafka partitions distribute load across consumer instances
- **Reliability**: Messages are persisted in Kafka until processed
- **Ordering**: Messages for the same chat room stay in order (same partition)

---

## Part 6: Debugging and Best Practices

### 6.1 Common Kafka Issues and Solutions

#### Issue 1: "No available brokers"

**Error:** `org.apache.kafka.common.errors.TimeoutException`

**Solution:**

```bash
# Check if Kafka is running
sudo systemctl status kafka

# Start Kafka (if using systemd)
sudo systemctl start kafka

# Or start manually
cd /path/to/kafka
bin/kafka-server-start.sh config/server.properties
```

#### Issue 2: Consumer group not receiving messages

**Problem:** Messages are sent but not received

**Debug Steps:**

```java
// Add to KafkaMessageProducer
@EventListener
public void handleSendResult(ProducerRecord<String, String> record, RecordMetadata metadata) {
    log.info("Message sent to topic: {}, partition: {}, offset: {}",
        metadata.topic(), metadata.partition(), metadata.offset());
}

// Check consumer group status
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group chatapp-group
```

#### Issue 3: Message serialization errors

**Error:** `SerializationException`

**Solution:**

```java
// Use proper JSON serialization
@Bean
public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(props);
}
```

### 6.2 Performance Tuning for Hackathons

#### For High-Throughput Chat (1000+ concurrent users):

```properties
# Producer optimizations
spring.kafka.producer.batch-size=32768
spring.kafka.producer.linger-ms=10
spring.kafka.producer.compression-type=snappy
spring.kafka.producer.buffer-memory=67108864

# Consumer optimizations
spring.kafka.consumer.max-poll-records=500
spring.kafka.consumer.fetch-min-bytes=50000
spring.kafka.consumer.fetch-max-wait=500
```

#### Consumer Concurrency:

```java
// In KafkaConfig
@Bean
public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
    factory.setConcurrency(Runtime.getRuntime().availableProcessors()); // Use all CPU cores
    return factory;
}
```

### 6.3 Testing Kafka Integration

#### Unit Test Example:

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"chat-messages"})
class KafkaMessageProducerTest {

    @Autowired
    private KafkaMessageProducer producer;

    @Test
    void testSendMessage() {
        MessageResponse message = MessageResponse.builder()
            .id(1L)
            .content("Test message")
            .chatRoomId(1L)
            .build();

        producer.sendChatMessage("1", message);

        // Verify message was sent (use TestContainers for integration tests)
    }
}
```

### 6.4 Monitoring and Alerting

Add metrics to track Kafka health:

```java
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public Health health() {
        try {
            // Try to send a health check message
            kafkaTemplate.send("health-check", "ping").get(5, TimeUnit.SECONDS);
            return Health.up().build();
        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }
}
```

---

## Part 7: Quick Setup for Hackathon

### 7.1 Local Development Setup

1. **Install Kafka (using Docker):**

```bash
# Create docker-compose.yml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

# Start Kafka
docker-compose up -d
```

2. **Switch to Kafka mode:**

```properties
# In application.properties
messaging.type=kafka
```

3. **Verify Kafka topics:**

```bash
# List topics
kafka-topics.sh --bootstrap-server localhost:9092 --list

# Check consumer group
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group chatapp-group
```

### 7.2 Cloud Deployment (AWS MSK, Confluent Cloud)

For hackathon cloud deployment:

```properties
# AWS MSK
spring.kafka.bootstrap-servers=your-msk-cluster.kafka.us-east-1.amazonaws.com:9092

# Confluent Cloud
spring.kafka.bootstrap-servers=pkc-xyz.us-east-1.aws.confluent.cloud:9092
spring.kafka.security.protocol=SASL_SSL
spring.kafka.sasl.mechanism=PLAIN
spring.kafka.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="API_KEY" password="API_SECRET";
```

### 7.3 Scaling Strategy for Hackathon

1. **Start with 1 instance + Kafka**
2. **Add instances as load increases**
3. **Monitor with Spring Boot Actuator**
4. **Use Kafka partitions to distribute load**

```java
// Quick load balancing
@Bean
public NewTopic chatMessagesTopic() {
    // More partitions = better load distribution
    return new NewTopic(CHAT_MESSAGES_TOPIC, 6, (short) 1);  // 6 partitions
}
```

---

## Key Takeaways for Hackathon Use

1. **Start Simple**: Use Redis for prototype, switch to Kafka when you need scale
2. **Message Ordering**: Use chat room ID as message key to maintain order
3. **Error Handling**: Always acknowledge messages to prevent infinite retries
4. **Monitoring**: Use Spring Boot Actuator to monitor Kafka health
5. **Testing**: Use `@EmbeddedKafka` for integration tests

This guide gives you everything needed to understand, implement, and debug Kafka in your Spring Boot chat application. The configuration is production-ready but optimized for hackathon-scale deployment.
