# Splitwise — LLD Interview Reference

## System Overview

The Splitwise clone manages shared expenses and tracks who owes whom across users and groups. Expenses can be split equally, by exact amounts, or by percentage. The system maintains a `BalanceSheet` — a bidirectional debt ledger — and supports settling individual debts. An Observer pattern notifies external services (e.g. email) when expenses are added or settled.

---

## Core Entities

| Entity | Key Fields | Responsibilities |
|--------|-----------|-----------------|
| `User` | `id`, `name`, `email`, `phone` | Registered participant |
| `Group` | `id`, `name`, `memberIds`, `expenseIds` | Named collection of users who share expenses |
| `Expense` | `id`, `amount`, `description`, `paidByUserId`, `splitType`, `splits`, `groupId`, `createdAt` | One shared bill event |
| `Split` | `userId`, `amount` | One participant's share of an expense |
| `EqualSplit` | (inherits Split) | Used when all shares are equal |
| `ExactSplit` | `amount` (inherits Split) | Used when the caller specifies each amount |
| `PercentageSplit` | `percentage` (inherits Split) | Used when percentages are provided |
| `BalanceSheet` | nested `ConcurrentHashMap<userId, ConcurrentHashMap<userId, Double>>` | Bidirectional debt ledger |

### Enums

| Enum | Values |
|------|--------|
| `SplitType` | `EQUAL`, `EXACT`, `PERCENTAGE` |

---

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | `SplitwiseService.getInstance()` | Single source of truth for users, groups, expenses, balances |
| **Strategy** | `SplitStrategy` (`EqualSplitStrategy`, `ExactSplitStrategy`, `PercentageSplitStrategy`) | Different calculation/validation logic per split type; selected by a `Map<SplitType, SplitStrategy>` registry |
| **Observer** | `ExpenseObserver`, `EmailNotificationObserver` | Decouple notification side-effects from core balance logic |
| **Inheritance** | `Split` → `EqualSplit`, `ExactSplit`, `PercentageSplit` | Type-safe modelling of different split kinds; strategy receives typed splits |

---

## Database Schema

```sql
-- Users
CREATE TABLE users (
    id          VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    email       VARCHAR(200) NOT NULL UNIQUE,
    phone       VARCHAR(20),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Groups
CREATE TABLE groups (
    id          VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    created_by  VARCHAR(36)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Group membership
CREATE TABLE group_members (
    group_id    VARCHAR(36)  NOT NULL,
    user_id     VARCHAR(36)  NOT NULL,
    joined_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, user_id),
    FOREIGN KEY (group_id) REFERENCES groups(id)  ON DELETE CASCADE,
    FOREIGN KEY (user_id)  REFERENCES users(id)   ON DELETE CASCADE
);

-- Expenses
CREATE TABLE expenses (
    id              VARCHAR(36)  PRIMARY KEY,
    group_id        VARCHAR(36),               -- NULL for non-group (direct) expenses
    paid_by_user_id VARCHAR(36)  NOT NULL,
    amount          DECIMAL(15,2) NOT NULL,
    description     VARCHAR(500),
    split_type      ENUM('EQUAL','EXACT','PERCENTAGE') NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP,                 -- soft delete
    FOREIGN KEY (group_id)        REFERENCES groups(id) ON DELETE SET NULL,
    FOREIGN KEY (paid_by_user_id) REFERENCES users(id)  ON DELETE RESTRICT,
    CONSTRAINT chk_amount CHECK (amount > 0)
);
CREATE INDEX idx_expenses_group   ON expenses(group_id, created_at DESC);
CREATE INDEX idx_expenses_paidby  ON expenses(paid_by_user_id);

-- Expense splits (one row per participant per expense)
CREATE TABLE expense_splits (
    id              VARCHAR(36)   PRIMARY KEY,
    expense_id      VARCHAR(36)   NOT NULL,
    user_id         VARCHAR(36)   NOT NULL,
    split_type      ENUM('EQUAL','EXACT','PERCENTAGE') NOT NULL,
    amount          DECIMAL(15,2) NOT NULL,    -- computed share in currency
    percentage      DECIMAL(5,2),              -- only set for PERCENTAGE splits
    UNIQUE (expense_id, user_id),
    FOREIGN KEY (expense_id) REFERENCES expenses(id)  ON DELETE CASCADE,
    FOREIGN KEY (user_id)    REFERENCES users(id)     ON DELETE RESTRICT
);

-- Balance ledger (materialized for fast queries; updated on every expense/settlement)
-- Denormalized for performance: balance[A][B] = how much A owes B
CREATE TABLE balances (
    from_user_id    VARCHAR(36)   NOT NULL,
    to_user_id      VARCHAR(36)   NOT NULL,
    amount          DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (from_user_id, to_user_id),
    FOREIGN KEY (from_user_id) REFERENCES users(id),
    FOREIGN KEY (to_user_id)   REFERENCES users(id)
);
-- Partial indexes only needed in analytical queries; PRIMARY KEY covers (A, B) lookup

-- Settlement records
CREATE TABLE settlements (
    id              VARCHAR(36)   PRIMARY KEY,
    from_user_id    VARCHAR(36)   NOT NULL,
    to_user_id      VARCHAR(36)   NOT NULL,
    amount          DECIMAL(15,2) NOT NULL,
    payment_method  VARCHAR(50),
    reference_id    VARCHAR(100),             -- external payment ref
    settled_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (from_user_id) REFERENCES users(id),
    FOREIGN KEY (to_user_id)   REFERENCES users(id)
);
CREATE INDEX idx_settlements_from ON settlements(from_user_id, settled_at DESC);
```

