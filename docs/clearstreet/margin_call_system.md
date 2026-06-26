# Margin Call System — Real-Time Risk & Alerting Specification

---

## ▶ PHASE 1 — OPENING (5 min of talk time)

### A. CLARIFYING QUESTIONS
1. **Valuation Frequency:** Is valuation calculated on every price tick (high-frequency streaming), periodically (e.g., every 60 seconds), or is it event-driven when a user places a leveraged trade?
2. **Haircut Multipliers:** Do we apply static flat haircuts (e.g., 20% for large caps, 50% for small caps), or do we support dynamic volatility-based haircuts updated daily?
3. **Auto-Liquidation Threshold:** Does the system trigger auto-liquidation immediately upon maintenance margin breach, or is there a hard grace period (e.g., 24 hours) for manual executive resolution?
4. **Leverage Asset Inclusion:** Are we monitoring long-only equity portfolios, or do we need to calculate short positions and derivatives which require complex margin-offset rules?
5. **Cross-Collateralization:** Do we support multi-asset collateral (e.g., utilizing stock holdings as collateral to purchase other stock holdings), or is cash the only collateral asset?

### B. ASSUMPTIONS & METRICS
*   **Scale:** 100K active margin accounts, 5M mirrored holdings positions evaluated continuously.
*   **Scope:** Equity portfolio monitoring with cash and stock collateral support. Haircut adjustments are applied to stock positions to determine collateral value.
*   **Formula:**
    *   **Market Value of Positions** = $\sum(\text{Position Quantity} \times \text{Current Price})$.
    *   **Haircut-Adjusted Collateral Value** = $\text{Cash Collateral} + \sum(\text{Position Quantity} \times \text{Current Price} \times (1 - \text{Haircut}))$.
    *   **Equity** = $\text{Cash Collateral} + \text{Market Value of Positions} - \text{Outstanding Debt}$.
    *   **Maintenance Margin Limit (MML)** = 30% of Outstanding Debt. If Equity < MML, trigger a Margin Call.
*   **Consistency:** Eventual consistency for mirroring positions (via Kafka CDC), but strict ACID compliance for writing alerts, updating account risk status, and locking records during liquidation.
*   **Currency:** Base currency is INR. All values are stored in integers representing **paise**.

### C. CORE API CONTRACTS

```http
GET /v1/margin/accounts/12345/valuation
Headers:
  Authorization: Bearer <JWT_TOKEN>

Response 200 OK:
{
  "user_id": 12345,
  "margin_status": "WARNING",
  "outstanding_debt_in_paise": 100000000,
  "cash_collateral_in_paise": 20000000,
  "market_value_of_positions_in_paise": 85000000,
  "haircut_adjusted_collateral_in_paise": 88000000,
  "equity_in_paise": 50000000,
  "maintenance_margin_limit_in_paise": 30000000,
  "timestamp": "2026-06-26T19:13:30Z"
}
```

```http
POST /v1/margin/alerts/98765/action
Headers:
  Content-Type: application/json
  X-Idempotency-Key: a4c9ef5a-c5c2-48a0-97f2-95f85023fa99
  Authorization: Bearer <EXECUTIVE_JWT_TOKEN>

Request JSON:
{
  "action_type": "INITIATE_LIQUIDATION",
  "notes": "Margin call not met within 24 hours. Triggering liquidation of stock AAPL."
}

Response 200 OK:
{
  "margin_call_id": 98765,
  "status": "LIQUIDATED",
  "action_recorded_at": "2026-06-26T19:15:00Z"
}
```

---

## ▶ PHASE 2 — FULL DESIGN

### 🏗️ HLD & SERVICE FLOWS

