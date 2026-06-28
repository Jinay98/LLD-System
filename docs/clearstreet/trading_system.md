# Trading System — Real-Time Transaction Processing Specification

---

## ▶ PHASE 1 — OPENING (5 min of talk time)

### A. CLARIFYING QUESTIONS
1. **Order Cancellation & Expiry:** Do we support Good 'Til Cancelled (GTC) limit orders that require a daily automated purging mechanism, or do all unexecuted orders expire at market close?
2. **Short Selling & Margin Support:** Is this system cash-backed only (requiring pre-funded wallets and long-only positions), or do we support leveraged trades and short-selling?
3. **Slippage Limits:** Must we enforce maximum slippage checks at the database layer (e.g., failing market orders if the execution price deviates by >1% from the validation-time cached price)?
4. **Fractional Share Execution:** Do we restrict quantities to integer shares, or do we allow fractional share quantities (which would require DECIMAL representations for quantities instead of integers)?
5. **Execution Provider:** Are we matching orders internally (internal dark pool) or routing to external broker-dealers/exchanges and processing execution reports asynchronously?

### B. ASSUMPTIONS & METRICS
*   **Scale:** 1M active daily users, peak throughput of 5,000 Order requests per second (TPS).
*   **Scope:** Cash-backed, long-only equities trading supporting Market and Limit orders with integer share quantities.
*   **Consistency:** Strict database-level transactional ACID consistency for cash balance reservations and holdings.
*   **Pricing:** Real-time stock prices are consumed from an external WebSocket feed, cached in Redis with a 1-second TTL, and used for validation.
*   **Currency:** Base currency is INR. All monetary math is handled in integers representing **paise** (1 INR = 100 paise) to prevent float inaccuracies.

### C. CORE API CONTRACTS

```http
POST /v1/orders
Headers: 
  Content-Type: application/json
  X-Idempotency-Key: 9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d
  Authorization: Bearer <JWT_TOKEN>

Request JSON:
{
  "ticker": "RELIANCE",
  "side": "BUY",
  "order_type": "LIMIT",
  "quantity": 100,
  "limit_price_in_paise": 245050
}

Response 201 Created:
{
  "order_id": 9876543210,
  "status": "PENDING",
  "filled_quantity": 0,
  "created_at": "2026-06-26T19:13:30Z"
}

Response 409 Conflict:
{
  "error_code": "DUPLICATE_REQUEST",
  "message": "Order with this idempotency key already exists."
}

Response 422 Unprocessable Entity:
{
  "error_code": "INSUFFICIENT_FUNDS",
  "message": "Required balance: 24505000 paise. Available balance: 12000000 paise."
}
```

```http
POST /v1/orders/9876543210/cancel
Headers:
  Content-Type: application/json
  X-Idempotency-Key: e3c9ef5a-c5c2-48a0-97f2-95f85023fa38
  Authorization: Bearer <JWT_TOKEN>

Response 200 OK:
{
  "order_id": 9876543210,
  "status": "CANCELLED",
  "released_amount_in_paise": 24505000,
  "updated_at": "2026-06-26T19:14:02Z"
}
```

---

## ▶ PHASE 2 — FULL DESIGN

### 🏗️ HLD & SERVICE FLOWS

#### A. Core Microservice Boundaries
1. **Order Service (owns Order DB):** Validates and registers order requests, manages order state transitions.
2. **Wallet Service (owns Wallet DB):** Tracks cash balances, locks funds for active buy orders, and registers ledger adjustments.
3. **Portfolio Service (owns Portfolio DB):** Tracks asset holdings and average purchase prices, locking shares for active sell orders.
4. **Matching Service (runs in-memory, no persistent DB):** Consumes open orders, executes orderbook matching, and dispatches trade executions.

