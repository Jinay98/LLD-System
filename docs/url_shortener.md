# URL Shortener — LLD Interview Reference

## System Overview

The URL Shortener maps long URLs to short alphanumeric keys. Three key-generation strategies are provided: Base62 (deterministic, counter-based), Random (6-char random), and UUID-truncated. Collision detection retries up to 10 times. An Observer pattern feeds an `AnalyticsService` that tracks creation events and access counts. The Repository pattern isolates storage from the service layer.

---

## Core Entities

| Entity | Key Fields | Responsibilities |
|--------|-----------|-----------------|
| `ShortenedURL` | `longURL`, `shortKey`, `creationDate` | Immutable value object built via inner Builder |
| `URLShortenerService` | `urlRepository`, `keyGenerationStrategy`, `domain`, `observers` | Core service: shorten, resolve, notify |
| `InMemoryURLRepository` | `keyToUrlMap`, `longUrlToKeyMap`, `idCounter` | In-memory storage with bidirectional lookup |
| `AnalyticsService` | `clickCounts` | Counts creates and accesses per short key |

### Enums

| Enum | Values |
|------|--------|
| `EventType` | `URL_CREATED`, `URL_ACCESSED` |

### Key Generation Strategies

| Strategy | Deterministic | Collision Risk | Notes |
|----------|--------------|----------------|-------|
| `Base62Strategy` | Yes (counter-based) | Minimal — deterministic | Produces 6-char keys starting at offset 916M |
| `RandomStrategy` | No | Higher — probability 1/62^6 ≈ 1/56B per attempt | Simple, good for low volume |
| `UUIDStrategy` | No | Low — UUID prefix | First 6 chars of UUID; risk of prefix collision in large sets |

---

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | `URLShortenerService.getInstance()` | One central registry of all short URLs |
| **Strategy** | `KeyGenerationStrategy` | Swap key-generation algorithm without changing service code |
| **Observer** | `Observer`, `AnalyticsService` | Decouple analytics tracking from URL operations |
| **Repository** | `URLRepository`, `InMemoryURLRepository` | Isolate storage details; easy to swap for DB-backed implementation |
| **Builder** | `ShortenedURL.Builder` | Immutable value object with optional fields (`creationDate`) |

---

## Database Schema

```sql
-- Shortened URLs
CREATE TABLE shortened_urls (
    id              BIGINT        PRIMARY KEY AUTO_INCREMENT,
    short_key       VARCHAR(10)   NOT NULL UNIQUE,
    long_url        TEXT          NOT NULL,
    long_url_hash   VARCHAR(64)   NOT NULL,       -- SHA-256 of long_url for fast lookup
    user_id         VARCHAR(36),                  -- NULL for anonymous
    custom_alias    BOOLEAN       NOT NULL DEFAULT FALSE,
    expires_at      TIMESTAMP,                    -- NULL = never expires
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP,                    -- soft delete
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE UNIQUE INDEX idx_short_key       ON shortened_urls(short_key) WHERE deleted_at IS NULL;
CREATE INDEX        idx_long_url_hash   ON shortened_urls(long_url_hash) WHERE deleted_at IS NULL;
CREATE INDEX        idx_user_urls       ON shortened_urls(user_id, created_at DESC);

-- Access log (append-only, high volume — consider separate table / time-series DB)
CREATE TABLE url_access_log (
    id              BIGINT        PRIMARY KEY AUTO_INCREMENT,
    short_key       VARCHAR(10)   NOT NULL,
    accessed_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    referrer        TEXT,
    country_code    CHAR(2),
    INDEX idx_access_short_key (short_key, accessed_at DESC)
);

-- Aggregated click counts (materialized, updated async)
CREATE TABLE url_click_counts (
    short_key       VARCHAR(10)   PRIMARY KEY,
    total_clicks    BIGINT        NOT NULL DEFAULT 0,
    last_clicked_at TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (short_key) REFERENCES shortened_urls(short_key)
);

-- Users (optional — for authenticated URL management)
CREATE TABLE users (
    id          VARCHAR(36)  PRIMARY KEY,
    email       VARCHAR(200) NOT NULL UNIQUE,
    api_key     VARCHAR(64)  NOT NULL UNIQUE,
    plan        ENUM('FREE','PRO','ENTERPRISE') NOT NULL DEFAULT 'FREE',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Counters table for Base62 strategy (global counter)
CREATE TABLE id_counters (
    name        VARCHAR(50)  PRIMARY KEY,
    next_value  BIGINT       NOT NULL DEFAULT 1,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
INSERT INTO id_counters (name, next_value) VALUES ('url_id', 1);
```

