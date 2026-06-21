# Amazon Locker System — LLD Interview Reference

## System Overview

The Amazon Locker System allows delivery agents to deposit packages into self-service locker kiosks and customers to collect them using a one-time 6-digit pickup code. A `LockerSystem` singleton manages multiple `LockerLocation`s, each holding a set of `Locker`s of four sizes (SMALL → XL). The locker assignment is pluggable via a `LockerAssignmentStrategy`; the current implementation ships two: `SmallestLockerStrategy` (prefer smallest fit) and `RoundRobinStrategy` (spread load). Notification delivery is also pluggable via `NotificationService`. Expired packages are cleaned up on demand by `cleanupExpiredPackages()`.

---

## Core Entities

| Entity | Key Fields | Responsibilities |
|--------|-----------|-----------------|
| `LockerSystem` | `locations`, `packageLockerMap`, `packageLocationMap`, `packageCodeMap`, `assignmentStrategy`, `notificationService` | Singleton façade; orchestrates delivery, pickup, and cleanup |
| `LockerLocation` | `id`, `address`, `lockers` (ConcurrentHashMap) | Physical kiosk site; owns a set of Lockers |
| `Locker` | `id`, `size`, `status`, `currentPackage`, `currentCode` | Individual locker unit; `assignPackage()` / `releasePackage()` are synchronized |
| `Package` | `id`, `orderId`, `lockerSize`, `status` | A parcel with a size requirement; status tracks lifecycle |
| `LockerCode` | `code`, `packageId`, `expirationTime` | One-time pickup code with expiry check |

### Package Lifecycle (State Machine)

```
CREATED → DELIVERED → PICKED_UP
CREATED → DELIVERED → RETURNED  (on cleanup after expiry)
```

### Enums

| Enum | Values |
|------|--------|
| `LockerSize` | `SMALL`, `MEDIUM`, `LARGE`, `XL` (each carries physical dimension bounds) |
| `LockerStatus` | `AVAILABLE`, `OCCUPIED`, `OUT_OF_SERVICE` |
| `PackageStatus` | `CREATED`, `DELIVERED`, `PICKED_UP`, `RETURNED` |

---

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | `LockerSystem` | One global system state; consistent locker/package maps across all calls |
| **Strategy** | `LockerAssignmentStrategy` → `SmallestLockerStrategy`, `RoundRobinStrategy` | Swap locker-selection algorithm at runtime without changing the delivery flow |
| **Strategy** | `NotificationService` → `ConsoleNotificationService` | Decouple notification channel (console, SMS, push) from assignment logic |

---

## Database Schema

```sql
-- Locker kiosk locations
CREATE TABLE locker_locations (
    id          VARCHAR(36)   PRIMARY KEY,
    address     VARCHAR(500)  NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Individual locker units
CREATE TABLE lockers (
    id          VARCHAR(36)   PRIMARY KEY,
    location_id VARCHAR(36)   NOT NULL,
    size        VARCHAR(10)   NOT NULL,  -- SMALL | MEDIUM | LARGE | XL
    status      VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE',  -- AVAILABLE | OCCUPIED | OUT_OF_SERVICE
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (location_id) REFERENCES locker_locations(id) ON DELETE RESTRICT,
    CONSTRAINT chk_locker_size   CHECK (size   IN ('SMALL','MEDIUM','LARGE','XL')),
    CONSTRAINT chk_locker_status CHECK (status IN ('AVAILABLE','OCCUPIED','OUT_OF_SERVICE'))
);

CREATE INDEX idx_lockers_location_status ON lockers(location_id, status);
CREATE INDEX idx_lockers_location_size_status ON lockers(location_id, size, status);

-- Packages (represents a parcel being delivered)
CREATE TABLE packages (
    id                  VARCHAR(36)   PRIMARY KEY,
    order_id            VARCHAR(36)   NOT NULL,
    required_locker_size VARCHAR(10)  NOT NULL,  -- size needed to fit this parcel
    status              VARCHAR(20)   NOT NULL DEFAULT 'CREATED',
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_pkg_size   CHECK (required_locker_size IN ('SMALL','MEDIUM','LARGE','XL')),
    CONSTRAINT chk_pkg_status CHECK (status IN ('CREATED','DELIVERED','PICKED_UP','RETURNED'))
);

CREATE INDEX idx_packages_order ON packages(order_id);
CREATE INDEX idx_packages_status ON packages(status);

-- Locker assignments — represents the active occupancy of a locker by a package
-- One row per delivery event; a locker can be re-assigned after pickup/return
CREATE TABLE locker_assignments (
    id              VARCHAR(36)   PRIMARY KEY,
    locker_id       VARCHAR(36)   NOT NULL,
    location_id     VARCHAR(36)   NOT NULL,
    package_id      VARCHAR(36)   NOT NULL UNIQUE,  -- one active assignment per package
    pickup_code     VARCHAR(6)    NOT NULL,          -- 6-digit code; store hashed in production
    assigned_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP     NOT NULL,
    picked_up_at    TIMESTAMP     NULL,              -- NULL until customer collects
    FOREIGN KEY (locker_id)   REFERENCES lockers(id)          ON DELETE RESTRICT,
    FOREIGN KEY (location_id) REFERENCES locker_locations(id) ON DELETE RESTRICT,
    FOREIGN KEY (package_id)  REFERENCES packages(id)         ON DELETE RESTRICT
);

CREATE INDEX idx_la_locker   ON locker_assignments(locker_id);
CREATE INDEX idx_la_package  ON locker_assignments(package_id);
CREATE INDEX idx_la_expires  ON locker_assignments(expires_at)  WHERE picked_up_at IS NULL;
-- ^ partial index accelerates the cleanup query for active, expired assignments
```

