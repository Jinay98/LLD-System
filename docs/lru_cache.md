# LRU Cache — LLD Interview Reference

## System Overview

The LRU (Least Recently Used) Cache evicts the least recently accessed entry when capacity is exceeded. It combines a `HashMap` for O(1) key lookup and a doubly-linked list to track access order. The most recently used node sits at the head; the least recently used sits at the tail. On every `get` and `put`, the accessed node moves to the front. Eviction removes the tail node.

---

## Core Entities

| Entity | Key Fields | Responsibilities |
|--------|-----------|-----------------|
| `LRUCache<K,V>` | `capacity`, `map` (HashMap), `list` (DoublyLinkedList) | Public API: `get()`, `put()` |
| `DoublyLinkedList<K,V>` | `head`, `tail` (dummy sentinels), `size` | Maintains access-order list; `addFirst()`, `remove()`, `moveToFront()`, `removeLast()` |
| `Node<K,V>` | `key`, `value`, `prev`, `next` | One cache entry in the linked list |

---

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Composite Data Structure** | `HashMap` + `DoublyLinkedList` | O(1) lookup (HashMap) + O(1) reorder and eviction (linked list) |
| **Generic types** | `LRUCache<K,V>`, `Node<K,V>` | Reusable for any key/value type |

---

## Database Schema

(LRU caches are in-memory by nature. The schema below covers the case where cache entries must be persisted for warm restarts or auditing.)

```sql
-- Cache entries (for persistence / warm-restart scenarios)
CREATE TABLE cache_entries (
    cache_name      VARCHAR(100)  NOT NULL,
    cache_key       VARCHAR(500)  NOT NULL,
    cache_value     TEXT          NOT NULL,    -- JSON-serialized value
    last_accessed   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    access_count    BIGINT        NOT NULL DEFAULT 1,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cache_name, cache_key)
);
CREATE INDEX idx_cache_lru ON cache_entries(cache_name, last_accessed ASC);
-- Eviction query: DELETE FROM cache_entries WHERE cache_name = ? ORDER BY last_accessed LIMIT ?
```

---

## API Modelling

LRU Cache is typically used as a library, not an HTTP service. Below covers both the Java API and a hypothetical distributed cache REST API.

### Java API

```java
// Create a cache of capacity 100
LRUCache<String, User> cache = new LRUCache<>(100);

// Put a value
cache.put("user:123", user);  // O(1)

// Get a value (moves to front)
User user = cache.get("user:123");  // O(1), returns null if not found

// Eviction happens automatically when capacity exceeded
```

**Failure Cases (Java API):**
- `capacity <= 0` → `IllegalArgumentException` (not currently validated — add it)
- `key == null` → `NullPointerException` from `HashMap.put()` — defend with null check
- `value == null` → allowed in HashMap; semantically ambiguous (null vs. not found). Consider rejecting null values.
- Thread safety: all public methods are `synchronized` — safe for concurrent use but single lock is a bottleneck. For high concurrency, use `ConcurrentHashMap` + segment-level locking (like `LinkedHashMap` with `accessOrder = true` + `removeEldestEntry`).

---

### REST API (Distributed Cache)

### GET /api/cache/{cacheName}/{key}
Get a cached value.

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | `{ "key": "...", "value": "..." }` |
| 404 | Key not in cache (cache miss) |
| 400 | cacheName or key invalid |

---

### PUT /api/cache/{cacheName}/{key}
Set a cache value.

**Request Body:**
```json
{ "value": "...", "ttlSeconds": 300 }
```

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Stored (eviction may have occurred) |
| 400 | Invalid key or value |

**Failure Cases:**
- Concurrent PUT for same key from two nodes → last writer wins (acceptable for cache)
- Cache capacity reached → LRU entry evicted; caller gets 200 but previous entry is gone
- `ttlSeconds` expiry: current implementation has no TTL. Add a `expiredAt` field to `Node` and check on `get()`.

---

### DELETE /api/cache/{cacheName}/{key}
Invalidate a cached entry.

**Responses:**
| Code | Meaning |
|------|---------|
| 204 | Deleted |
| 404 | Key not found |

---

### GET /api/cache/{cacheName}/stats
Cache statistics.

**Response 200:**
```json
{
  "capacity": 100,
  "size": 87,
  "hitRate": 0.94,
  "evictions": 213
}
```

---

## Concurrency & Thread-Safety Notes

- All public methods (`get`, `put`) are `synchronized` on the `LRUCache` instance — correct for thread safety.
- `DoublyLinkedList` methods are NOT synchronized — only safe because they are always called from within `LRUCache`'s synchronized methods.
- **Single lock is a bottleneck** under high concurrency. For a high-throughput scenario, replace with `LinkedHashMap(capacity, 0.75f, true)` and override `removeEldestEntry()` — Java's built-in access-order LRU. Use `Collections.synchronizedMap()` wrapper, or better, segment the cache into N shards each with their own lock (`ConcurrentLinkedHashMap` from Guava or Caffeine library).

---

## Code Review Findings

**Critical:**
- **No null-key or null-value guard.** `cache.put(null, value)` throws NPE from `map.put(null, node)` since Java `HashMap` allows null keys but `DoublyLinkedList.addFirst()` would fail on `node.key` access. Add explicit null checks.
- **No capacity validation.** `new LRUCache<>(0)` or `new LRUCache<>(-1)` creates a dysfunctional cache that evicts on every put. Fix: `if (capacity <= 0) throw new IllegalArgumentException(...)`.

**Design:**
- `DoublyLinkedList.size` is tracked manually — can get out of sync if `remove()` or `addFirst()` is called without the other in the right order. Use `assert size >= 0` guards or simplify by computing size from `map.size()`.
- `LRUCache.get()` returns `null` for a cache miss — semantically identical to a stored null value. Consider returning `Optional<V>`.
- The `Node` class is package-private — good. But the `key` field on `Node` is used only during eviction (to remove from the map). Document this as the reason the key is stored in the node.

**Minor:**
- `DoublyLinkedList` uses dummy head/tail sentinel nodes — excellent pattern to avoid null checks. Worth calling out in an interview.
- `LRUCacheDemo` uses `String` keys and values — a more realistic demo with objects would be more interview-impressive.

---

## Extension Points

- **TTL (Time To Live):** Add `expiresAt` to `Node`; in `get()`, check if expired and treat as a miss.
- **LFU Cache:** Replace the doubly-linked list with a frequency-bucketed structure for Least Frequently Used eviction.
- **Stats:** Track `hits`, `misses`, `evictions` counters; expose via `getStats()`.
- **Serialization:** Add `serialize()` / `deserialize()` to `LRUCache` for warm restart from a snapshot file.
