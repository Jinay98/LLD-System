# Rate Limiter — LLD Interview Reference

## System Overview

The Rate Limiter provides a pluggable `RateLimitingStrategy` interface with five algorithm implementations: Fixed Window, Token Bucket, Leaky Bucket, Sliding Window Log, and Sliding Window Counter. Each tracks per-user state using `ConcurrentHashMap`. A `RateLimiterService` delegates to the active strategy.

---

## Core Entities

| Entity | Key Fields | Responsibilities |
|--------|-----------|-----------------|
| `RateLimiterService` | `strategy` | Thin delegate to the active strategy |
| `RateLimitingStrategy` (interface) | `allowRequest(userId)` | Contract for all algorithms |
| `FixedWindowStrategy` | `windowStartTimes`, `requestCounts`, `maxRequests`, `windowSizeMs` | Fixed time window; resets counter at window boundary |
| `TokenBucketStrategy` | `buckets` (Map<userId, tokens>), `lastRefillTime`, `maxTokens`, `refillRate` | Allows burst up to capacity; tokens replenish over time |
| `LeakyBucketStrategy` | `queues` (Map<userId, Queue>), `lastProcessedTime`, `capacity`, `leakRateMs` | Fixed output rate; absorbs bursts up to queue capacity |
| `SlidingWindowLogStrategy` | `requestLogs` (Map<userId, Deque<Long>>), `maxRequests`, `windowSizeMs` | Exact sliding window using timestamp log |
| `SlidingWindowCounterStrategy` | `currentWindowCounts`, `previousWindowCounts`, `windowBoundary`, `windowSizeMs`, `maxRequests` | Approximate sliding window using two buckets |

---

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Strategy** | `RateLimitingStrategy` | Swap algorithm without changing service code; each algorithm encapsulates its own state |
| **Delegate / Wrapper** | `RateLimiterService` | Adds a single indirection point for future cross-cutting concerns (logging, metrics) |

---

## Algorithm Comparison

| Algorithm | Accuracy | Memory | Burst Handling | Complexity |
|-----------|---------|--------|----------------|------------|
| Fixed Window | Low (edge bursts) | O(users) | Allows 2x burst at window boundary | Simple |
| Token Bucket | High | O(users) | Allows burst up to capacity | Medium |
| Leaky Bucket | High | O(users × queue) | Smooths burst, drops excess | Medium |
| Sliding Window Log | Exact | O(users × requests) | None — precise | Higher memory |
| Sliding Window Counter | Approximate | O(users) | None — approximate | Medium |

---

## Database Schema

(Rate limiting state is almost always kept in Redis or in-memory for performance. This schema is for audit/analytics only.)

```sql
-- Rate limit configurations per API route / user tier
CREATE TABLE rate_limit_configs (
    id              VARCHAR(36)   PRIMARY KEY,
    name            VARCHAR(100)  NOT NULL UNIQUE,    -- e.g. 'default', 'premium'
    algorithm       ENUM('FIXED_WINDOW','TOKEN_BUCKET','LEAKY_BUCKET',
                        'SLIDING_WINDOW_LOG','SLIDING_WINDOW_COUNTER') NOT NULL,
    max_requests    INT           NOT NULL,
    window_seconds  INT           NOT NULL,
    burst_capacity  INT,                              -- for token bucket
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Per-user rate limit overrides
CREATE TABLE user_rate_limits (
    user_id         VARCHAR(36)   NOT NULL,
    config_id       VARCHAR(36)   NOT NULL,
    PRIMARY KEY (user_id),
    FOREIGN KEY (user_id)   REFERENCES users(id),
    FOREIGN KEY (config_id) REFERENCES rate_limit_configs(id)
);

-- Rate limit violation log (for alerting)
CREATE TABLE rate_limit_violations (
    id              BIGINT        PRIMARY KEY AUTO_INCREMENT,
    user_id         VARCHAR(36)   NOT NULL,
    endpoint        VARCHAR(500),
    violated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    config_name     VARCHAR(100)
);
CREATE INDEX idx_violations_user ON rate_limit_violations(user_id, violated_at DESC);
CREATE INDEX idx_violations_time ON rate_limit_violations(violated_at DESC);
```

**Redis-based state (preferred over DB for production):**
```
# Fixed Window
INCR ratelimit:{userId}:fixed:{window_start}
EXPIRE ratelimit:{userId}:fixed:{window_start} {window_seconds}

# Token Bucket
HSET ratelimit:{userId}:tokens tokens {count} last_refill {timestamp}
```

---

## API Modelling

Rate limiters are typically middleware, not REST endpoints. The "API" is the `allowRequest()` contract, but here is the full pattern for a rate-limit-aware service.

### Middleware contract: `allowRequest(userId): boolean`

Called before every inbound API request. Returns `true` to allow, `false` to reject.

**HTTP Response when rejected:**
```
HTTP 429 Too Many Requests
Retry-After: 30
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1719000000
```

**Headers to always return (even on allowed requests):**
| Header | Meaning |
|--------|---------|
| `X-RateLimit-Limit` | Max requests allowed |
| `X-RateLimit-Remaining` | Requests left in current window |
| `X-RateLimit-Reset` | Unix timestamp when window resets |
| `Retry-After` | Seconds before client should retry (only on 429) |

---