#### B. Step-by-Step Service Communication Flow
1. `Client` —[HTTPS POST /v1/orders]→ `API Gateway`: Submits order with JWT and `X-Idempotency-Key`.
2. `API Gateway` —[HTTP POST /internal/orders/validate]→ `Order Service`: Authorizes user and forwards order.
3. `Order Service` —[Redis GET price:{ticker}]→ `Price Cache`: Fetches real-time price to validate price thresholds.
4. `Order Service` —[HTTP POST /internal/wallets/:userId/reserve]→ `Wallet Service` (for BUY) or —[HTTP POST /internal/portfolios/:userId/reserve]→ `Portfolio Service` (for SELL): Requests resource reservation.
5. `Wallet Service` —[SQL SELECT FOR UPDATE]→ `PostgreSQL DB`: Locks user's wallet, verifies funds, and shifts money from `balance_in_paise` to `locked_balance_in_paise`.
6. `Order Service` —[SQL INSERT]→ `Order PostgreSQL DB`: Creates `PENDING` order record.
7. `Order Service` —[Kafka Event: order_placed]→ `Kafka Broker`: Publishes order details.
8. `Matching Service` —[Kafka Consumer]→ `Orderbook`: Matches buy/sell limit prices.
9. `Matching Service` —[Kafka Event: trade_executed]→ `Kafka Broker`: Emits execution parameters.
10. `Order Service` —[Kafka Consumer]→ `Order DB`: Receives execution event, triggers atomic settlement transaction updating order state, wallet cash releases, portfolio shares, and inserts `executions` log.

#### C. Tech Stack Summary
```
DB: PostgreSQL  [Why: ACID for fund reservation and execution settlement; UNIQUE on idempotency_key prevents duplicate orders]
Cache: Redis    [Why: real-time stock price quotes (1-sec TTL); idempotency fast-path checks (24-hour TTL)]
Queue: Kafka    [Why: decouple matching engine from order service; decouple settlement notifications from trade execution path]
Read/Write: Write-heavy during market hours (5K TPS). Critical path: reserve funds → insert order → publish to Kafka. Settlement happens async via Kafka consumer.
```

---

### 🗄️ LLD PART 1 — DATABASE DESIGN & ACCESS PATTERNS

#### A. Detailed Schema Design (DDL style)

```sql
CREATE TABLE wallets (
    id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    user_id BIGINT UNIQUE NOT NULL,
    balance_in_paise BIGINT NOT NULL DEFAULT 0 CONSTRAINT chk_wallet_balance CHECK (balance_in_paise >= 0),
    locked_balance_in_paise BIGINT NOT NULL DEFAULT 0 CONSTRAINT chk_wallet_locked CHECK (locked_balance_in_paise >= 0),
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE portfolio_holdings (
    id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    user_id BIGINT NOT NULL,
    ticker VARCHAR(16) NOT NULL,
    quantity INT NOT NULL DEFAULT 0 CONSTRAINT chk_holding_qty CHECK (quantity >= 0),
    locked_quantity INT NOT NULL DEFAULT 0 CONSTRAINT chk_holding_locked CHECK (locked_quantity >= 0),
    average_buy_price_in_paise BIGINT NOT NULL DEFAULT 0 CONSTRAINT chk_holding_avg_price CHECK (average_buy_price_in_paise >= 0),
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uq_user_ticker UNIQUE(user_id, ticker)
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    user_id BIGINT NOT NULL,
    ticker VARCHAR(16) NOT NULL,
    side VARCHAR(8) NOT NULL CONSTRAINT chk_order_side CHECK (side IN ('BUY', 'SELL')),
    order_type VARCHAR(16) NOT NULL CONSTRAINT chk_order_type CHECK (order_type IN ('MARKET', 'LIMIT')),
    limit_price_in_paise BIGINT NULL,
    quantity INT NOT NULL CONSTRAINT chk_order_qty CHECK (quantity > 0),
    filled_quantity INT NOT NULL DEFAULT 0 CONSTRAINT chk_order_filled CHECK (filled_quantity >= 0 AND filled_quantity <= quantity),
    status VARCHAR(32) NOT NULL CONSTRAINT chk_order_status CHECK (status IN ('PENDING', 'PARTIALLY_FILLED', 'FILLED', 'REJECTED', 'CANCELLED')),
    idempotency_key VARCHAR(64) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE executions (
    id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    ticker VARCHAR(16) NOT NULL,
    exec_price_in_paise BIGINT NOT NULL CONSTRAINT chk_exec_price CHECK (exec_price_in_paise > 0),
    exec_quantity INT NOT NULL CONSTRAINT chk_exec_qty CHECK (exec_quantity > 0),
    execution_fee_in_paise BIGINT NOT NULL DEFAULT 0 CONSTRAINT chk_exec_fee CHECK (execution_fee_in_paise >= 0),
    transaction_id VARCHAR(64) UNIQUE NOT NULL,
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
INDEX: idx_wallets_user_id ON wallets(user_id)
Optimized Query:   SELECT * FROM wallets WHERE user_id = :user_id
Without index:     Full table scan on every transaction or order request to check cash availability.
Type:              B-tree unique index.

INDEX: idx_holdings_user_ticker ON portfolio_holdings(user_id, ticker)
Optimized Query:   SELECT * FROM portfolio_holdings WHERE user_id = :user_id AND ticker = :ticker
Without index:     Full table scan of all positions when validating stock sell limits.
Type:              Composite B-tree.

INDEX: idx_orders_ticker_status ON orders(ticker, status) WHERE status IN ('PENDING', 'PARTIALLY_FILLED')
Optimized Query:   SELECT * FROM orders WHERE ticker = :ticker AND status = 'PENDING' AND order_type = 'LIMIT'
Without index:     Matching engine scans inactive orders to find outstanding matches.
Type:              Partial B-tree.

INDEX: idx_executions_timestamp ON executions(created_at DESC)
Optimized Query:   SELECT * FROM executions WHERE created_at < :cursor_timestamp ORDER BY created_at DESC LIMIT 20
Without index:     Full table scan and file sort on every activity page load.
Type:              B-tree.
```

