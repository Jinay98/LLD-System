# Movie Booking System — LLD Interview Reference

## System Overview

The Movie Booking System allows users to search for shows by movie title and city, select seats, and confirm a booking with payment. Concurrent seat selection is handled through an optimistic-locking-style seat lock with a configurable TTL. The system applies pluggable pricing strategies (weekday/weekend surcharge) and pluggable payment strategies. A `SeatLockManager` uses a `ScheduledExecutorService` to auto-expire locks, preventing ghost seats.

---

## Core Entities

| Entity | Key Fields | Responsibilities |
|--------|-----------|-----------------|
| `Movie` | `id`, `title`, `durationInMinutes` | Catalogue entry |
| `City` | `id`, `name` | Geographic grouping for cinema search |
| `Cinema` | `id`, `name`, `city`, `screens` | Physical venue |
| `Screen` | `id`, `seats` | One auditorium inside a cinema |
| `Seat` | `id`, `row`, `col`, `type`, `status` | A single bookable position |
| `Show` | `id`, `movie`, `screen`, `startTime`, `seatPrices`, `pricingStrategy`, `lock` | One screening event; holds the per-show concurrency monitor |
| `User` | `id`, `name`, `email` | Registered customer |
| `Booking` | `id`, `user`, `show`, `seats`, `totalAmount`, `payment` | Confirmed reservation |
| `Payment` | `id`, `amount`, `status`, `transactionId` | Payment record attached to a booking |

### Enums

| Enum | Values |
|------|--------|
| `SeatStatus` | `AVAILABLE`, `LOCKED`, `BOOKED` |
| `SeatType` | `REGULAR`, `PREMIUM`, `RECLINER` |
| `PaymentStatus` | `SUCCESS`, `FAILURE`, `PENDING` |

**Seat status transitions:**
```
AVAILABLE → LOCKED (lockSeats)
LOCKED    → BOOKED  (confirmSeats)
LOCKED    → AVAILABLE (unlockSeats / expiry)
```

---

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | `MovieBookingService.getInstance()` | Single registry of shows, cinemas, users |
| **Strategy** | `PricingStrategy` (`WeekdayPricingStrategy`, `WeekendPricingStrategy`) | Swap pricing rules per show without touching booking logic |
| **Strategy** | `PaymentStrategy` (`CreditCardPaymentStrategy`) | Swap payment gateway without touching booking flow |
| **Template Method** (implicit) | `BookingManager.createBooking()` | Fixed sequence: lock → price → pay → confirm → create booking |
| **Observer** (missing — extension point) | Could notify user on booking confirmation | Decouple email/SMS from booking logic |

---

## Database Schema

