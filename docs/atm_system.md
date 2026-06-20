# ATM System — LLD Interview Reference

## System Overview

The ATM system models a physical cash machine that transitions through three states: idle, card-inserted, and authenticated. It supports cash withdrawal with denomination-level dispensing, deposits, and balance checks. A Chain of Responsibility pattern routes the dispensing logic across denominations (100, 50, 20, 10). Each ATM is a Singleton; the BankService stores accounts and cards in memory.

---

## Core Entities

| Entity | Key Fields | Responsibilities |
|--------|-----------|-----------------|
| `ATM` | `currentState`, `currentCard`, `currentAccount`, `cashDispenser` | Central facade; routes calls to the active state handler |
| `Account` | `accountNumber`, `balance` | Holds balance; `debit()` / `credit()` are synchronized |
| `Card` | `cardNumber`, `pin`, `accountNumber` | Maps physical card to an account |
| `CashDispenser` | `denominationHandlers` (LinkedHashMap), `chainHead` | Validates and executes cash dispensing via CoR |
| `DenominationHandler` | `denomination`, `count`, `nextHandler` | One node in the CoR chain; handles one bill denomination |
| `Transaction` | `id`, `type`, `amount`, `accountNumber`, `timestamp` | Audit record (not yet persisted) |
| `BankService` | `accounts`, `cards` (ConcurrentHashMap) | In-memory bank; authenticates PIN |

### State Enum — `ATMState`

| State | Allowed Transitions | Entry Trigger |
|-------|--------------------|-|
| `IDLE` | → `CARD_INSERTED` | card inserted |
| `CARD_INSERTED` | → `AUTHENTICATED`, → `IDLE` | PIN verified / card ejected |
| `AUTHENTICATED` | → `IDLE` | card ejected |

### Denomination Enum

`HUNDRED(100)`, `FIFTY(50)`, `TWENTY(20)`, `TEN(10)` — processed in descending order.

---

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | `ATM.getInstance()` | One physical machine per JVM instance |
| **State** | `ATMStateHandler`, `IdleState`, `CardInsertedState`, `AuthenticatedState` | Encapsulates which operations are valid at each lifecycle phase; eliminates switch/if chains in ATM itself |
| **Chain of Responsibility** | `CashHandler` → `DenominationHandler` | Each denomination tries to satisfy the remaining amount; cleanly extensible to new denominations |
| **Strategy** (implicit) | `ATMStateHandler` interface | Each state is a pluggable strategy for handling the same method signatures |

---

## Database Schema

```sql
-- Accounts
CREATE TABLE accounts (
    id            BIGINT          PRIMARY KEY AUTO_INCREMENT,
    account_number VARCHAR(20)    NOT NULL UNIQUE,
    balance       DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
    status        ENUM('ACTIVE','FROZEN','CLOSED') NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version       BIGINT         NOT NULL DEFAULT 0,  -- optimistic locking
    CONSTRAINT chk_balance CHECK (balance >= 0)
);

-- Cards
CREATE TABLE cards (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
    card_number    VARCHAR(20)  NOT NULL UNIQUE,
    pin_hash       VARCHAR(64)  NOT NULL,     -- bcrypt hash, NEVER plain text
    account_id     BIGINT       NOT NULL,
    status         ENUM('ACTIVE','BLOCKED','EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    failed_pin_attempts INT     NOT NULL DEFAULT 0,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT
);
CREATE INDEX idx_cards_account ON cards(account_id);

-- ATM machines
CREATE TABLE atm_machines (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    location    VARCHAR(255) NOT NULL,
    status      ENUM('ACTIVE','OUT_OF_SERVICE','MAINTENANCE') NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Cash inventory per denomination per ATM
CREATE TABLE atm_cash_inventory (
    atm_id          BIGINT   NOT NULL,
    denomination    INT      NOT NULL,  -- 100, 50, 20, 10
    count           INT      NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (atm_id, denomination),
    FOREIGN KEY (atm_id) REFERENCES atm_machines(id)
);

-- Transaction log (append-only, never update)
CREATE TABLE transactions (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    reference_id    VARCHAR(36)     NOT NULL UNIQUE,  -- UUID
    account_id      BIGINT          NOT NULL,
    atm_id          BIGINT,
    type            ENUM('WITHDRAWAL','DEPOSIT','BALANCE_CHECK') NOT NULL,
    amount          DECIMAL(15,2)   NOT NULL,
    status          ENUM('SUCCESS','FAILED','REVERSED') NOT NULL DEFAULT 'SUCCESS',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id),
    FOREIGN KEY (atm_id)     REFERENCES atm_machines(id)
);
CREATE INDEX idx_txn_account_date ON transactions(account_id, created_at DESC);
```

**Key design decisions:**
- `balance` uses `DECIMAL(15,2)`, never `DOUBLE`, to avoid floating-point money errors.
- `version` on `accounts` enables optimistic locking — increment it on every balance update and reject updates where the version has changed.
- `pin_hash` stores a bcrypt hash; the current code stores plain-text PIN which is a critical security flaw.
- Transactions are append-only — no UPDATE allowed on that table.

---

## API Modelling

### POST /api/atm/{atmId}/session
Insert a card and start a session.