#### A. Core Microservice Boundaries
1. **Position Mirror Service (owns Mirror DB):** Consumes Kafka position events from the core trading system and updates risk positions and debt data.
2. **Pricing Service (owns Redis Cache):** Consumes market data feed ticks and updates prices.
3. **Risk Evaluation Service (owns Policy DB):** Sweeps positions, applies risk models (haircuts), and identifies accounts in breach.
4. **Alerting Service (owns Alert DB):** Manages margin call lifecycle, routes notices, and logs audit activities.

#### B. Step-by-Step Service Communication Flow
1. `Price Feed` —[Kafka Event: stock_price_changed]→ `Pricing Service`: Receives market data pricing ticks.
2. `Pricing Service` —[Redis SET price:{ticker}]→ `Price Cache`: Updates real-time stock price quote.
3. `Risk Evaluation Service` —[Cron Scheduler]→ `Position Mirror Service`: Scans list of active accounts.
4. `Risk Evaluation Service` —[Redis GET price:{ticker}]→ `Price Cache`: Retrieves real-time stock quotes.
5. `Risk Evaluation Service` —[SQL SELECT]→ `PostgreSQL DB`: Calculates total asset value and haircut-adjusted collateral.
6. `Risk Evaluation Service` —[SQL UPDATE]→ `PostgreSQL DB`: Updates `margin_accounts.margin_status` to `BREACHED` using optimistic locking version comparison.
7. `Alerting Service` —[SQL INSERT]→ `PostgreSQL DB`: Writes margin call details to `margin_calls` and writes notification details to `outbox_events`.
8. `Alerting Service` —[Kafka Event: margin_call_triggered]→ `Kafka Broker`: Publishes details to risk alert topic.
9. `Notification Service` —[Kafka Consumer]→ `Client Device`: Sends push/email alert to user.
10. `Executive Portal` —[HTTPS POST /v1/margin/alerts/98765/action]→ `Alerting Service`: Risk officer signs off or starts liquidation.
11. `Alerting Service` —[SQL SELECT FOR UPDATE]→ `PostgreSQL DB`: Locks margin account and transitions state to `LIQUIDATING`.

#### C. Infrastructure Topology
*   **Load Balancer:** Network Load Balancer (NLB) routing TCP connections to WebSocket handlers, and ALB for HTTP routing.
*   **Rate Limiter:** Token Bucket algorithm implemented in Redis. Configured at 60 requests per minute per executive to secure critical actions.
*   **Blob Storage:** AWS S3 for storage of daily margin audit snapshots and liquidation compliance audit reports.
*   **Cache Layers:** Redis Cluster caching stock price quotes (1-sec TTL, volatile-lru eviction) and tracking active alert notifications (10-min TTL).

---

### 🗄️ LLD PART 1 — DATABASE DESIGN & ACCESS PATTERNS

#### A. Detailed Schema Design (DDL style)