**Key decisions:**
- `long_url_hash` stores a SHA-256 hash for fast deduplication lookup without scanning full `TEXT` column.
- `url_access_log` is append-only and high-volume — in production use a time-series DB (InfluxDB, TimescaleDB) or push to a Kafka topic.
- `url_click_counts` is a materialized aggregate updated asynchronously to keep resolution fast.
- The `id_counters` table provides a distributed-safe counter for `Base62Strategy` — use `SELECT ... FOR UPDATE` or atomic `UPDATE id_counters SET next_value = next_value + 1 WHERE name = 'url_id'` with `LAST_INSERT_ID()`.

---

## API Modelling

### POST /api/urls/shorten
Create a short URL.

**Request Body:**
```json
{
  "longUrl": "https://www.example.com/very/long/path?query=param",
  "customAlias": "mypage",      // optional
  "expiresAt": "2027-01-01T00:00:00Z"  // optional
}
```

**Response 201:**
```json
{
  "shortUrl": "https://short.ly/aB3xYz",
  "shortKey": "aB3xYz",
  "longUrl": "https://www.example.com/very/long/path?query=param",
  "createdAt": "2026-06-20T10:00:00Z"
}
```

**Responses:**
| Code | Meaning |
|------|---------|
| 201 | Created |
| 200 | Long URL already exists — returns existing short URL |
| 400 | Invalid URL format; custom alias contains invalid characters |
| 409 | Custom alias already taken |
| 429 | Rate limit exceeded |

**Happy Path:**
1. Validate URL format (must be a valid HTTP/HTTPS URL)
2. Compute `SHA-256(longUrl)`, look up `long_url_hash` — if found, return existing short URL
3. If `customAlias` provided, check availability; else generate via `KeyGenerationStrategy`
4. Retry up to 10 times on collision (for non-deterministic strategies)
5. Persist and notify observers
6. Return short URL

**Failure Cases:**
- Invalid URL (e.g. `javascript:alert(1)`) → reject with 400; validate against safe URL patterns to prevent XSS via redirect
- Custom alias with path traversal characters (`../`) → sanitize/reject
- Counter-based strategy: concurrent calls can get the same counter value if not atomic. Fix: use DB-level atomic increment or `SKIP LOCKED` queue
- After 10 collision retries, service throws `RuntimeException` — expose as 503, not 500

---

### GET /{shortKey}
Redirect to the original URL.

This is the critical hot path — must be fast.

**Response:**
| Code | Meaning |
|------|---------|
| 301 | Permanent redirect (cached by browser — bad for click tracking) |
| 302 | Temporary redirect (re-requests each time — good for analytics) |
| 404 | Short key not found |
| 410 | URL expired (tombstone response) |

**Happy Path:**
1. Look up `shortKey` in cache (Redis) — cache hit → redirect immediately
2. Cache miss → look up in DB → populate cache → redirect
3. Asynchronously log access event to `url_access_log`

**Failure Cases:**
- Expired URL → return 410 with a friendly page, not 404
- Deleted URL → distinguish between "never existed" (404) and "was deleted" (410)
- Malicious short key (SQL injection attempt in path) → use parameterized queries
- Cache eviction during high traffic → thundering herd on DB; use cache stampede prevention (lock or probabilistic early expiration)
- **Current code bug:** `resolve()` returns `shortKey` not `longURL` — should return `longUrl` for actual redirect

---

### GET /api/urls/{shortKey}/stats
Get click analytics for a URL.