#### C. Concurrency & Locking Strategy
*   **Pessimistic Locking (`SELECT ... FOR UPDATE`)**: Used on wallets and portfolio holdings when settling executions. This prevents race conditions leading to overdrawing funds. Deadlocks are avoided by sorting user IDs and locking the lower user ID first.
*   **Optimistic Locking (`version` column)**: Used for Order status updates by client threads. When cancelling an order, the worker verifies that the expected status state hasn't changed.

##### Pessimistic Locking Query (Settle Executions)
```sql
BEGIN;
  -- Lock wallets in sorted order to avoid deadlock
  SELECT balance_in_paise, locked_balance_in_paise 
  FROM wallets 
  WHERE user_id IN (LEAST(:buyer_id, :seller_id), GREATEST(:buyer_id, :seller_id)) 
  FOR UPDATE;

  -- Lock portfolios in sorted order
  SELECT quantity, locked_quantity 
  FROM portfolio_holdings 
  WHERE user_id IN (LEAST(:buyer_id, :seller_id), GREATEST(:buyer_id, :seller_id)) AND ticker = :ticker
  FOR UPDATE;

  -- Execute adjustments...
COMMIT;
```

##### Optimistic Locking Query (Order Cancellation)
```sql
UPDATE orders 
SET status = 'CANCELLED', updated_at = NOW() 
WHERE id = :order_id 
  AND status = 'PENDING';
-- Checks rows affected; if 0, the order has already been matched or filled.
```

#### D. Key Business Queries

```sql
-- 1. Open orders for a ticker (matching engine query, cursor-paginated)
SELECT id, user_id, side, order_type, limit_price_in_paise, quantity, filled_quantity
FROM orders
WHERE ticker = :ticker
  AND status IN ('PENDING', 'PARTIALLY_FILLED')
  AND id < :cursor_id
ORDER BY id DESC LIMIT 100;
-- Index used: idx_orders_ticker_status (partial B-tree)

-- 2. User order history (cursor-paginated)
SELECT id, ticker, side, order_type, quantity, filled_quantity, status, created_at
FROM orders
WHERE user_id = :user_id AND id < :cursor_id
ORDER BY id DESC LIMIT 20;
-- Add index: idx_orders_user ON orders(user_id, id DESC)

-- 3. Reconciliation — expected portfolio vs. actual
SELECT 
  (COALESCE(SUM(exec_quantity) FILTER (WHERE buyer_id = :user_id), 0) - 
   COALESCE(SUM(exec_quantity) FILTER (WHERE seller_id = :user_id), 0)) AS expected_quantity
FROM executions 
WHERE ticker = :ticker;
-- Compare against portfolio_holdings.quantity for drift detection
```