> **Schema notes:**
> - `pickup_code` should be stored as a bcrypt hash in production, not plaintext.
> - `locker_assignments` is an append-only log (soft history). The current occupancy is the row where `picked_up_at IS NULL`.
> - Optimistic locking (`version` column) should be added to `lockers` if the assignment step is moved to the DB layer (prevents two concurrent deliveries grabbing the same locker).

---

## API Modelling

### POST /api/v1/locations/{locationId}/deliveries
Deliver a package to a locker at a specific location.

**Path param:** `locationId`

**Request Body:**
```json
{
  "packageId": "PKG-001",
  "orderId": "ORD-001",
  "requiredLockerSize": "SMALL",
  "expiryHours": 72
}
```

**Response:**
| Status | Meaning |
|--------|---------|
| `201 Created` | Package delivered; pickup code sent to customer |
| `400 Bad Request` | Missing/invalid fields, invalid locker size |
| `404 Not Found` | `locationId` does not exist |
| `409 Conflict` | Package already delivered or in a locker |
| `503 Service Unavailable` | No locker of required size (or larger) available |

**Happy path:**
1. Validate `locationId` exists.
2. Fetch all lockers at location.
3. Invoke `LockerAssignmentStrategy.assignLocker(requiredSize, lockers)`.
4. Atomically call `locker.assignPackage(pkg, code)` (synchronized).
5. Persist assignment row in `locker_assignments`.
6. Send notification via `NotificationService`.
7. Return `201` with assignment details.

**Failure cases:**
- Location not found → `404`
- Package ID already exists in `locker_assignments` → `409`
- No available locker of any fitting size → `503` (`NoAvailableLockerException`)
- Notification service failure → still return `201` but log the failure (non-blocking)

---

### POST /api/v1/locations/{locationId}/lockers/{lockerId}/pickup
Pick up a package from a specific locker using a code.

**Path params:** `locationId`, `lockerId`

**Request Body:**
```json
{
  "pickupCode": "482910"
}
```

**Response:**
| Status | Meaning |
|--------|---------|
| `200 OK` | Package released; returns package details |
| `400 Bad Request` | Missing/malformed code |
| `404 Not Found` | Location or locker not found |
| `409 Conflict` | Package already picked up |
| `410 Gone` | Package expired — contact support |
| `422 Unprocessable Entity` | Invalid pickup code |

**Happy path:**
1. Resolve `locationId` → `LockerLocation`.
2. Resolve `lockerId` → `Locker`.
3. Acquire `synchronized(locker)`.
4. Validate `LockerCode`: check `isExpired()`, then compare code.
5. Call `locker.releasePackage()`.
6. Update `packages.status = PICKED_UP` and set `locker_assignments.picked_up_at`.
7. Return package metadata.

**Failure cases:**
- Locker is `AVAILABLE` (nothing in it) → `404`
- Code expired → `410` (`PackageExpiredException`)
- Package already picked up → `409` (`PackageAlreadyPickedUpException`)
- Wrong code → `422` (`InvalidCodeException`); increment attempt counter for brute-force protection

---

### POST /api/v1/admin/cleanup-expired
Sweep all locations and release lockers with expired packages.

**Auth:** Admin-only

**Response:** `200 OK` with a count of packages returned.

**Happy path:**
1. For each locker that is `OCCUPIED` with an expired `LockerCode`, acquire `synchronized(locker)`.
2. Call `locker.releasePackage()`, set `PackageStatus.RETURNED`.
3. Update DB: `packages.status = RETURNED`, soft-delete `locker_assignments` row.
4. Return summary `{ "returnedCount": 3 }`.

**Failure cases:**
- Concurrent pickup happening at the same locker → synchronized block ensures mutual exclusion; one operation wins atomically.

---

### GET /api/v1/packages/{packageId}/status
Check current status of a package.