### GET /api/ratelimit/status/{userId}
Admin: Check current rate limit state for a user.

**Response 200:**
```json
{
  "userId": "u1",
  "algorithm": "TOKEN_BUCKET",
  "tokensRemaining": 45,
  "maxTokens": 100,
  "nextRefillAt": "2026-06-20T14:30:05Z"
}
```

---

### POST /api/ratelimit/config
Admin: Create or update rate limit config.

**Request Body:**
```json
{
  "name": "premium",
  "algorithm": "TOKEN_BUCKET",
  "maxRequests": 500,
  "windowSeconds": 60,
  "burstCapacity": 100
}
```

---

## Algorithm-Specific Failure Cases

### Fixed Window
- **Edge burst:** At the end of window W1 and start of window W2, a client can send `maxRequests` in the last second of W1 and `maxRequests` in the first second of W2 — effectively 2× the limit. Use Sliding Window to fix.
- **Concurrent window check + update:** Without `synchronized`, two threads can both see count < max and both increment. The code uses `ConcurrentHashMap` but the check-and-increment is not atomic. Fix: use `compute()` atomically.

### Token Bucket
- **Refill timing drift:** `lastRefillTime` is per-user; if no requests come for a long time, the next request refills to `maxTokens` immediately (correct), but the refill calculation using `System.currentTimeMillis()` can have clock skew in distributed systems.
- **Token starvation:** If `refillRate` is very low and `maxTokens` is small, bursty workloads get throttled aggressively. Configure `burstCapacity` appropriately.

### Leaky Bucket
- **Queue overflow:** When the queue for a user fills up (capacity reached), new requests are dropped. The `allowRequest()` method should return `false` and callers receive 429.
- **Memory leak:** If a userId sends many requests and then goes silent, their queue sits in memory indefinitely. Add a cleanup task that removes empty, idle queues after N seconds.

### Sliding Window Log
- **Memory:** Each request timestamp is stored in a `Deque`. For 1000 req/s over a 60s window, that's 60,000 timestamps per user. At scale, this is too expensive. Use SlidingWindowCounter instead.
- **Clock skew in distributed systems:** Timestamp comparison across nodes must use a synchronized clock (e.g. NTP, AWS Time Sync Service).

### Sliding Window Counter
- **Approximation error:** Up to `maxRequests * (1 - overlapFraction)` requests can be over-counted or under-counted at window boundaries. For most APIs, this ~10% error is acceptable.

---

## Concurrency & Thread-Safety Notes

- All strategies use `ConcurrentHashMap` for per-user state — safe for concurrent reads.
- **Critical: non-atomic check-and-update.** In `FixedWindowStrategy.allowRequest()`, the pattern is:
  ```java
  count = requestCounts.getOrDefault(userId, 0);
  if (count < maxRequests) { requestCounts.put(userId, count + 1); return true; }
  ```
  This is not atomic — two threads can both read `count < maxRequests` and both increment. **Fix:**
  ```java
  requestCounts.compute(userId, (k, v) -> v == null ? 1 : v + 1);
  ```
  Then check if result > maxRequests and decrement if so. Or use `AtomicInteger.incrementAndGet()` with a compare.
- Same TOCTOU issue applies to `TokenBucketStrategy` and `SlidingWindowLogStrategy`.
- `LeakyBucketStrategy.queues` stores `Queue<Long>` objects. The queue itself is not thread-safe (plain `LinkedList`). Fix: use `ConcurrentLinkedQueue`.

---

## Code Review Findings

**Critical:**
- **Non-atomic check-and-increment in all strategies** (see Concurrency section). Under concurrent load, all five strategies can allow more requests than the limit.
- **`LeakyBucketStrategy` uses a non-thread-safe `LinkedList` queue.** Under concurrent access, this will throw `ConcurrentModificationException`. Replace with `ConcurrentLinkedQueue` or wrap with `synchronized`.

**Design:**
- `RateLimiterService` is a thin wrapper with no added value currently — it re-exposes `allowRequest()`. This is fine for now as an extension point but should at least add metrics logging.
- `SlidingWindowCounterStrategy` stores `previousWindowCounts` and `currentWindowCounts` as separate maps — the calculation correctly weights the two windows but resets atomically on `windowBoundary` check. This reset is not thread-safe: two threads can both trigger the reset.
- All strategies take `userId` as a String. For per-IP or per-endpoint limiting, the key should be a composite: `ip:endpoint` or `userId:endpoint`.

**Minor:**
- `FixedWindowStrategy.windowSizeMs` is a `long` — document the unit (milliseconds) or use `Duration`.
- No `allowRequest()` method returns remaining quota or retry-after time. The middleware needs this to set response headers. Add `RateLimitResult` object as return type.

---

## Extension Points

- **Distributed rate limiting:** Replace in-memory maps with Redis calls. Use Redis's `INCR` + `EXPIRE` for Fixed Window, `EVALSHA` (Lua script) for atomic token bucket.
- **Per-endpoint limits:** Change strategy key from `userId` to `userId:endpoint:method`. Inject a `KeyExtractor` strategy.
- **Admin UI:** Expose `GET /api/ratelimit/configs` and `PUT /api/ratelimit/override/{userId}` for runtime adjustments.
- **Circuit breaker integration:** When a downstream service is slow, automatically tighten rate limits via an event hook.
