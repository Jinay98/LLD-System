# Pub-Sub System — LLD Interview Reference

## System Overview

The Pub-Sub system provides asynchronous topic-based message delivery. Publishers push `Message` objects to named topics; all subscribers to that topic receive the message asynchronously via a cached thread pool. `CopyOnWriteArraySet` ensures thread-safe subscriber management. Two subscriber implementations are provided: `NewsSubscriber` (general) and `AlertSubscriber` (formatted differently).

---

## Core Entities

| Entity | Key Fields | Responsibilities |
|--------|-----------|-----------------|
| `PubSubService` | `topics` (ConcurrentHashMap), `executorService` (CachedThreadPool) | Singleton; topic registry and async message dispatcher |
| `Topic` | `name`, `subscribers` (CopyOnWriteArraySet) | Holds subscriber list; broadcasts messages |
| `Message` | `payload`, `timestamp` | Immutable value object |
| `Subscriber` (interface) | `getId()`, `onMessage(Message)` | Contract for all message consumers |
| `NewsSubscriber` | `id`, `name` | Standard subscriber |
| `AlertSubscriber` | `id`, `name` | Formats messages differently |

---

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | `PubSubService.getInstance()` | Single broker; all topics in one registry |
| **Observer** | `Subscriber` / `Topic.broadcast()` | Many subscribers notified on one publish event |
| **Thread Pool** | `Executors.newCachedThreadPool()` | Each subscriber delivery is a separate async task |

---

## Database Schema

```sql
-- Topics
CREATE TABLE topics (
    id          VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(200) NOT NULL UNIQUE,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    retention_hours INT      NOT NULL DEFAULT 168,  -- 7 days
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Subscribers (registered consumers)
CREATE TABLE subscribers (
    id              VARCHAR(36)  PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    endpoint        VARCHAR(500),             -- webhook URL / queue ARN / etc.
    subscriber_type ENUM('NEWS','ALERT','WEBHOOK','QUEUE') NOT NULL DEFAULT 'NEWS',
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Topic subscriptions
CREATE TABLE topic_subscriptions (
    topic_id        VARCHAR(36)  NOT NULL,
    subscriber_id   VARCHAR(36)  NOT NULL,
    subscribed_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    filter_expr     TEXT,        -- optional server-side message filtering
    PRIMARY KEY (topic_id, subscriber_id),
    FOREIGN KEY (topic_id)      REFERENCES topics(id)      ON DELETE CASCADE,
    FOREIGN KEY (subscriber_id) REFERENCES subscribers(id) ON DELETE CASCADE
);

-- Messages (for durability / replay)
CREATE TABLE messages (
    id          VARCHAR(36)  PRIMARY KEY,
    topic_id    VARCHAR(36)  NOT NULL,
    payload     TEXT         NOT NULL,
    published_by VARCHAR(36),
    published_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at  TIMESTAMP,               -- based on topic retention
    FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
);
CREATE INDEX idx_messages_topic_time ON messages(topic_id, published_at DESC);

-- Delivery log (at-least-once tracking)
CREATE TABLE message_deliveries (
    id              VARCHAR(36)  PRIMARY KEY,
    message_id      VARCHAR(36)  NOT NULL,
    subscriber_id   VARCHAR(36)  NOT NULL,
    status          ENUM('PENDING','DELIVERED','FAILED','RETRYING') NOT NULL DEFAULT 'PENDING',
    attempt_count   INT          NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP,
    delivered_at    TIMESTAMP,
    error_message   TEXT,
    UNIQUE (message_id, subscriber_id),
    FOREIGN KEY (message_id)    REFERENCES messages(id)    ON DELETE CASCADE,
    FOREIGN KEY (subscriber_id) REFERENCES subscribers(id) ON DELETE CASCADE
);
CREATE INDEX idx_deliveries_pending ON message_deliveries(status, last_attempt_at)
    WHERE status IN ('PENDING','RETRYING','FAILED');
```

---

## API Modelling

### POST /api/topics
Create a topic.

**Request Body:**
```json
{ "name": "news.breaking", "description": "Breaking news alerts", "retentionHours": 72 }
```

**Responses:**
| Code | Meaning |
|------|---------|
| 201 | Topic created |
| 409 | Topic name already exists |
| 400 | Invalid name format (spaces, special chars) |

---

### POST /api/topics/{topicName}/subscribe
Subscribe to a topic.