```sql
-- Cities
CREATE TABLE cities (
    id          VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Cinemas
CREATE TABLE cinemas (
    id          VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    city_id     VARCHAR(36)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE RESTRICT
);
CREATE INDEX idx_cinemas_city ON cinemas(city_id);

-- Screens
CREATE TABLE screens (
    id          VARCHAR(36)  PRIMARY KEY,
    cinema_id   VARCHAR(36)  NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cinema_id) REFERENCES cinemas(id) ON DELETE CASCADE
);

-- Seats (static layout per screen)
CREATE TABLE seats (
    id          VARCHAR(36)  PRIMARY KEY,
    screen_id   VARCHAR(36)  NOT NULL,
    row_number  INT          NOT NULL,
    col_number  INT          NOT NULL,
    seat_type   ENUM('REGULAR','PREMIUM','RECLINER') NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (screen_id, row_number, col_number),
    FOREIGN KEY (screen_id) REFERENCES screens(id) ON DELETE CASCADE
);

-- Movies
CREATE TABLE movies (
    id                  VARCHAR(36)  PRIMARY KEY,
    title               VARCHAR(300) NOT NULL,
    duration_minutes    INT          NOT NULL,
    language            VARCHAR(50),
    genre               VARCHAR(100),
    rating              VARCHAR(10),
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_movies_title ON movies(title);

-- Users
CREATE TABLE users (
    id          VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    email       VARCHAR(200) NOT NULL UNIQUE,
    phone       VARCHAR(20),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Shows
CREATE TABLE shows (
    id                  VARCHAR(36)  PRIMARY KEY,
    movie_id            VARCHAR(36)  NOT NULL,
    screen_id           VARCHAR(36)  NOT NULL,
    start_time          DATETIME     NOT NULL,
    pricing_type        ENUM('WEEKDAY','WEEKEND','HOLIDAY') NOT NULL DEFAULT 'WEEKDAY',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (movie_id)  REFERENCES movies(id)  ON DELETE RESTRICT,
    FOREIGN KEY (screen_id) REFERENCES screens(id) ON DELETE RESTRICT
);
CREATE INDEX idx_shows_movie   ON shows(movie_id);
CREATE INDEX idx_shows_screen  ON shows(screen_id);
CREATE INDEX idx_shows_start   ON shows(start_time);

-- Per-show seat prices
CREATE TABLE show_seat_prices (
    show_id     VARCHAR(36)  NOT NULL,
    seat_type   ENUM('REGULAR','PREMIUM','RECLINER') NOT NULL,
    price       DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (show_id, seat_type),
    FOREIGN KEY (show_id) REFERENCES shows(id) ON DELETE CASCADE
);

-- Show seat inventory (dynamic per show, one row per seat per show)
CREATE TABLE show_seats (
    id          VARCHAR(36)  PRIMARY KEY,
    show_id     VARCHAR(36)  NOT NULL,
    seat_id     VARCHAR(36)  NOT NULL,
    status      ENUM('AVAILABLE','LOCKED','BOOKED') NOT NULL DEFAULT 'AVAILABLE',
    locked_by   VARCHAR(36),           -- user_id holding the lock
    locked_at   TIMESTAMP,             -- when the lock was acquired
    lock_ttl_seconds INT DEFAULT 600,  -- 10 min in production
    version     BIGINT NOT NULL DEFAULT 0,  -- optimistic locking
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (show_id, seat_id),
    FOREIGN KEY (show_id)  REFERENCES shows(id)  ON DELETE CASCADE,
    FOREIGN KEY (seat_id)  REFERENCES seats(id)  ON DELETE RESTRICT,
    FOREIGN KEY (locked_by) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_showseats_show_status ON show_seats(show_id, status);

-- Payments
CREATE TABLE payments (
    id              VARCHAR(36)   PRIMARY KEY,
    user_id         VARCHAR(36)   NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    status          ENUM('PENDING','SUCCESS','FAILURE','REFUNDED') NOT NULL DEFAULT 'PENDING',
    transaction_id  VARCHAR(100)  NOT NULL UNIQUE,  -- gateway reference
    gateway         VARCHAR(50),
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Bookings
CREATE TABLE bookings (
    id              VARCHAR(36)   PRIMARY KEY,
    user_id         VARCHAR(36)   NOT NULL,
    show_id         VARCHAR(36)   NOT NULL,
    payment_id      VARCHAR(36)   NOT NULL UNIQUE,
    total_amount    DECIMAL(10,2) NOT NULL,
    status          ENUM('CONFIRMED','CANCELLED','REFUNDED') NOT NULL DEFAULT 'CONFIRMED',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)   REFERENCES users(id)  ON DELETE RESTRICT,
    FOREIGN KEY (show_id)   REFERENCES shows(id)  ON DELETE RESTRICT,
    FOREIGN KEY (payment_id) REFERENCES payments(id)
);
CREATE INDEX idx_bookings_user ON bookings(user_id);
CREATE INDEX idx_bookings_show ON bookings(show_id);

-- Booking <-> seat mapping (many-to-many)
CREATE TABLE booking_seats (
    booking_id  VARCHAR(36) NOT NULL,
    show_seat_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (booking_id, show_seat_id),
    FOREIGN KEY (booking_id)   REFERENCES bookings(id)    ON DELETE CASCADE,
    FOREIGN KEY (show_seat_id) REFERENCES show_seats(id)  ON DELETE RESTRICT
);
```

---

## API Modelling

### GET /api/shows?movieTitle=&city=
Search for shows.

**Query Params:** `movieTitle` (required), `city` (required), `date` (optional), `language` (optional)

**Response 200:**
```json
[
  {
    "showId": "...",
    "movieTitle": "Inception",
    "cinemaName": "PVR Cinemas",
    "screenId": "...",
    "startTime": "2026-06-20T18:30:00",
    "availableSeats": 45,
    "pricingType": "WEEKDAY"
  }
]
```

**Failure Cases:**
- No shows found → 200 with empty array (not 404)
- Missing required params → 400
- Pagination: add `page` / `size` params; shows list can be huge

---

### GET /api/shows/{showId}/seats
Get seat availability for a show.

**Response 200:**
```json
{
  "showId": "...",
  "seats": [
    { "seatId": "A1", "row": 1, "col": 1, "type": "REGULAR", "status": "AVAILABLE", "price": 150.00 }
  ]
}
```

**Failure Cases:**
- Invalid showId → 404
- Show already started → 410 Gone or still 200 with all seats marked BOOKED
- Real-time seat status: this endpoint needs strong-read or cache with short TTL

---

### POST /api/bookings
Create a booking (lock → pay → confirm).

**Request Body:**
```json
{
  "userId": "...",
  "showId": "...",
  "seatIds": ["A1", "A2"],
  "paymentMethod": "CREDIT_CARD",
  "cardNumber": "4111111111111111",
  "cvv": "123",
  "idempotencyKey": "uuid-v4"
}
```

**Response 201:**
```json
{
  "bookingId": "...",
  "seats": ["A1", "A2"],
  "totalAmount": 300.00,
  "transactionId": "TXN_..."
}
```