**Key decisions:**
- `balances` is a materialized ledger updated atomically with each expense/settlement. An alternative is to compute balances from `expense_splits` and `settlements` at query time (slower but always consistent with history). For scale, the materialized approach is preferred.
- `expenses.deleted_at` enables soft delete so balance history is auditable.
- `DECIMAL(15,2)` handles currency amounts.

---

## API Modelling

### POST /api/users
Register a user.

**Request Body:**
```json
{ "name": "Alice", "email": "alice@example.com", "phone": "+1234567890" }
```

**Responses:**
| Code | Meaning |
|------|---------|
| 201 | User created |
| 409 | Email already in use |
| 400 | Invalid email format, missing name |

---

### POST /api/groups
Create a group.

**Request Body:**
```json
{ "name": "Goa Trip", "createdByUserId": "u1", "memberIds": ["u1", "u2", "u3"] }
```

**Responses:**
| Code | Meaning |
|------|---------|
| 201 | Group created with all members |
| 404 | One or more userIds not found |

---

### POST /api/expenses
Add an expense.

**Request Body (equal split):**
```json
{
  "amount": 300.00,
  "description": "Dinner at Nobu",
  "paidByUserId": "u1",
  "splitType": "EQUAL",
  "groupId": "g1",
  "splits": [
    { "userId": "u1" },
    { "userId": "u2" },
    { "userId": "u3" }
  ]
}
```

**Request Body (exact split):**
```json
{
  "amount": 300.00,
  "description": "Hotel",
  "paidByUserId": "u1",
  "splitType": "EXACT",
  "splits": [
    { "userId": "u1", "amount": 100.00 },
    { "userId": "u2", "amount": 200.00 }
  ]
}
```

**Request Body (percentage split):**
```json
{
  "amount": 300.00,
  "description": "Cab",
  "paidByUserId": "u1",
  "splitType": "PERCENTAGE",
  "splits": [
    { "userId": "u1", "percentage": 50.0 },
    { "userId": "u2", "percentage": 30.0 },
    { "userId": "u3", "percentage": 20.0 }
  ]
}
```

**Response 201:**
```json
{
  "expenseId": "...",
  "splits": [
    { "userId": "u2", "owes": "u1", "amount": 100.00 },
    { "userId": "u3", "owes": "u1", "amount": 100.00 }
  ]
}
```

**Responses:**
| Code | Meaning |
|------|---------|
| 201 | Expense created, balances updated |
| 400 | Exact amounts don't sum to total; percentages don't sum to 100 |
| 404 | Payer or a split participant not found |
| 403 | Payer is not a member of the group |
| 422 | Empty splits list |