```sql
CREATE TABLE margin_accounts (
    id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    user_id BIGINT UNIQUE NOT NULL,
    credit_limit_in_paise BIGINT NOT NULL CONSTRAINT chk_credit_limit CHECK (credit_limit_in_paise >= 0),
    outstanding_debt_in_paise BIGINT NOT NULL DEFAULT 0 CONSTRAINT chk_outstanding_debt CHECK (outstanding_debt_in_paise >= 0),
    cash_collateral_in_paise BIGINT NOT NULL DEFAULT 0 CONSTRAINT chk_cash_collateral CHECK (cash_collateral_in_paise >= 0),
    margin_status VARCHAR(32) NOT NULL DEFAULT 'HEALTHY' CONSTRAINT chk_margin_status CHECK (margin_status IN ('HEALTHY', 'WARNING', 'BREACHED', 'LIQUIDATING')),
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE position_holdings (
    id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    user_id BIGINT NOT NULL REFERENCES margin_accounts(user_id),
    ticker VARCHAR(16) NOT NULL,
    quantity INT NOT NULL CONSTRAINT chk_position_qty CHECK (quantity >= 0),
    version INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uq_margin_user_ticker UNIQUE(user_id, ticker)
);

CREATE TABLE asset_risk_policies (
    ticker VARCHAR(16) PRIMARY KEY,
    haircut_percentage DECIMAL(5,2) NOT NULL CONSTRAINT chk_haircut CHECK (haircut_percentage BETWEEN 0 AND 100),
    maintenance_margin_pct DECIMAL(5,2) NOT NULL DEFAULT 30.00 CONSTRAINT chk_maintenance CHECK (maintenance_margin_pct BETWEEN 0 AND 100),
    is_marginable BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE margin_calls (
    id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    user_id BIGINT NOT NULL,
    equity_at_breach_in_paise BIGINT NOT NULL,
    outstanding_debt_in_paise BIGINT NOT NULL,
    margin_deficit_in_paise BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' CONSTRAINT chk_call_status CHECK (status IN ('PENDING', 'NOTIFIED', 'COLLATERAL_RECEIVED', 'EXECUTIVE_RESOLVED', 'LIQUIDATED')),
    deadline TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE margin_call_actions (
    id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    margin_call_id BIGINT NOT NULL REFERENCES margin_calls(id),
    actor_id BIGINT NOT NULL,
    action_type VARCHAR(64) NOT NULL CONSTRAINT chk_action_type CHECK (action_type IN ('SEND_NOTIFICATION', 'LOG_COLLATERAL_PROMISE', 'INITIATE_LIQUIDATION', 'CLOSE_ALERT')),
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE outbox_events (
    id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED')),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

#### B. Index Design with Query Justifications

```
INDEX: idx_margin_accounts_status ON margin_accounts(margin_status)
Optimized Query:   SELECT * FROM margin_accounts WHERE margin_status = 'BREACHED'
Without index:     Full table scan on accounts to run valuation checks during scheduler sweeps.
Type:              B-tree index.

INDEX: idx_margin_calls_unresolved ON margin_calls(status, created_at DESC) WHERE status IN ('PENDING', 'NOTIFIED')
Optimized Query:   SELECT * FROM margin_calls WHERE status = 'PENDING' ORDER BY created_at DESC
Without index:     Dashboard loads require sorting through millions of historical resolved alerts.
Type:              Partial B-tree (highly compact since active cases are few).

INDEX: idx_margin_calls_user ON margin_calls(user_id, status)
Optimized Query:   SELECT EXISTS(SELECT 1 FROM margin_calls WHERE user_id = :user_id AND status = 'PENDING')
Without index:     Valuator checks entire alert logs before creating a new duplicate call.
Type:              Composite B-tree.
```

#### C. Concurrency & Locking Strategy
*   **Optimistic Locking (`version` column)**: Used by background valuation sweep threads. When marking an account as `BREACHED` after valuation, the engine checks that the version hasn't changed (due to concurrent collateral deposits).
*   **Pessimistic Locking (`SELECT ... FOR UPDATE`)**: Used when a risk executive acts on an alert or triggers liquidation. This blocks concurrent operations on the account and resolves race conditions where collateral is deposited during liquidation.

##### Optimistic Status Update (Valuation Thread)
```sql
UPDATE margin_accounts 
SET margin_status = 'BREACHED', version = version + 1, updated_at = NOW() 
WHERE user_id = :user_id 
  AND version = :expected_version;
```

##### Pessimistic Locking Query (Executive Action Execution)
```sql
BEGIN;
  -- Lock margin call record
  SELECT id, status FROM margin_calls WHERE id = :alert_id FOR UPDATE;
  
  -- Lock the account to prevent external balance updates
  SELECT id, margin_status FROM margin_accounts WHERE user_id = :user_id FOR UPDATE;

  -- Proceed to update status and record action logs...
