# Online Stock Exchange — LLD Interview Reference

## System Overview

The Online Stock Exchange simulates a brokerage system with order placement, price-time priority matching, partial fills, and real-time portfolio updates. A `StockBrokerageSystem` façade validates orders before forwarding them to the `StockExchange` matching engine. Orders go through a state machine (Open → PartiallyFilled → Filled / Cancelled / Failed). The Observer pattern notifies users of stock price changes, and a Strategy pattern encapsulates limit vs market execution logic.

---

## Core Entities

| Entity | Key Fields | Responsibilities |
|--------|-----------|-----------------|
| `StockBrokerageSystem` | `users`, `stocks`, `stockExchange` | Façade; pre-validates buy/sell orders |
| `StockExchange` | `orderBooks`, `tradeHistory` | Core matching engine; synchronized matching loop |
| `User` | `userId`, `name`, `account` | Implements `StockObserver`; receives price and order notifications |
| `Account` | `balance`, `portfolio` (symbol→quantity) | Manages cash and stock holdings |
| `Stock` | `symbol`, `price`, `observers` | Subject in Observer pattern; notifies on price change |
| `Order` | `orderId`, `user`, `stock`, `type`, `transactionType`, `quantity`, `price`, `executionStrategy`, `status`, `filledQuantity`, `currentState` | Single buy/sell request with state machine |
| `OrderBook` | `symbol`, `buyOrders`, `sellOrders` | Per-symbol sorted queues; `getBestBuy()` and `getBestSell()` |
| `Trade` | `tradeId`, `buyer`, `seller`, `stock`, `quantity`, `price` | Executed trade record |

### Order Lifecycle (State Machine)

```
OPEN → PARTIALLY_FILLED → FILLED
OPEN → CANCELLED
OPEN → FAILED
PARTIALLY_FILLED → CANCELLED
PARTIALLY_FILLED → FILLED
```

### Enums

| Enum | Values |
|------|--------|
| `OrderType` | `LIMIT`, `MARKET` |
| `TransactionType` | `BUY`, `SELL` |
| `OrderStatus` | `OPEN`, `FILLED`, `PARTIALLY_FILLED`, `CANCELLED`, `FAILED` |

---

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | `StockBrokerageSystem`, `StockExchange` | One global exchange; consistent order book |
| **Builder** | `OrderBuilder` | Order has 7+ fields; builder enforces required fields and selects correct strategy |
| **State** | `OrderState` → `OpenState`, `FilledState`, `PartiallyFilledState`, `CancelledState`, `FailedState` | Controls which transitions are valid; prevents cancelling an already-filled order |
| **Observer** | `StockObserver`, `User.update()` | Notify users when stock price changes without coupling Stock to User |
| **Strategy** | `ExecutionStrategy` → `LimitOrderStrategy`, `MarketOrderStrategy` | Encapsulates price-acceptance logic; `canExecute()` called during matching |
| **Façade** | `StockBrokerageSystem` | Shields clients from `StockExchange` internals; adds pre-validation layer |

---

## Database Schema