**Failure Cases:**
- Payer not in splits list (they paid but their share isn't represented) → payer can be excluded from splits (they owe nothing); the code correctly handles this by skipping payer in balance update
- Floating-point rounding: 100/3 = 33.33, 33.33, 33.34 — last person absorbs remainder (handled in `EqualSplitStrategy`)
- Concurrent expense creation in same group → `balances` table must use `SELECT ... FOR UPDATE` or row-level locking on `(from_user_id, to_user_id)` pair

---

### GET /api/balances/{userId}
Get all debts for a user.

**Response 200:**
```json
{
  "userId": "u1",
  "owes": [
    { "toUserId": "u2", "amount": 150.00 }
  ],
  "isOwed": [
    { "byUserId": "u3", "amount": 75.00 }
  ]
}
```

**Failure Cases:**
- userId not found → 404
- Response should only include non-zero balances

---

### GET /api/balances/{userId1}/{userId2}
Get the net balance between two users.

**Response 200:**
```json
{ "fromUserId": "u1", "toUserId": "u2", "amount": 50.00 }
```

Positive = `u1` owes `u2`; negative = `u2` owes `u1`.

---

### POST /api/settlements
Settle a debt.

**Request Body:**
```json
{
  "fromUserId": "u1",
  "toUserId": "u2",
  "amount": 50.00,
  "paymentMethod": "UPI",
  "referenceId": "UPI_TXN_123"
}
```

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Balance updated |
| 400 | Amount > outstanding debt or non-positive |
| 404 | User not found |

**Failure Cases:**
- Settling more than owed → allow (creates negative balance / credit) or return 400 — choose one and document it
- Concurrent settlements: two users simultaneously settling → use optimistic locking on `balances` row
- Settlement recorded but balance not updated (system crash) → use a DB transaction spanning both writes

---

### GET /api/groups/{groupId}/expenses
List all expenses in a group.

**Query Params:** `page`, `size`, `from` (date), `to` (date)

**Failure Cases:**
- groupId not found → 404
- Caller not a member → 403

---

## Concurrency & Thread-Safety Notes

- `BalanceSheet.updateBalance()` is `synchronized` on the instance — serializes all balance updates. This is a bottleneck at scale; a real system would use DB transactions.
- `BalanceSheet` uses `ConcurrentHashMap` internally, but the `synchronized` wrapper already provides mutual exclusion — the inner `ConcurrentHashMap` provides no additional benefit here. Either use `synchronized` maps or use `ConcurrentHashMap` with CAS operations — mixing both is redundant.
- `SplitwiseService` uses `CopyOnWriteArrayList<ExpenseObserver>` — safe for concurrent iteration, copy-on-write for adds.
- `SplitwiseService` uses `ConcurrentHashMap` for users/groups/expenses — safe.
- `Group.addMember()` checks `!memberIds.contains(userId)` on an `ArrayList` inside an unsynchronized method — **race condition** if two threads add members concurrently. Fix: make `memberIds` a `CopyOnWriteArrayList` or synchronize `addMember`.

---

## Code Review Findings

**Critical:**
- **`Group.addMember()` is not thread-safe.** Two threads can both pass `!memberIds.contains(userId)` and both add the same userId. Fix: use `CopyOnWriteArrayList` or add `synchronized` to the method.
- **Floating-point accumulation in `BalanceSheet`.** Using `double` for monetary amounts (even with rounding) can drift. Use `BigDecimal` for financial calculations.
- **No transaction around expense + balance update.** If the process crashes between `expenses.put(id, expense)` and `balanceSheet.updateBalance(...)`, the expense exists but balances are not updated. Fix: wrap both in a DB transaction in production.

**Design:**
- `BalanceSheet` uses both `synchronized` methods AND `ConcurrentHashMap` — redundant. Simplify to one approach.
- `SplitwiseService.getInstance()` has a `resetInstance()` method — only safe for testing, should be package-private.
- `PercentageSplitStrategy.validate()` uses `instanceof PercentageSplit` check — violates Liskov Substitution in a sense. The `Split` base class could carry `splitType` and the validation could be unified.
- `Expense.splits` is `List.copyOf()` — correctly immutable. But `Split.setAmount()` is mutable. A strategy mutates shared split objects passed in by the caller — coupling that should be made explicit in documentation.

**Minor:**
- `Group.expenseIds` is a `List<String>` — should be a `Set<String>` to prevent duplicate expense IDs.
- `SplitwiseService.addExpense()` validates `paidByUserId` but not each split's `userId` — a split could reference a non-existent user.
- Observer notification happens after balance update; if the observer throws, the exception propagates and the caller may think the expense failed even though balances were already updated.

---

## Extension Points

- **Simplify debts (debt minimization):** Add a `DebtSimplifier` service that reduces N pairwise debts to at most N-1 transactions using a greedy algorithm.
- **Group activity feed:** Add an `ActivityFeedObserver` that appends to a per-group event log on every expense/settlement.
- **Recurring expenses:** Add `recurrence_type` and `next_occurrence_date` to the expenses table; a scheduler creates expenses automatically.
- **Currency support:** Add `currency` to `Expense` and a `FxRate` lookup table; `BalanceSheet` should track debts in a base currency.