COMMIT;
```

#### D. NoSQL Decision
Transactional integrity is managed in **PostgreSQL**.
We use **Redis** (NoSQL Key-Value Store) for caching real-time stock prices `price:{ticker}` to calculate Mark-to-Market (MTM) without querying historical databases.

---

## ▶ PHASE 2 — LLD PART 2 — OOD CLASS DESIGN & DESIGN PATTERNS

### A. Core Class Specifications

```
MarginAccount (Entity)
  Attributes: 
    userId: long, creditLimitInPaise: long, outstandingDebtInPaise: long, 
    cashCollateralInPaise: long, marginStatus: MarginStatus, version: int
  Key Methods:
    calculateEquity(portfolioMarketValue: long): long
    calculateMaintenanceMargin(debt: long): long
    isBreachingMaintenance(equity: long, maintenanceMarginLimit: long): boolean

RiskEvaluator (Service)
  Attributes: 
    priceProvider: PriceProvider, riskPolicyRepository: RiskPolicyRepository
  Key Methods:
    evaluateAccount(account: MarginAccount, holdings: List<PositionHolding>): EvaluationResult

AlertService (Service)
  Attributes: 
    alertRepository: MarginCallRepository, outbox: OutboxEventRepository
  Key Methods:
    createAlert(userId: long, deficit: long): MarginCall
    executeAction(alertId: long, action: ActionType, notes: String): void
```

### B. Formal Entity Relationships
*   **Composition:** `MarginCall` owns `MarginCallAction`. Actions (like notification status updates) are strictly dependent on the existence of a parent margin call alert.
*   **Aggregation:** `MarginAccount` has a collection of `PositionHoldings`. Holdings can change, clear, or exist separately from the specific risk parameters.
*   **Association:** `MarginAccount` has a One-to-Many association with `PositionHoldings`.
*   **Inheritance/Realization:** `RiskEvaluator` depends on a `PriceProvider` interface, which is realized by `RedisPriceProvider`.

### C. Applied Design Patterns
*   **Observer Pattern on AlertService:** When a margin call is triggered, the system notifies dashboard widgets, the risk desk, and sends SMS alerts to clients via Kafka consumers.
*   **Strategy Pattern on CollateralValueCalculator:** Different asset categories (equities, cash equivalents, mutual funds) require different discount calculations. The engine uses strategy classes to calculate haircut values.
*   **Transactional Outbox Pattern:** Ensures risk notifications are generated and published reliably by writing events directly to the `outbox_events` table within the Postgres database transaction.

### D. Strict Coding Rules
*   Use `BigDecimal` for computing ratio indices in calculations. Convert result thresholds back to integer paise.
*   Use `SecureRandom` when generating alert tracking tokens.
*   Model account risk states using the State Pattern or enums with validation rules.

---

## ▶ PHASE 2 — LLD PART 3 — EDGE CASES & SENIOR SIGNALS

### 1. Idempotency Checks
To prevent duplicate evaluations and alerts for the same account during price drops:
- We track active margin calls in Redis with the key `active_margin_call:user_id` (expires in 24 hours).
- If a background job evaluates an account and finds it breached, it checks the Redis cache first. If a notification is already flagged, it skips redundant creation.
- The `margin_calls` table enforces a partial index unique constraint: `UNIQUE (user_id) WHERE (status = 'PENDING')`, preventing multiple open alerts.

### 2. Transactional Outbox Pattern
When a margin call triggers, we record the alert and queue the messaging job atomically:
```sql
BEGIN;
  INSERT INTO margin_calls (user_id, equity_at_breach_in_paise, outstanding_debt_in_paise, margin_deficit_in_paise, status, deadline)
  VALUES (:user_id, :equity, :debt, :deficit, 'PENDING', NOW() + INTERVAL '24 hours');

  INSERT INTO outbox_events (aggregate_type, aggregate_id, event_type, payload, status)
  VALUES ('MarginCall', LASTVAL(), 'MARGIN_CALL_TRIGGERED', '{"user_id": 12345, "deficit": 20000}', 'PENDING');