#### E. NoSQL Decision
Relational PostgreSQL is the primary database for orders, wallets, holdings, and executions to ensure ACID transactions. 
We use **Redis** (NoSQL Key-Value Store) for caching stock price streams to prevent high write throughput from degrading RDBMS performance.
```
Redis Key:   price:{ticker}
Type:        Hash
Fields:      {"price_in_paise": "245050", "timestamp": "1782928822000"}
TTL:         2 seconds
```

---

## ▶ PHASE 2 — LLD PART 2 — OOD CLASS DESIGN & DESIGN PATTERNS

### A. Core Class Specifications

```
Order (Entity)
  Attributes: 
    id: long, userId: long, ticker: String, side: OrderSide, 
    type: OrderType, limitPriceInPaise: long, quantity: int, 
    filledQuantity: int, status: OrderStatus
  Key Methods:
    getRemainingQuantity(): int
    fill(qty: int): void
    cancel(): void

Wallet (Entity)
  Attributes: 
    id: long, userId: long, balanceInPaise: long, 
    lockedBalanceInPaise: long, version: int
  Key Methods:
    reserveFunds(amount: long): void
    releaseFunds(amount: long): void
    settleDebit(amount: long): void
    settleCredit(amount: long): void

OrderValidator (Interface)
  Key Methods:
    validate(order: Order, wallet: Wallet, holding: PortfolioHolding): void

LimitOrderValidator (Realization)
  Attributes: priceProvider: PriceProvider
  Key Methods:
    validate(order: Order, wallet: Wallet, holding: PortfolioHolding): void

TradeSettler (Service)
  Attributes: walletRepository: WalletRepository, portfolioRepository: PortfolioRepository, outbox: OutboxEventRepository
  Key Methods:
    settle(buyerId: long, sellerId: long, ticker: String, quantity: int, price: long, txId: String): ExecutionRecord
```

### B. Dependency Injection Wiring Table

| Class | Depends On (Interface) | Injected Impl |
|---|---|---|
| `OrderController` | `OrderService` | `OrderService` (Spring Bean) |
| `OrderService` | `OrderRepository` | `JpaOrderRepository` |
| `OrderService` | `OrderValidator` | `LimitOrderValidator`, `MarketOrderValidator` |
| `OrderService` | `WalletRepository` | `JpaWalletRepository` |
| `TradeSettler` | `WalletRepository` | `JpaWalletRepository` |
| `TradeSettler` | `PortfolioRepository` | `JpaPortfolioRepository` |
| `TradeSettler` | `OutboxEventRepository` | `JpaOutboxEventRepository` |

### C. Formal Entity Relationships
*   **Composition:** `Order` owns `Execution`. If an order is deleted, its execution records are deleted or archived with it; executions cannot exist without an order.
*   **Aggregation:** `Wallet` has a collection of `LedgerEntries`. If the wallet object is cleared from memory, ledger entries remain in the database as historical records.
*   **Association:** `Wallet` and `User` maintain a One-to-One association. `Order` maintains a Many-to-One association with `User`.
*   **Inheritance/Realization:** `LimitOrderValidator` and `MarketOrderValidator` realize the `OrderValidator` interface.

### D. State Transition Table

| Current State | Trigger / Event | Next State | Guard Condition |
|---|---|---|---|
| `PENDING` | `partialFill(qty)` | `PARTIALLY_FILLED` | filled_quantity < quantity |
| `PENDING` | `fullFill(qty)` | `FILLED` | filled_quantity == quantity |
| `PARTIALLY_FILLED` | `additionalFill(qty)` | `PARTIALLY_FILLED` | still not fully filled |
| `PARTIALLY_FILLED` | `finalFill(qty)` | `FILLED` | filled_quantity == quantity |
| `PENDING` | `cancel()` | `CANCELLED` | optimistic lock check (version) |
| `PARTIALLY_FILLED` | `cancel()` | `CANCELLED` | optimistic lock check |
| `PENDING` | `reject()` | `REJECTED` | insufficient funds or invalid price |
| `FILLED` | — | — | terminal state |
| `CANCELLED` | — | — | terminal state |
| `REJECTED` | — | — | terminal state |