```sql
-- Users
CREATE TABLE users (
    id          VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    email       VARCHAR(200) NOT NULL UNIQUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Accounts (1-to-1 with user)
CREATE TABLE accounts (
    id              VARCHAR(36)   PRIMARY KEY,
    user_id         VARCHAR(36)   NOT NULL UNIQUE,
    cash_balance    DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    version         BIGINT        NOT NULL DEFAULT 0,  -- optimistic locking
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_cash CHECK (cash_balance >= 0)
);

-- Portfolio positions
CREATE TABLE portfolio_positions (
    account_id  VARCHAR(36)   NOT NULL,
    symbol      VARCHAR(10)   NOT NULL,
    quantity    INT           NOT NULL DEFAULT 0,
    avg_cost    DECIMAL(18,4),  -- average cost basis
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (account_id, symbol),
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT chk_qty CHECK (quantity >= 0)
);

-- Stocks
CREATE TABLE stocks (
    symbol          VARCHAR(10)   PRIMARY KEY,
    company_name    VARCHAR(300)  NOT NULL,
    current_price   DECIMAL(18,4) NOT NULL,
    market          VARCHAR(50),
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Orders
CREATE TABLE orders (
    id                  VARCHAR(36)    PRIMARY KEY,
    user_id             VARCHAR(36)    NOT NULL,
    symbol              VARCHAR(10)    NOT NULL,
    order_type          ENUM('LIMIT','MARKET')                   NOT NULL,
    transaction_type    ENUM('BUY','SELL')                       NOT NULL,
    quantity            INT            NOT NULL,
    limit_price         DECIMAL(18,4),         -- NULL for MARKET orders
    filled_quantity     INT            NOT NULL DEFAULT 0,
    status              ENUM('OPEN','PARTIALLY_FILLED','FILLED','CANCELLED','FAILED') NOT NULL DEFAULT 'OPEN',
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (symbol)  REFERENCES stocks(symbol),
    CONSTRAINT chk_qty    CHECK (quantity > 0),
    CONSTRAINT chk_limit  CHECK (order_type = 'MARKET' OR limit_price IS NOT NULL)
);
CREATE INDEX idx_orders_user   ON orders(user_id, created_at DESC);
CREATE INDEX idx_orders_symbol ON orders(symbol, status);
CREATE INDEX idx_orders_active ON orders(symbol, status, transaction_type, limit_price)
    WHERE status IN ('OPEN','PARTIALLY_FILLED');  -- partial index for matching

-- Trades (append-only)
CREATE TABLE trades (
    id              VARCHAR(36)    PRIMARY KEY,
    buy_order_id    VARCHAR(36)    NOT NULL,
    sell_order_id   VARCHAR(36)    NOT NULL,
    symbol          VARCHAR(10)    NOT NULL,
    quantity        INT            NOT NULL,
    price           DECIMAL(18,4)  NOT NULL,
    buyer_id        VARCHAR(36)    NOT NULL,
    seller_id       VARCHAR(36)    NOT NULL,
    executed_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (buy_order_id)  REFERENCES orders(id),
    FOREIGN KEY (sell_order_id) REFERENCES orders(id),
    FOREIGN KEY (symbol)        REFERENCES stocks(symbol),
    FOREIGN KEY (buyer_id)      REFERENCES users(id),
    FOREIGN KEY (seller_id)     REFERENCES users(id)
);
CREATE INDEX idx_trades_symbol ON trades(symbol, executed_at DESC);
CREATE INDEX idx_trades_buyer  ON trades(buyer_id, executed_at DESC);
CREATE INDEX idx_trades_seller ON trades(seller_id, executed_at DESC);

-- Stock price history (time-series)
CREATE TABLE stock_price_history (
    id          BIGINT         PRIMARY KEY AUTO_INCREMENT,
    symbol      VARCHAR(10)    NOT NULL,
    price       DECIMAL(18,4)  NOT NULL,
    recorded_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (symbol) REFERENCES stocks(symbol)
);
CREATE INDEX idx_price_history_symbol ON stock_price_history(symbol, recorded_at DESC);
```

---

## API Modelling

### POST /api/orders
Place a buy or sell order.

**Request Body (limit buy):**
```json
{
  "userId": "u1",
  "symbol": "AAPL",
  "transactionType": "BUY",
  "orderType": "LIMIT",
  "quantity": 10,
  "limitPrice": 175.00,
  "idempotencyKey": "uuid-v4"
}
```

**Request Body (market sell):**
```json
{
  "userId": "u1",
  "symbol": "AAPL",
  "transactionType": "SELL",
  "orderType": "MARKET",
  "quantity": 5
}
```

**Response 201:**
```json
{
  "orderId": "...",
  "status": "OPEN",
  "symbol": "AAPL",
  "quantity": 10,
  "filledQuantity": 0
}
```

**Responses:**
| Code | Meaning |
|------|---------|
| 201 | Order placed and queued |
| 400 | Missing fields; quantity ≤ 0; limitPrice missing for LIMIT order |
| 402 | Insufficient cash for BUY (LIMIT pre-authorization) |
| 409 | Insufficient shares for SELL |
| 404 | User or symbol not found |
| 422 | Symbol not actively traded |

**Happy Path (LIMIT BUY):**
1. Validate user exists; stock exists and active
2. Estimate cost = `quantity × limitPrice`; check `account.balance >= cost`
3. Create order via `OrderBuilder`, initial state `OPEN`
4. `StockExchange.placeBuyOrder()` → add to `OrderBook`, then `matchOrders()`
5. Matching loop: find `bestBuy` / `bestSell`; call `executeTrade()` if both strategies agree
6. Notify user of status change via `order.notifyUser()`
7. If fully filled → status `FILLED`; if partially → `PARTIALLY_FILLED`