**Responses:**
| Code | Meaning |
|------|---------|
| 201 | Booking confirmed |
| 400 | Invalid input (empty seats list, invalid showId) |
| 404 | User or show not found |
| 409 | One or more seats already LOCKED or BOOKED |
| 402 | Payment failed |
| 408 | Seat lock expired before payment completed (refund issued) |
| 422 | Show has already started |

**Happy Path:**
1. Validate user and show exist
2. `lockSeats(show, seats, userId)` — atomically mark LOCKED inside `show.getLock()`
3. Calculate price via `PricingStrategy`
4. Call `paymentStrategy.pay(amount)`
5. If payment fails → `unlockSeats()` → return 402
6. `confirmSeats(show, seats, userId)` — verify lock still held, mark BOOKED
7. If lock expired during payment → `refund()` → return 408
8. Persist booking record
9. Return 201

**Critical Failure Cases:**
- Two users request the same seat simultaneously → only one succeeds because `show.getLock()` is per-show; the second gets 409
- Payment succeeds but DB write fails → payment taken but no booking created → must rollback payment (compensating transaction) or use an outbox pattern
- Lock expires during slow payment (500ms in demo, 10 min in production) → `confirmSeats()` returns false → refund initiated → 408
- Idempotency key re-submission → return existing booking 200 without re-processing

---

### DELETE /api/bookings/{bookingId}
Cancel a booking and initiate refund.

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Booking cancelled, refund queued |
| 400 | Cancellation window has closed (e.g. <2 hours before show) |
| 403 | Not the booking owner |
| 404 | Booking not found |

**Failure Cases:**
- Refund gateway times out → mark payment `REFUND_PENDING`, retry async
- Show already occurred → no refund, 400
- Partial cancellation (cancel some seats) → not supported in current design; requires splitting booking

---

## Concurrency & Thread-Safety Notes

- `SeatLockManager` uses `show.getLock()` (per-show monitor) for all lock/unlock/confirm operations — correct granularity: locks on show A don't block show B.
- `lockedSeats` is a `ConcurrentHashMap<Show, Map<Seat, String>>` — outer map is thread-safe but inner `Map<Seat, String>` is a plain `HashMap` accessed only within `synchronized(show.getLock())` — safe.
- `ScheduledExecutorService` in `SeatLockManager` auto-expires locks in a background thread; the expiry callback calls `unlockSeats()` which acquires `show.getLock()` — no deadlock risk since no other lock is held inside.
- `MovieBookingService` is a `volatile` Singleton with double-checked locking — correct.
- `Seat.setStatus()` is not synchronized — safe only because all callers do so within `show.getLock()`. If called from outside a lock context, this would be a race condition.

---

## Code Review Findings

**Critical:**
- **Payment success + DB failure = data inconsistency.** `BookingManager.createBooking()` calls `paymentStrategy.pay()` and then just constructs a `Booking` object in memory. There is no persistence layer — so if the service crashes after payment but before saving, money is taken with no booking. **Fix:** Use an outbox pattern or at minimum persist payment before confirming seats.
- **`CreditCardPaymentStrategy` uses `Math.random() > 0.05`** for a 95% success simulation. Do not merge this pattern into any production codepath — use a proper payment gateway abstraction.
- **Seat status is mutable without synchronization outside lock context.** `Seat.setStatus()` is public and unsynchronized. Callers outside `SeatLockManager` could corrupt seat state.

**Design:**
- `SeatLockManager.LOCK_TIMEOUT_MS = 500` — 0.5 seconds is unrealistically short. In production this should be 5–10 minutes and read from configuration.
- `MovieBookingService.findShows()` builds the `screenToCinema` index correctly. But it performs case-insensitive string comparison on city name — should compare by ID.
- `Show` carries both `seatPrices` (data) and `pricingStrategy` (behaviour). The pricing strategy should compute the final prices; the raw `seatPrices` should just be the base prices.
- `Booking` is an in-memory object; there is no repository for loading bookings by ID. Add `BookingRepository`.

**Minor:**
- `Movie.getTitle()` exists but `getDurationInMinutes()` is defined with no getter exposed — the field exists but is inaccessible.
- `User` in this package is different from `User` in other packages — naming conflicts if ever combined.
- `Payment` lacks a `refundTransactionId` field for tracking refunds.

---

## Extension Points

- **Cancellation / Refund Policy:** Add a `CancellationPolicy` strategy to `Show` that decides refund percentage based on time before show.
- **Waitlist:** When a BOOKED seat is cancelled, notify waiting users via an Observer chain.
- **Dynamic pricing:** Replace `WeekendPricingStrategy` with a `DynamicPricingStrategy` that factors in demand (seats remaining / time before show).
- **Multi-seat type restrictions:** Add seat adjacency validation so groups always get contiguous seats.