**Response 200:**
```json
{
  "shortKey": "aB3xYz",
  "totalClicks": 1042,
  "lastClickedAt": "2026-06-20T09:55:00Z",
  "clicksByDay": [
    { "date": "2026-06-20", "clicks": 150 }
  ]
}
```

**Failure Cases:**
- 404 if short key not found
- 403 if caller is not the URL owner

---

### DELETE /api/urls/{shortKey}
Soft-delete a URL.

**Responses:**
| Code | Meaning |
|------|---------|
| 204 | Deleted (soft) |
| 403 | Not the owner |
| 404 | Not found |

After deletion, the redirect endpoint should return 410.

---

### GET /api/urls?userId=
List all URLs for a user.

**Query Params:** `page`, `size`, `active` (boolean)

---

## Concurrency & Thread-Safety Notes

- `URLShortenerService.getInstance()` uses `synchronized` on the method — correct but slightly coarser than double-checked locking; low impact since instance creation is rare.
- `InMemoryURLRepository` uses `ConcurrentHashMap` for both maps and `AtomicLong` for the counter — correct and thread-safe.
- `shorten()` has a TOCTOU race: `findKeyByLongURL()` returns empty, another thread shortens the same URL before this thread calls `save()`. Result: duplicate short keys for the same long URL. **Fix:** use an `INSERT ... ON DUPLICATE KEY` style atomic upsert or a `computeIfAbsent` pattern.
- Observer list is a plain `ArrayList` — not thread-safe. If `addObserver()` and `notifyObservers()` are called concurrently, `ConcurrentModificationException` is possible. **Fix:** use `CopyOnWriteArrayList`.
- `generateUniqueKey()` retry loop has no synchronization — between `existsByKey()` returning false and `urlRepository.save()` being called, another thread can claim the same key. **Fix:** wrap check-and-save atomically.

---

## Code Review Findings

**Critical:**
- **`resolve()` returns `shortKey` instead of `longURL`.** `URLShortenerService.resolve()` calls `urlRepository.findByKey(shortKey).get()` but then returns `shortKey` — the redirect target is lost. Should return `shortenedURL.getLongURL()`.
- **`INSTANCE` initialized eagerly:** `private static URLShortenerService INSTANCE = new URLShortenerService()` creates an instance before `configure()` is called. The service is broken until `configure()` is called but no error is thrown on `shorten()` before configuration — NPE on `urlRepository.findKeyByLongURL()`.
- **Plain `ArrayList` for observers** is not thread-safe under concurrent `addObserver` + `notifyObservers`. Replace with `CopyOnWriteArrayList`.

**Design:**
- `URLShortenerService.configure()` is a post-construction setter — prefer constructor injection to enforce dependencies at creation time.
- `Base62Strategy` is package-private — `Base62Strategy.class` cannot be instantiated from outside the package. Should be `public`.
- `RandomStrategy` is `public` but `Base62Strategy` and `UUIDStrategy` are package-private — inconsistent visibility.
- `AnalyticsService` tracks click counts per short key using `ConcurrentHashMap<String, AtomicLong>` — correct for thread safety, but in-memory only. Production would write to a DB or stream.

**Minor:**
- `ShortenedURL.Builder` sets `creationDate = LocalDateTime.now()` by default — timezone-naive. Use `ZonedDateTime` or `Instant`.
- `MAX_RETRIES = 10` is a magic number in `URLShortenerService`. Move to a constant with a comment explaining why 10.
- `UUIDStrategy` takes `long id` parameter but ignores it — misleading. Remove the parameter from the interface and implementation, or document the ignored value.

---

## Extension Points

- **Custom domain support:** Add a `domain` field to `shortened_urls`; serve vanity URLs like `brand.com/code`.
- **QR code generation:** Add `GET /api/urls/{shortKey}/qr` that generates a QR code PNG using the short URL.
- **A/B testing redirects:** Add a `redirect_targets` table with `(short_key, target_url, weight)` for weighted random redirect selection.
- **Expiry cleanup:** Add a background scheduler that soft-deletes (sets `deleted_at`) for all URLs where `expires_at < NOW()`.