**Request Body:**
```json
{ "cardNumber": "4111111111111111" }
```

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Card accepted, session token returned |
| 404 | Card not found |
| 409 | ATM already has an active session |
| 503 | ATM out of service |

**Failure Cases:**
- Card reported as stolen → 403 with reason code
- ATM in `OUT_OF_SERVICE` state → 503
- Card is expired → 400

---

### POST /api/atm/{atmId}/session/authenticate
Verify PIN.

**Request Body:**
```json
{ "pin": "1234" }
```

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Authenticated |
| 401 | Wrong PIN (include remaining attempts in response) |
| 403 | Card locked after 3 failed attempts |
| 409 | No active session / wrong state |

**Failure Cases:**
- No card inserted yet → 409 (state mismatch)
- 3rd wrong PIN → card status set to `BLOCKED`, session ended, 403 returned
- PIN brute-force: implement rate-limiting at this endpoint

---

### POST /api/atm/{atmId}/session/withdraw
Withdraw cash.

**Request Body:**
```json
{ "amount": 150 }
```

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Cash dispensed; returns denomination breakdown |
| 400 | Amount not a multiple of 10, or non-positive |
| 402 | Insufficient account balance |
| 409 | Not authenticated / wrong state |
| 422 | ATM cannot make exact change (e.g. only $100 bills, requesting $50) |
| 500 | Dispenser mechanical failure |

**Happy Path:**
1. Validate amount (>0, multiple of 10)
2. Check account balance ≥ amount
3. Check `canDispense(amount)` against current inventory
4. Atomically: `dispense()` → reduce inventory → `debit()` account → log transaction
5. Return denomination map

**Critical Failure Cases:**
- Dispenser jams after `debit()` but before bills are released → account debited but no cash given. **Fix:** debit only after confirming dispenser succeeded (the code correctly dispenses first, then debits).
- Concurrent withdrawal from same account (mobile app + ATM simultaneously) → use optimistic locking on `accounts.version`
- Network timeout when logging transaction → transaction logged as `FAILED`; do not double-debit

---

### POST /api/atm/{atmId}/session/deposit
Deposit cash.

**Request Body:**
```json
{ "amount": 500.00 }
```

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Deposit recorded |
| 400 | Non-positive amount |
| 409 | Not authenticated |

**Failure Cases:**
- Physical deposit accepted but system crashes before `credit()` → use a two-phase commit or at-least-once delivery with idempotency key
- Deposit envelope empty / count mismatch → flag for manual review; credit only after teller confirms

---

### GET /api/atm/{atmId}/session/balance

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | `{ "balance": 1234.56 }` |
| 409 | Not authenticated |

---

### DELETE /api/atm/{atmId}/session
Eject card and end session.

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Card ejected |
| 409 | No active session |

---

## Concurrency & Thread-Safety Notes

- `ATM` methods are `synchronized` on the instance — only one operation at a time per machine. Correct.
- `Account.debit()` and `credit()` are synchronized — safe for concurrent access.
- `BankService` uses `ConcurrentHashMap` — safe for concurrent reads/writes.
- `CashDispenser.dispense()` and `canDispense()` are synchronized — inventory is consistent.
- **Gap:** `canDispense()` and `dispense()` are two separate calls with no atomic check-then-act guarantee at the object level. The outer `ATM.synchronized` block covers this in practice, but if `CashDispenser` is ever shared across ATMs, a TOCTOU race exists.
- `Transaction` is created but never stored — no persistence concurrency risk currently.

---

## Code Review Findings

**Critical:**
- `Card` stores plain-text PIN (`private final String pin`). In production, hash with bcrypt. `BankService.authenticate()` compares raw strings — must compare hashes.
- No PIN lockout logic — the system allows unlimited PIN attempts. After 3 failures, block the card.
- `Transaction` is created in `ATMDemo` but never stored anywhere. Transaction history is lost on restart.

**Design:**
- `ATM.getInstance(BankService, CashDispenser)` takes parameters only on first call; subsequent calls ignore them silently. Use a dedicated `initialize()` method or disallow re-initialization via exception.
- `AuthenticatedState.withdraw()` duplicates the balance check that also exists in `Account.debit()`. Single source of truth: let `Account.debit()` throw and catch that exception in the state handler.
- `BankService.debit()` and `BankService.credit()` are public but the state handlers call `account.debit()` directly — inconsistent access paths.
- `Denomination` enum only covers USD. For a multi-currency ATM, denomination handling would need a currency dimension.

**Minor:**
- `CashDispenser` uses `double amount` in some places and `int amount` in others — standardize on `int` for cash (always whole currency units).
- `DenominationHandler.removeBills()` is defined but never called — dead code.
- `Transaction.timestamp` is set via `LocalDateTime.now().toString()` which is timezone-naive; use `Instant.now()`.

---

## Extension Points

- **Multi-currency ATM:** Add a `currency` field to `Denomination` and `Account`; `CashDispenser` selects the right chain by currency.
- **PIN lockout:** Add `failedAttempts` to `Card`; `CardInsertedState.authenticate()` increments count and transitions to a new `LockedState` after 3 failures.
- **Receipt printer:** Add a `ReceiptPrinter` observer to `ATM`; notify after every transaction.
- **Remote monitoring:** Expose `ATMState` and cash inventory over a health endpoint; hook into a monitoring system.
