# Vending Machine — LLD Interview Reference

## System Overview

The Vending Machine uses a State pattern to manage four states: Idle, HasMoney, ItemSelected, and Dispensing. Users select an item, insert coins, and trigger dispensing. Change is returned automatically. Inventory tracks item stock. A single `VendingMachine` Singleton holds all state.

---

## Core Entities

| Entity | Key Fields | Responsibilities |
|--------|-----------|-----------------|
| `VendingMachine` | `currentState`, `inventory`, `insertedAmount`, `selectedItem` | Singleton; delegates all actions to current state |
| `VendingMachineState` (interface) | `selectItem()`, `insertCoin()`, `dispense()`, `cancel()` | State contract |
| `IdleState` | — | Accepts item selection |
| `HasMoneyState` | — | Accepts more coins or item selection |
| `ItemSelectedState` | — | Accepts coins until amount meets price |
| `DispensingState` | — | Dispenses item and returns change |
| `Inventory` | `items` (Map<code, Item>), `quantities` (Map<code, int>) | Stock management |
| `Item` | `code`, `name`, `price` | One product |
| `Coin` (enum) | `PENNY(1)`, `NICKEL(5)`, `DIME(10)`, `QUARTER(25)` | Accepted denominations |

### State Transitions

```
IDLE → HAS_MONEY (coin inserted)
IDLE → ITEM_SELECTED (item selected first)
HAS_MONEY → ITEM_SELECTED (item selected after inserting money)
HAS_MONEY → IDLE (cancel)
ITEM_SELECTED → DISPENSING (sufficient amount inserted)
ITEM_SELECTED → HAS_MONEY (more coins needed)
ITEM_SELECTED → IDLE (cancel)
DISPENSING → IDLE (dispensing complete)
```

---

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | `VendingMachine` | One physical machine |
| **State** | `VendingMachineState`, 4 state implementations | Encodes which actions are valid at each phase; prevents invalid transitions |

---

## Database Schema

```sql
-- Vending machines
CREATE TABLE vending_machines (
    id          VARCHAR(36)  PRIMARY KEY,
    location    VARCHAR(300) NOT NULL,
    status      ENUM('ACTIVE','OUT_OF_SERVICE','MAINTENANCE') NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Products catalogue
CREATE TABLE products (
    id          VARCHAR(36)  PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(200) NOT NULL,
    price       DECIMAL(10,2) NOT NULL,
    category    VARCHAR(100),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Inventory per machine
CREATE TABLE machine_inventory (
    machine_id  VARCHAR(36)  NOT NULL,
    product_id  VARCHAR(36)  NOT NULL,
    quantity    INT          NOT NULL DEFAULT 0,
    slot_code   VARCHAR(20)  NOT NULL,   -- physical slot label
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (machine_id, product_id),
    FOREIGN KEY (machine_id) REFERENCES vending_machines(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)         ON DELETE RESTRICT,
    CONSTRAINT chk_qty CHECK (quantity >= 0)
);

-- Transaction log
CREATE TABLE vending_transactions (
    id              VARCHAR(36)   PRIMARY KEY,
    machine_id      VARCHAR(36)   NOT NULL,
    product_id      VARCHAR(36),  -- NULL if transaction cancelled
    amount_inserted DECIMAL(10,2) NOT NULL,
    amount_charged  DECIMAL(10,2),
    change_returned DECIMAL(10,2),
    status          ENUM('COMPLETED','CANCELLED','REFUNDED','FAILED') NOT NULL,
    transaction_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (machine_id) REFERENCES vending_machines(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);
CREATE INDEX idx_vend_txn_machine ON vending_transactions(machine_id, transaction_at DESC);
```

---

## API Modelling

### POST /api/machines/{machineId}/select
Select an item.

**Request Body:**
```json
{ "itemCode": "A1" }
```

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Item selected; returns price required |
| 404 | Item code not found in this machine |
| 409 | Item out of stock |
| 400 | Invalid state (already dispensing) |

---

### POST /api/machines/{machineId}/insert
Insert a coin.

**Request Body:**
```json
{ "coin": "QUARTER" }
```

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Coin accepted; returns total inserted and amount still needed |
| 400 | Invalid coin type |
| 409 | No item selected yet (state mismatch) |

**Failure Cases:**
- Coin slot jammed → 503; log maintenance alert
- Counterfeit coin → hardware detection outside scope; system treats any inserted coin as valid

---

### POST /api/machines/{machineId}/dispense
Trigger dispensing (when enough money inserted).

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Item dispensed; returns change breakdown |
| 402 | Insufficient funds; returns amount still needed |
| 422 | Item out of stock (race condition — stock depleted between select and dispense) |
| 500 | Mechanical failure during dispensing |

**Failure Cases:**
- Machine can't make exact change → return inserted money and abort
- Item stuck in slot → dispense returns failure; credit is not consumed; manual intervention required
- Concurrent dispensing from two sessions (multi-payment terminal) → single lock on machine prevents this

---

### POST /api/machines/{machineId}/cancel
Cancel and return inserted coins.

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Cancelled; returns refund amount |
| 409 | Cannot cancel during dispensing state |

---

## Concurrency & Thread-Safety Notes

- `VendingMachine` is a Singleton with synchronized state mutations.
- State transitions and insertions should be atomic — ensure the state handler transitions are inside a `synchronized` block.
- `Inventory` quantity decrement and state transition should be atomic — use a single lock spanning both operations.

---

## Code Review Findings

**Critical:**
- State transitions between `insertCoin` and `dispense` are not atomically guarded. Two concurrent threads could both verify sufficient funds and both trigger dispensing.

**Design:**
- `Inventory` stores items and quantities in separate maps by code — these should be combined into a single `Map<String, InventoryEntry>` with both item and quantity.
- No change-making algorithm — the machine should verify it has sufficient coins to return change before accepting payment.

**Minor:**
- `Coin` enum values are in cents (PENNY=1, QUARTER=25) — document the unit clearly.
- No receipt / transaction ID returned after dispensing.

---

## Extension Points

- **Cashless payment:** Add a `CardPaymentState` that handles NFC/card taps, bypassing coin insertion.
- **Remote restocking alerts:** Add an Observer that fires when any item quantity drops below a threshold.
- **Multiple items per transaction:** Track a basket instead of a single `selectedItem`.