**Response:** `200 OK`
```json
{
  "packageId": "PKG-001",
  "status": "DELIVERED",
  "locationAddress": "123 Main St, Seattle",
  "expiresAt": "2026-06-24T10:00:00Z"
}
```

| Status | Meaning |
|--------|---------|
| `200 OK` | Status returned |
| `404 Not Found` | Package does not exist |

---

## Concurrency & Thread-Safety Notes

| Shared State | Protection | Notes |
|-------------|-----------|-------|
| `LockerSystem` singleton creation | Double-checked locking + `volatile instance` | Correct DCL with `volatile` prevents instruction reorder |
| `LockerLocation.lockers` | `ConcurrentHashMap` | Safe for concurrent add/read |
| `Locker.assignPackage()` / `releasePackage()` | `synchronized` on `Locker` instance | Prevents two deliveries grabbing the same locker; prevents pickup racing with cleanup |
| `LockerSystem.pickupPackage()` | `synchronized(locker)` | Combined validity check + release is atomic |
| `LockerSystem.cleanupExpiredPackages()` | `synchronized(locker)` per locker | Same lock as pickup; safe to run concurrently across lockers (only contention is per-locker) |
| `packageLockerMap`, `packageLocationMap`, `packageCodeMap` | `ConcurrentHashMap` | Thread-safe for single-key put/remove |

**Race condition — SmallestLockerStrategy not synchronized:**  
`SmallestLockerStrategy.assignLocker()` iterates available lockers and returns the first match without holding a lock. Two concurrent deliveries can both see the same locker as `AVAILABLE` and both attempt `assignPackage()`. The second call will throw `IllegalStateException` ("Locker X is not available"). The calling code in `LockerSystem.deliverPackageWithCustomExpiry()` has no retry loop — the delivery will fail instead of falling back to the next available locker. A fix is to catch `IllegalStateException` and retry.

**Race condition — RoundRobinStrategy index drift:**  
`lastAssignedIndex` is updated on the filtered `matching` list, which may differ in size between calls. The index can point past the list end on the next call, causing an `IndexOutOfBoundsException` if the list shrinks. The `% matching.size()` guard only works if `matching.size() > 0`, which is already checked, but the index carries state across calls with different list sizes.

---

## Code Review Findings

**Critical**
- `generateCode()` uses `java.util.Random` (not cryptographically secure). A 6-digit code is only 1,000,000 possibilities. With `Random`, seeds are predictable. Replace with `SecureRandom`.
- No brute-force protection on pickup: a bad actor can try all 1,000,000 codes against an occupied locker. Add a max-attempt counter (e.g., 3 attempts → lock the locker and alert support).
- `getCodeForPackage(packageId)` exposes the plaintext code to any caller who knows a package ID — this should never be a public API method. Codes must only flow through the notification channel.
- `Package.java` contains `import com.lld.realworldexamples.AmazonLocker.entities.Package;` — a self-referential import that should be removed.

**Design**
- `SmallestLockerStrategy.assignLocker()` is stateless and unsynchronized. Two threads can race past the availability check. Either add a retry loop in `deliverPackageWithCustomExpiry()` after catching `IllegalStateException`, or move the locker selection inside the `synchronized(locker)` block.
- `cleanupExpiredPackages()` has no notification step — the customer whose package expired is never informed. `NotificationService` should be called before releasing.
- `LockerSystem.deliverPackageWithCustomExpiry()` is not atomic with respect to the strategy: strategy picks a locker, but between strategy selection and `locker.assignPackage()` another thread may have occupied it. This is the critical window.
- No persistence: all state lives in in-memory maps. A JVM restart loses all assignments.

**Minor**
- `LockerCode.expirationTime` is a raw `long` (epoch millis). Using `java.time.Instant` improves readability and eliminates timezone confusion.
- `LockerLocation.getAvailableLockersBySize()` is defined but never called by any strategy (both strategies use `getAllLockers()` instead). Either use this method in strategies or remove it.
- `OUT_OF_SERVICE` status exists in the enum but is never set anywhere in the codebase.

---

## Extension Points

- **Multi-code retry flow:** Add a `maxAttempts` field to `LockerCode` and increment on failed pickup; lock the locker and trigger a support ticket after threshold.
- **Scheduled cleanup:** Replace the on-demand `cleanupExpiredPackages()` with a `ScheduledExecutorService` that runs periodically (e.g., nightly), decoupling cleanup from admin API calls.
- **Locker health monitoring:** Implement `OUT_OF_SERVICE` transitions triggered by a hardware health-check service; `SmallestLockerStrategy` and `RoundRobinStrategy` already skip non-AVAILABLE lockers so no strategy change needed.
- **Multi-locker delivery (oversized items):** Allow a `Package` to request two adjacent lockers; `LockerAssignmentStrategy` interface would need to return `List<Locker>` instead of a single `Locker`.