### E. Applied Design Patterns
*   **Strategy Pattern on OrderValidator:** Wraps order validation logic in concrete classes depending on the order side and type. This allows adding new types (such as stop-loss or options) without changing existing core order placement logic.
*   **State Pattern on OrderStatus:** Manages transitions between order states (`PENDING`, `PARTIALLY_FILLED`, `FILLED`, `CANCELLED`). This prevents invalid lifecycle state jumps.
*   **Transactional Outbox Pattern on TradeSettler:** Writes business events (e.g., trade executions) to an `outbox_events` table within the same PostgreSQL transaction that updates balances, ensuring eventual delivery to Kafka.

### F. SOLID Principles Checklist

| Principle | How it shows in this design |
|---|---|
| S — Single Responsibility | OrderService handles order lifecycle; TradeSettler handles atomic settlement; OrderValidator handles price/fund validation |
| O — Open/Closed | OrderValidator interface — add StopLossOrderValidator without modifying OrderService dispatch logic |
| L — Liskov Substitution | LimitOrderValidator and MarketOrderValidator are drop-in replacements for OrderValidator |
| I — Interface Segregation | OrderRepository exposes only save, findById, updateStatus — no unrelated portfolio methods |
| D — Dependency Inversion | OrderService depends on OrderValidator interface, not LimitOrderValidator directly — swappable for tests |

### G. Strict Coding Rules
*   Use `BigDecimal` for precision calculations like interest fees or exchange rates in the application layer. Convert calculations back to integer paise before DB writes.
*   Use `SecureRandom` when generating transaction tokens or API request tracking UUIDs.
*   Model order statuses using the State Pattern or enums with validation rules.

---

## ▶ PHASE 2 — LLD PART 3 — EDGE CASES & SENIOR SIGNALS

### 1. Idempotency Checks
To prevent duplicate execution of identical requests, we use a two-tiered check:
- **Fast Path:** Upon receiving a request, the API Gateway checks Redis for `idempotency_key:X`. If present and marked `SUCCESS`, the cached JSON response is returned immediately. If marked `PROCESSING`, a `202 Accepted` or `409 Conflict` is returned.
- **Slow Path:** The `orders` database table enforces a `UNIQUE` constraint on the `idempotency_key` column. If a duplicate request bypasses the cache, the database transaction fails with a unique constraint violation, prompting a roll back.

### 2. Transactional Outbox Pattern
To prevent dual-write failures (where database updates succeed but Kafka publishing fails):
```sql
-- Done within the same database transaction:
INSERT INTO executions (order_id, buyer_id, seller_id, ticker, exec_price_in_paise, exec_quantity, transaction_id) 
VALUES (:order_id, :buyer, :seller, :ticker, :price, :qty, :tx_id);

INSERT INTO outbox_events (aggregate_type, aggregate_id, event_type, payload, status)
VALUES ('Execution', :tx_id, 'TRADE_EXECUTED', '{"buyer": 1, "seller": 2, "price": 245050}', 'PENDING');
```
A background worker (Debezium / CDC runner) reads the `outbox_events` table using PostgreSQL write-ahead logs (WAL) and publishes the events to Kafka, updating the status to `PROCESSED` once acknowledged by the broker.

### 3. Reconciliation Logic
A nightly reconciliation batch job runs at market close. It validates data integrity across services:
- It queries the append-only `executions` ledger:
  ```sql
  SELECT 
    (COALESCE(SUM(exec_quantity) FILTER (WHERE buyer_id = :user_id), 0) - 
     COALESCE(SUM(exec_quantity) FILTER (WHERE seller_id = :user_id), 0)) AS expected_quantity
  FROM executions 
  WHERE ticker = :ticker;
  ```
- It compares this `expected_quantity` with `portfolio_holdings.quantity`. Any discrepancy triggers a P0 pager alert to the risk operations team.

