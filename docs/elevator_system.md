# Elevator System — LLD Interview Reference

## System Overview

The Elevator System manages a fleet of elevators across a multi-floor building. Each elevator runs on its own daemon thread controlled by an `ElevatorController`. Requests are accumulated in `TreeSet` structures (one for up, one for down) and processed using a SCAN algorithm (like a disk scheduler). Two dispatch strategies are provided: `NearestElevatorStrategy` (proximity + direction scoring) and `ZoneBasedStrategy` (floor-zone assignment). The Observer pattern updates a floor `Display` on every state change.

---

## Core Entities

| Entity | Key Fields | Responsibilities |
|--------|-----------|-----------------|
| `ElevatorSystem` | `elevators`, `controllers`, `controllerThreads`, `floors`, `dispatchStrategy` | Singleton façade; accepts external requests and dispatches |
| `Elevator` | `id`, `currentFloor`, `direction`, `state`, `door`, `upRequests` (TreeSet), `downRequests` (TreeSet), `observers` | Core state holder; thread-safe request queue |
| `ElevatorController` | `elevator`, `running` | Runnable loop; processes requests using SCAN algorithm |
| `Door` | `state` | Open/close mechanics |
| `Floor` | `number` | Physical floor entity |
| `Request` | `floor`, `direction`, `type`, `timestamp` | Represents one request (created but used via TreeSet in practice) |
| `Display` | Implements `ElevatorObserver` | Renders current floor and direction |

### State Enums

| Enum | Values |
|------|--------|
| `ElevatorState` | `IDLE`, `MOVING_UP`, `MOVING_DOWN`, `DOOR_OPEN`, `OUT_OF_SERVICE` |
| `Direction` | `UP`, `DOWN`, `IDLE` |
| `DoorState` | `OPEN`, `CLOSED` |
| `RequestType` | `EXTERNAL` (hall button), `INTERNAL` (cabin button) |

**State transitions:**
```
IDLE → MOVING_UP / MOVING_DOWN (on first request)
MOVING_UP → DOOR_OPEN (on arriving at requested floor)
DOOR_OPEN → MOVING_UP / MOVING_DOWN / IDLE (after door closes)
Any → OUT_OF_SERVICE (admin command)
```

---

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | `ElevatorSystem.getInstance()` | One building system; consistent state across all callers |
| **Strategy** | `DispatchStrategy` (`NearestElevatorStrategy`, `ZoneBasedStrategy`) | Swap dispatch algorithm at runtime |
| **Observer** | `ElevatorObserver`, `Display` | Decouple display updates from elevator movement |
| **State** (implicit) | `ElevatorState` enum + `ElevatorController` logic | Direction and state drive which requests are served next |
| **SCAN Algorithm** | `TreeSet<Integer> upRequests/downRequests` in `Elevator.getNextStop()` | Elevator serves requests in one direction before reversing — minimizes travel distance |

---

## Database Schema