**Failure Cases:**
- Market order with empty order book (no counter-party) → order stays `OPEN` indefinitely; need a market-order expiry policy
- LIMIT BUY insufficient funds check at placement time but price moves up before execution — re-validate at `executeTrade()`
- Concurrent matching on same symbol → `StockExchange` is fully `synchronized` — single-threaded matching per exchange instance (correct but won't scale horizontally)
- Idempotency key re-submission → return existing order

---

### DELETE /api/orders/{orderId}
Cancel an open or partially-filled order.

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Cancelled |
| 400 | Order already FILLED or FAILED |
| 403 | Not the order owner |
| 404 | Order not found |

**Failure Cases:**
- Order is being matched simultaneously → `StockExchange.cancelOrder()` is synchronized; safe
- PARTIALLY_FILLED cancellation: remaining shares released, filled portion already settled
- No refund logic for held funds on cancelled LIMIT BUY — need to release reserved cash

---

### GET /api/orders/{orderId}
Get order status.

**Response 200:**
```json
{
  "orderId": "...",
  "symbol": "AAPL",
  "status": "PARTIALLY_FILLED",
  "quantity": 10,
  "filledQuantity": 6,
  "remainingQuantity": 4,
  "limitPrice": 175.00
}
```

**Failure Cases:**
- 404 if orderId invalid
- 403 if not the order owner

---

### GET /api/orders?userId=&symbol=&status=
List orders for a user (with filters).

**Query Params:** `userId` (required), `symbol` (optional), `status` (optional), `page`, `size`

---

### GET /api/portfolio/{userId}
Get user's stock holdings and cash balance.

**Response 200:**
```json
{
  "userId": "u1",
  "cashBalance": 5000.00,
  "positions": [
    { "symbol": "AAPL", "quantity": 15, "currentPrice": 178.00, "marketValue": 2670.00 }
  ]
}
```

---

### GET /api/stocks/{symbol}/price
Get current price.

**Response 200:**
```json
{ "symbol": "AAPL", "price": 175.50, "updatedAt": "2026-06-20T14:30:00Z" }
```

---

### GET /api/trades?symbol=&from=&to=
Trade history for a symbol.

---

## Concurrency & Thread-Safety Notes

- `StockExchange` is `synchronized` on `this` for all `placeBuyOrder`, `placeSellOrder`, `cancelOrder`, and `matchOrders` calls. This serializes the entire exchange — correct but single-threaded.
- `Account.debit()`, `credit()`, `addStock()`, `removeStock()` are all `synchronized` — correct.
- `OrderBook` uses `CopyOnWriteArrayList` for buy/sell orders — safe for concurrent iteration but `remove()` scans the list (O(n)).
- `Stock.setPrice()` notifies observers synchronously inside the exchange's `synchronized` block — if an observer is slow (e.g. makes an HTTP call), it blocks the entire matching engine. **Fix:** push notifications to an async queue.
- **No cash reservation:** A LIMIT BUY checks cash at placement but doesn't reserve it. If the user places two orders that together exceed their balance, both are accepted and one will fail at execution. **Fix:** debit reserved funds at order placement; refund on cancellation.
- **Race in `OrderBook.getBestBuy/getBestSell`:** These scan `CopyOnWriteArrayList` while the exchange lock is held — safe, but the linear scan is O(n). Use `PriorityQueue` for O(log n) matching.

---

## Code Review Findings

**Critical:**
- **No cash reservation on LIMIT BUY.** `StockBrokerageSystem.placeBuyOrder()` checks `balance >= estimatedCost` but doesn't hold/reserve those funds. A second order placed immediately after can exceed the balance.
- **`Account.portfolio` is a `ConcurrentHashMap` but `Account.balance` is a plain `double`.** The synchronized methods protect `balance`, but `portfolio` operations via `getStockQuantity()` outside synchronized context may see stale values.
- **Observer notification inside exchange lock.** `buyOrder.getStock().setPrice(tradePrice)` calls `notifyObservers()` while `StockExchange` holds `this` monitor. If observers do anything that touches the exchange, deadlock is possible.

**Design:**
- `OrderBook` uses `CopyOnWriteArrayList` — O(n) for `getBestBuy/getBestSell` on every match attempt. Replace with `TreeMap<Double, Queue<Order>>` for O(log n) price-level access (price-time priority).
- `StockExchange` is a god class: it matches orders, executes trades, updates accounts, and manages the order book. Extract `TradeExecutor` for settlement logic.
- `Order.notifyUser()` is called inside `setStatus()` which is called inside the exchange's `synchronized` block — notification tied to state update is fragile. Decouple via an event queue.
- `StockBrokerageSystem.addStock()` creates a `Stock` with no observer registered. The current demo manually calls `stock.addObserver(user)` — there's no automatic subscription mechanism.

**Minor:**
- `Trade` has no `executedAt` timestamp — add `LocalDateTime executedAt = LocalDateTime.now()`.
- `Account.portfolio` is exposed via `getPortfolio()` returning `Map.copyOf()` — good immutability. But `getStockQuantity()` accesses the live map without synchronization when called externally — add `synchronized` or use the copy.
- `OrderBuilder.build()` has no validation — a builder with null `user` or `stock` will produce an NPE later. Add null-checks.

---

## Extension Points

- **Order types:** Add `STOP_LOSS` and `STOP_LIMIT` order types with trigger prices; implement as a new `StopOrderStrategy`.
- **Market data feed:** Publish trade events to a Kafka topic; downstream consumers build OHLC candlestick charts.
- **Risk management:** Add a `RiskEngine` that rejects orders exceeding position limits or concentration thresholds, called inside `StockBrokerageSystem.placeBuyOrder/placeSellOrder`.
- **Short selling:** Allow negative portfolio positions with margin account tracking; `InsufficientStockException` is already thrown but short-selling bypass would need a flag on the account.