**Request Body:**
```json
{ "subscriberId": "s1", "filterExpr": "payload CONTAINS 'sports'" }
```

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Subscribed |
| 404 | Topic or subscriber not found |
| 409 | Already subscribed |

---

### DELETE /api/topics/{topicName}/subscribe/{subscriberId}
Unsubscribe.

**Responses:**
| Code | Meaning |
|------|---------|
| 204 | Unsubscribed |
| 404 | Topic or subscriber not found |

---

### POST /api/topics/{topicName}/publish
Publish a message to a topic.

**Request Body:**
```json
{ "payload": "Breaking: Earthquake in XYZ region" }
```

**Response 202:**
```json
{ "messageId": "m1", "topicName": "news.breaking", "subscriberCount": 42 }
```

**Responses:**
| Code | Meaning |
|------|---------|
| 202 | Accepted for async delivery |
| 404 | Topic not found |
| 503 | Thread pool exhausted |

**Happy Path:**
1. Validate topic exists
2. Persist message (for replay/audit)
3. Create delivery records for all current subscribers (`PENDING`)
4. Submit one task per subscriber to `executorService`
5. Each task calls `subscriber.onMessage(message)`; update delivery record to `DELIVERED` or `FAILED`
6. Return 202 immediately (don't wait for delivery)

**Failure Cases:**
- Topic has 10,000 subscribers → `newCachedThreadPool()` creates 10,000 threads — risk of OOM. **Fix:** Use a bounded thread pool (`Executors.newFixedThreadPool(n)`) and a work queue.
- Subscriber `onMessage()` throws → message is marked `FAILED`; need retry logic with exponential backoff
- Subscriber is slow → cached thread pool grows unboundedly; use bounded pool with rejection policy
- Publisher publishes to a deleted topic between check and submit → 404

---

### GET /api/messages/{messageId}
Get message metadata.

### GET /api/subscribers/{subscriberId}/deliveries
List delivery history for a subscriber with status filter.

---

## Concurrency & Thread-Safety Notes

- `PubSubService.topics` is `ConcurrentHashMap` — safe for concurrent subscribe/publish.
- `Topic.subscribers` is `CopyOnWriteArraySet` — writes are copy-on-write (expensive for large subscriber sets); iteration is lock-free and safe.
- `newCachedThreadPool()` creates a new thread per task if none are idle — no upper bound. Under a high-publish / many-subscriber scenario, this will spawn hundreds of threads. **Fix:** `Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2)` with a bounded `LinkedBlockingQueue`.
- Message delivery is fire-and-forget — there is no retry or dead-letter mechanism in the current code.

---

## Code Review Findings

**Critical:**
- **`newCachedThreadPool()` is unbounded.** Publishing to a topic with thousands of subscribers will spawn thousands of threads simultaneously, risking OOM. Replace with a fixed/bounded pool.
- **No delivery guarantee.** If the JVM crashes between `broadcast()` calls and subscriber delivery, messages are lost. A production PubSub needs at-least-once delivery via persistent message + delivery log.
- **No error handling in broadcast.** If `subscriber.onMessage()` throws a `RuntimeException`, it propagates inside the executor task and is silently swallowed. Add a try/catch that logs and marks delivery as `FAILED`.

**Design:**
- `Message` has only `payload` (String) and `timestamp` — no `id`, `topicName`, or `publisherId`. Makes tracking impossible. Add a UUID `messageId`.
- `PubSubService` is the only way to create a topic — there is no `Topic` factory or builder. Fine for simple use, but callers cannot pre-configure topic settings (retention, etc.).
- `Topic.broadcast()` iterates subscribers and calls `executorService.submit(...)` for each — `executorService` is a reference passed in. Consider making `Topic` a passive data holder and keeping dispatch in `PubSubService`.

**Minor:**
- `NewsSubscriber` and `AlertSubscriber` have different output formats but no shared interface method for format selection. Consider a `MessageFormatter` strategy.
- `PubSubService.getInstance()` uses double-checked locking but `lock` object is `private static final Object` — correct pattern.

---

## Extension Points

- **Message filtering:** Before delivering to a subscriber, evaluate `filterExpr` — e.g. `payload CONTAINS 'sports'`. Implement a `MessageFilter` strategy.
- **Dead-letter queue:** After N failed delivery attempts, route to a `dead-letter` topic that ops teams can monitor.
- **Fan-out to external systems:** Add a `WebhookSubscriber` that POSTs to an external URL, enabling integration with third-party services.
- **Message ordering:** Add a `partitionKey` to `Message`; route messages with the same key to the same thread to preserve ordering.