(Note: Elevator systems often don't need traditional persistence, but a production system would log events for maintenance and analytics.)

```sql
-- Buildings
CREATE TABLE buildings (
    id          VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    total_floors INT         NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Elevators
CREATE TABLE elevators (
    id              VARCHAR(36)  PRIMARY KEY,
    building_id     VARCHAR(36)  NOT NULL,
    elevator_number INT          NOT NULL,
    capacity_persons INT         NOT NULL DEFAULT 10,
    status          ENUM('ACTIVE','OUT_OF_SERVICE','MAINTENANCE') NOT NULL DEFAULT 'ACTIVE',
    current_floor   INT          NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (building_id, elevator_number),
    FOREIGN KEY (building_id) REFERENCES buildings(id)
);

-- Elevator event log (for maintenance analytics)
CREATE TABLE elevator_events (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    elevator_id     VARCHAR(36)  NOT NULL,
    event_type      ENUM('ARRIVED','DOOR_OPEN','DOOR_CLOSE','REQUEST_DISPATCHED',
                        'STATE_CHANGE','ERROR') NOT NULL,
    floor           INT,
    direction       ENUM('UP','DOWN','IDLE'),
    state           ENUM('IDLE','MOVING_UP','MOVING_DOWN','DOOR_OPEN','OUT_OF_SERVICE'),
    metadata        JSON,
    occurred_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (elevator_id) REFERENCES elevators(id)
);
CREATE INDEX idx_events_elevator ON elevator_events(elevator_id, occurred_at DESC);
CREATE INDEX idx_events_type     ON elevator_events(event_type, occurred_at DESC);

-- Maintenance records
CREATE TABLE maintenance_records (
    id              VARCHAR(36)  PRIMARY KEY,
    elevator_id     VARCHAR(36)  NOT NULL,
    type            ENUM('SCHEDULED','EMERGENCY','INSPECTION') NOT NULL,
    description     TEXT,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    technician      VARCHAR(200),
    FOREIGN KEY (elevator_id) REFERENCES elevators(id)
);
```

---

## API Modelling

### POST /api/buildings/{buildingId}/elevators/request
Request an elevator from a floor (hall button press).

**Request Body:**
```json
{ "floor": 5, "direction": "UP" }
```

**Response 200:**
```json
{
  "dispatchedElevatorId": "e2",
  "currentFloor": 3,
  "estimatedArrivalSeconds": 10
}
```

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Elevator dispatched |
| 400 | Invalid floor (< 1 or > max); direction is `IDLE` |
| 503 | All elevators out of service |

**Happy Path:**
1. Validate floor within range; direction is UP or DOWN
2. `dispatchStrategy.selectElevator(elevators, floor, direction)`
3. `elevator.addRequest(floor, direction)` — added to `upRequests` or `downRequests` TreeSet
4. Return dispatched elevator info

**Failure Cases:**
- All elevators `OUT_OF_SERVICE` → `NearestElevatorStrategy` throws `ElevatorException` → 503
- Requesting floor 0 → `ElevatorException("Invalid floor: 0")` → 400
- Direction = `IDLE` from external request → throw 400 (currently checked in `ElevatorSystem.requestElevator()`)
- Emergency stop: add ability to halt all elevators and transition to `OUT_OF_SERVICE`

---

### POST /api/elevators/{elevatorId}/cabin/request
Request a specific floor from inside the elevator (cabin button press).

**Request Body:**
```json
{ "floor": 8 }
```

**Response 200:**
```json
{ "accepted": true, "floor": 8 }
```

**Failure Cases:**
- Elevator `OUT_OF_SERVICE` → 503
- Floor out of range → 400
- Same floor as current → 200 with "already here" response

---

### GET /api/buildings/{buildingId}/elevators
Get status of all elevators.

**Response 200:**
```json
{
  "elevators": [
    {
      "id": "e1",
      "currentFloor": 5,
      "direction": "UP",
      "state": "MOVING_UP",
      "pendingStops": [7, 9]
    }
  ]
}
```

---

### PATCH /api/elevators/{elevatorId}/status
Set elevator in/out of service (admin).

**Request Body:**
```json
{ "status": "OUT_OF_SERVICE", "reason": "Routine maintenance" }
```

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Status updated |
| 409 | Cannot take out of service while passengers inside |

---

## Concurrency & Thread-Safety Notes

- `Elevator` methods that touch `upRequests`, `downRequests`, `currentFloor`, `direction`, `state` are all `synchronized` on the Elevator instance — correct.
- `ElevatorController.run()` accesses `elevator.hasRequests()`, `elevator.getDirection()`, etc. in a tight loop — all are `synchronized` on the elevator.
- `ElevatorController.processRequests()` calls `elevator.setDirection()` outside a critical section in some paths — for example, the call to `elevator.setDirection(Direction.IDLE)` in the `run()` method is NOT synchronized. Since `setDirection()` is synchronized, the write is safe, but the read-modify-write pattern (read `hasRequests`, then write direction) is not atomic. A request could arrive between the two calls.
- `ElevatorSystem.requestElevator()` is `synchronized` on the instance — dispatches are serialized. This prevents two requests from selecting the same elevator simultaneously.
- `CopyOnWriteArrayList<ElevatorObserver>` is used for observers — safe for concurrent add and iteration.
- **Thread.sleep() inside `serveFloor()`** holds no locks — correct. The elevator's state is set to `DOOR_OPEN` before the sleep, so the system correctly represents the door being open during that period.
- **Daemon threads:** All `ElevatorController` threads are daemon threads — they will be killed when the main thread exits without processing remaining requests. For graceful shutdown, call `ElevatorSystem.shutdown()` explicitly.

---

## Code Review Findings

**Critical:**
- **`ElevatorController.run()` reads `elevator.hasRequests()` and then writes `elevator.setState(IDLE)` in two separate synchronized calls.** A request can arrive between these two calls. If the elevator goes idle while a request is pending, it stays idle until the next iteration of the loop (100ms later). Not data-loss-critical but causes latency spikes.
- **`Elevator.getNextStop()` has a logic issue for the `DOWN` direction.** The `downRequests` TreeSet uses `reverseOrder()` — `ceiling(currentFloor)` on a reverse-ordered TreeSet returns the largest element ≥ currentFloor, which is not the floor just below the current floor. This means the SCAN algorithm doesn't correctly find the next stop when going down. **Fix:** For downRequests, use `floor(currentFloor)` semantics with a properly ordered TreeSet, or use `TreeSet.headSet(currentFloor)` to get floors below and pick the highest.

**Design:**
- `Request` class is created in `ElevatorDemo` but `Elevator.addRequest()` takes `(int floor, Direction direction)` — the `Request` object is never used inside the system. Either use `Request` objects in the TreeSet (with a custom comparator) or remove the `Request` class.
- `ElevatorSystem.getInstance(numElevators, numFloors)` re-uses the existing instance if already created, silently ignoring the new parameters. Same Singleton anti-pattern as ATM.
- `ZoneBasedStrategy` falls back to nearest elevator if the zone's assigned elevator is out of service — good. But the fallback does not prefer direction, only proximity.
- `Display` is always added as an observer in `Elevator`'s constructor — no way to remove it. Observers should be removable.

**Minor:**
- `ElevatorController.stop()` sets `running = false` but `running` is `volatile` — correct.
- `Thread.sleep(500)` between floor movements is hardcoded — should be configurable (`floorTravelTimeMs`).
- `Door` class has an `open()` and `close()` method but no state validation — you can open an already-open door with no error.

---

## Extension Points

- **Weight / capacity limit:** Add `currentWeight` and `maxWeight` to `Elevator`; refuse `addRequest()` when at capacity. Fire a `WEIGHT_EXCEEDED` event to the Observer.
- **Emergency mode:** Add `EMERGENCY` to `ElevatorState`; on fire alarm, all elevators go to ground floor and door stays open.
- **Priority passengers:** Add `PRIORITY` to `RequestType`; priority requests (e.g. wheelchair) jump to the front of the request queue.
- **Predictive dispatch:** Track peak hour patterns in the event log; pre-position elevators at the lobby at 9:00 AM using a `PredictiveDispatchStrategy`.