COMMIT;
```
A background daemon reads these outbox entries from PostgreSQL WAL logs and routes them to Kafka.

### 3. Reconciliation Logic
A nightly reconciliation batch job sweeps the risk database:
- It recalculates: `outstanding_debt_in_paise` and compares it against core Ledger entries.
- It verifies: `portfolio_holdings` quantities against core Portfolio database values.
- It tests: If `margin_accounts.margin_status` is marked `HEALTHY`, it executes an independent verification check to confirm that the account's haircut-adjusted collateral exceeds the maintenance margin limit. Any discrepancy triggers a P0 pager alert.

### 4. Rounding or Division Remainder Distribution
When calculating ratio-based haircut adjustments:
- If a stock with a 15% haircut has a price of 999 paise:
  $$\text{Collateral Value} = 999 \times 0.85 = 849.15\text{ paise}$$
- To ensure conservative risk modeling, the system rounds down values representing asset valuations (e.g., $849$ paise instead of $850$).
- For calculations representing user debt, liabilities, or deficits, the system rounds up to ensure potential shortfalls are never under-reported.

---

## ▶ PHASE 2 — LLD PART 4 — CONCEPTUAL DIRECTORY LAYOUT

```
com.slice.risk
├── controller/
│   ├── RiskController.java          # API endpoints for executive portals
│   └── AlertController.java         # API routes for alert status actions
├── service/
│   ├── RiskEvaluationService.java   # Valuation calculations and audits
│   ├── AlertService.java            # Alert creation and escalation pipelines
│   └── PositionUpdateConsumer.java  # Syncs holding adjustments from Kafka
├── repository/
│   ├── MarginAccountRepository.java # Database operations for account records
│   ├── PositionMirrorRepository.java# Queries for mirrored stock positions
│   └── AlertRepository.java         # Alerts logging and audits interface
├── model/
│   ├── MarginAccount.java           # Mirrored cash balances & state
│   ├── PositionHolding.java         # Mirrored asset rows
│   └── MarginCall.java              # Alert parameters log
├── dto/
│   ├── ValuationResponse.java       # MTM valuation response object
│   └── ExecuteActionRequest.java    # Request payload for alert actioning
└── exception/
│   ├── AlertAlreadyResolvedException.java
│   └── StalePriceQuoteException.java
```

---

## 💬 REVISION & CHALLENGE CARDS

*   **"Why not float for money?"**
    *   *Verbal response:* "Using floating-point variables introduces IEEE 754 precision errors. At scale, repeating calculations on fractions leads to rounding deviations (e.g., $0.1 + 0.2 = 0.30000000000000004$). For risk math calculating margin deficits, a 1-paise difference can block trade execution or trigger an illegal margin call. Storing balances as integers in paise avoids precision issues."
*   **"Why cursor pagination over OFFSET?"**
    *   *Verbal response:* "Using `OFFSET` forces the database to read and discard prior records to reach the target index. For dashboards loading historical margin calls, query latency increases with volume ($O(N)$ complexity). Cursor-based pagination uses an indexed comparison (e.g., `WHERE id < :cursor_id`) to retrieve records in $O(\log N)$ time."
*   **"Why pessimistic over optimistic locking for alerts?"**
    *   *Verbal response:* "Optimistic locking is ideal for low-contention operations that can easily be retried. However, for executive actions like triggering liquidation, a conflict retry error is unacceptable. If two executives click 'Liquidate' at the same time, we need the first to lock the row, change the status, and force the second to receive an 'Already Actioned' error. Pessimistic locking (`FOR UPDATE`) guarantees execution serialization."
*   **"Why relational DB over NoSQL for risk mirroring?"**
    *   *Verbal response:* "Risk valuation depends on strict integrity between accounts, positions, risk policies, and active alerts. If we used an eventually consistent NoSQL database, we might read a stale position value while pricing updates, leading to a false margin call. PostgreSQL provides ACID compliance, supporting multi-table transactions to evaluate and update risk status atomically."