### 4. Rounding or Division Remainder Distribution
When splitting execution fees (e.g., sharing a 10-paise regulatory fee equally among 3 transactions):
- $10 / 3 = 3$ paise (integer division).
- The remainder of $1$ paise ($10 - (3 \times 3)$) is assigned to the broker's own ledger account as an adjusting entry or added to the execution log of the oldest transaction ID. This guarantees that the sum of parts matches the total transaction value.

---

## ▶ PHASE 2 — LLD PART 4 — CONCEPTUAL DIRECTORY LAYOUT

```
com.clearstreet.trading
├── controller/
│   ├── OrderController.java         # API routes for order placement/cancellation
│   └── WalletController.java        # Balance lookups and manual deposits
├── service/
│   ├── OrderService.java            # Order lifecycle and validation orchestration
│   ├── TradeSettler.java            # Atomic trade database transactions
│   └── MatchingEngineConsumer.java  # Kafka matches listener
├── repository/
│   ├── OrderRepository.java         # JDBC/PostgreSQL interface for orders
│   ├── WalletRepository.java        # Balance SELECT FOR UPDATE execution
│   └── OutboxRepository.java        # Outbox event persist operations
├── model/
│   ├── Order.java                   # Order database entity
│   ├── Wallet.java                  # Wallet balance entity
│   └── Execution.java               # Execution transaction record
├── dto/
│   ├── CreateOrderRequest.java      # JSON mapping for order placement
│   └── OrderResponse.java           # Serialization payload for API responses
└── exception/
│   ├── InsufficientBalanceException.java
│   └── DuplicateRequestException.java
```

---

## 💬 REVISION & CHALLENGE CARDS

*   **"Why not float for money?"**
    *   *Verbal response:* "IEEE 754 floating-point representations cannot represent base-10 fractions like 0.1 or 0.2 accurately. At scale, operations like `0.1 + 0.2` result in rounding errors (e.g., `0.30000000000000004`). In financial systems, these errors compound across millions of transactions, causing balance drift and audit failures. We store money as integers representing paise or cents, which eliminates decimal representation errors."
*   **"Why cursor pagination over OFFSET?"**
    *   *Verbal response:* "Using `LIMIT 20 OFFSET 100000` forces the database to scan and discard 100,000 rows before returning the next 20, resulting in $O(N)$ query times. Cursor-based pagination uses an indexed filter (e.g., `WHERE created_at < :cursor_timestamp`) to jump directly to the target record location, performing in $O(\log N)$ time regardless of query depth."
*   **"Why pessimistic over optimistic locking for wallets?"**
    *   *Verbal response:* "Optimistic locking assumes low contention and forces retries on conflicts. For financial wallets during active trading, transactions are frequent. Forcing users to retry payments because a concurrent update changed the version column results in a poor user experience. Pessimistic locking (`FOR UPDATE`) serializes updates on the database thread pool, queuing incoming transactions safely."
*   **"Why relational DB over NoSQL for ledgers?"**
    *   *Verbal response:* "Financial ledgers require strict atomicity, isolation, and relational integrity. In double-entry bookkeeping, a debit on one account must balance a credit on another. If either operation fails, both must roll back. Relational databases support multi-table ACID transactions, ensuring that balances never drift. NoSQL databases are eventually consistent by default, which can lead to double-spending."
*   **"Walk me through the SOLID principles in your design."**
    *   *Verbal response:* "Single Responsibility: OrderService handles order lifecycle only — fund reservation delegates to Wallet Service via HTTP, settlement delegates to TradeSettler. Open/Closed: OrderValidator interface lets us add StopLossOrderValidator without touching existing validation logic. Liskov: LimitOrderValidator and MarketOrderValidator are identical drop-ins for OrderValidator. Interface Segregation: each repository interface exposes only what its consumer needs — TradeSettler's WalletRepository only has reserveFunds and settleDebit. Dependency Inversion: OrderService depends on OrderValidator and WalletRepository interfaces — concrete JPA or HTTP implementations are injected, making the service testable with in-memory stubs."
