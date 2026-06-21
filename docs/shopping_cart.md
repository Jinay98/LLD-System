# Shopping Cart — LLD Interview Reference

## System Overview

The Shopping Cart system models an e-commerce cart with item management, per-item quantity enforcement, three interchangeable discount strategies, and an Observer-based event pipeline for logging and abandoned-cart detection. A `ShoppingCartService` singleton manages cart lifecycle; `Cart` is the aggregate root that owns `CartItem`s and enforces state transitions. A `DiscountStrategy` is applied at total calculation time so discounts can be swapped without modifying cart internals. The Observer pattern allows `CartEventLogger` and `AbandonedCartAlertObserver` to react to item additions, removals, and checkout events without Cart depending on them directly.

---

## Core Entities

| Entity | Key Fields | Responsibilities |
|--------|-----------|-----------------|
| `ShoppingCartService` | `carts` (ConcurrentHashMap), `cartCounter` (AtomicInteger) | Singleton; creates and retrieves carts; enforces one active cart per customer |
| `Cart` | `id`, `customer`, `items` (ConcurrentHashMap), `observers`, `status`, `discountStrategy` | Aggregate root; all mutations synchronized; emits Observer events |
| `CartItem` | `product`, `quantity`, `priceAtAddition` | Immutable price snapshot + mutable quantity; price locked at add time |
| `Customer` | `id`, `name`, `email` | Identity; used to enforce one-active-cart rule |
| `Product` | `id`, `name`, `price`, `category`, `maxQuantityPerCart` | Catalog item; enforces per-cart quantity ceiling |

### Cart Lifecycle (State Machine)

```
ACTIVE → CHECKED_OUT
ACTIVE → ABANDONED
```
Once the cart leaves `ACTIVE`, no mutations are allowed (`validateActive()` guards every write method).

### Enums

| Enum | Values |
|------|--------|
| `CartStatus` | `ACTIVE`, `CHECKED_OUT`, `ABANDONED` |
| `DiscountType` | `PERCENTAGE`, `FLAT_AMOUNT`, `BUY_X_GET_Y_FREE` |
| `ProductCategory` | `ELECTRONICS`, `CLOTHING`, `GROCERIES`, `BOOKS`, `HOME_AND_KITCHEN` |

---

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | `ShoppingCartService` | One service manages all carts; consistent cart counter and lookup map |
| **Strategy** | `DiscountStrategy` → `PercentageDiscountStrategy`, `FlatAmountDiscountStrategy`, `BuyXGetYFreeStrategy` | Swap discount algorithm at runtime; `calculateDiscount(items)` is called lazily at `getTotal()` so the same cart reflects a new coupon immediately |
| **Observer** | `CartObserver` → `CartEventLogger`, `AbandonedCartAlertObserver` | Decouple event consumers from `Cart`; adding a new observer (e.g., inventory reservation) requires no change to `Cart` |

---

## Database Schema

```sql
-- Customers
CREATE TABLE customers (
    id          VARCHAR(36)   PRIMARY KEY,
    name        VARCHAR(200)  NOT NULL,
    email       VARCHAR(200)  NOT NULL UNIQUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Product catalog
CREATE TABLE products (
    id                    VARCHAR(36)    PRIMARY KEY,
    name                  VARCHAR(300)   NOT NULL,
    price                 DECIMAL(10,2)  NOT NULL,  -- use DECIMAL, never DOUBLE, for money
    category              VARCHAR(30)    NOT NULL,
    max_quantity_per_cart INT            NOT NULL DEFAULT 10,
    is_active             BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_product_price    CHECK (price >= 0),
    CONSTRAINT chk_product_max_qty  CHECK (max_quantity_per_cart > 0),
    CONSTRAINT chk_product_category CHECK (category IN (
        'ELECTRONICS','CLOTHING','GROCERIES','BOOKS','HOME_AND_KITCHEN'
    ))
);

CREATE INDEX idx_products_category ON products(category);

-- Shopping carts
CREATE TABLE carts (
    id              VARCHAR(36)  PRIMARY KEY,
    customer_id     VARCHAR(36)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    -- Discount snapshot (denormalised for audit; NULL when no discount applied)
    discount_type   VARCHAR(30)  NULL,
    discount_value  DECIMAL(10,2) NULL,  -- percentage or flat amount
    discount_category VARCHAR(30) NULL,  -- for BUY_X_GET_Y_FREE: which category
    discount_buy_count  INT      NULL,   -- for BUY_X_GET_Y_FREE: X
    discount_free_count INT      NULL,   -- for BUY_X_GET_Y_FREE: Y
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    CONSTRAINT chk_cart_status CHECK (status IN ('ACTIVE','CHECKED_OUT','ABANDONED'))
);

CREATE INDEX idx_carts_customer_status ON carts(customer_id, status);
-- ^ critical: enforces one-active-cart lookup in O(log n)

-- Cart items
CREATE TABLE cart_items (
    id                  VARCHAR(36)    PRIMARY KEY,
    cart_id             VARCHAR(36)    NOT NULL,
    product_id          VARCHAR(36)    NOT NULL,
    quantity            INT            NOT NULL,
    price_at_addition   DECIMAL(10,2)  NOT NULL,  -- snapshot; insulates against catalog price changes
    added_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (cart_id, product_id),                 -- one row per product per cart
    FOREIGN KEY (cart_id)    REFERENCES carts(id)    ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT chk_cart_item_qty CHECK (quantity > 0)
);

CREATE INDEX idx_cart_items_cart ON cart_items(cart_id);

-- Cart event log (for analytics / abandoned cart audit trail)
CREATE TABLE cart_events (
    id          BIGINT        AUTO_INCREMENT PRIMARY KEY,
    cart_id     VARCHAR(36)   NOT NULL,
    event_type  VARCHAR(30)   NOT NULL,  -- ITEM_ADDED | ITEM_REMOVED | ITEM_UPDATED | CHECKOUT | ABANDONED
    product_id  VARCHAR(36)   NULL,
    quantity    INT           NULL,
    occurred_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cart_id)    REFERENCES carts(id)    ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL
);

CREATE INDEX idx_cart_events_cart ON cart_events(cart_id, occurred_at DESC);
```

> **Schema notes:**
> - `price_at_addition` locks the price seen by the customer at add-time, matching `CartItem.priceAtAddition` in the Java code. This is intentional — a product price change does not retro-affect an open cart.
> - The discount columns are nullable and denormalised into `carts` to avoid a separate `cart_discounts` table for this single-discount-per-cart model.
> - `cart_events` replaces the in-memory `CartEventLogger` observer for persistence. The `AbandonedCartAlertObserver` logic (inactivity detection) is handled by a scheduled job querying `cart_events` for carts with `status = ACTIVE` and no event in the last N hours.

---

## API Modelling

### POST /api/v1/carts
Create a new cart for a customer (or return the existing active one).

**Request Body:**
```json
{
  "customerId": "CUST-001"
}
```

**Response:** `201 Created` (new) or `200 OK` (returning existing active cart)
```json
{
  "cartId": "CART-1",
  "customerId": "CUST-001",
  "status": "ACTIVE"
}
```

| Status | Meaning |
|--------|---------|
| `200 OK` | Active cart already exists; returned it |
| `201 Created` | New cart created |
| `404 Not Found` | Customer does not exist |

**Happy path:**
1. Look up active cart for `customerId`.
2. If found, return `200` with existing cart.
3. Otherwise create new `Cart`, persist, return `201`.

**Failure cases:**
- Customer not found → `404`

---

### POST /api/v1/carts/{cartId}/items
Add a product to the cart (or increment quantity if already present).

**Request Body:**
```json
{
  "productId": "PROD-42",
  "quantity": 2
}
```

**Response:**
| Status | Meaning |
|--------|---------|
| `200 OK` | Item added/updated |
| `400 Bad Request` | Quantity ≤ 0 or would exceed `maxQuantityPerCart` |
| `404 Not Found` | Cart or product not found |
| `409 Conflict` | Cart is not in ACTIVE status |

**Happy path:**
1. Resolve cart and product.
2. Validate quantity > 0.
3. Check `currentQuantity + newQuantity <= maxQuantityPerCart`.
4. Snapshot `product.price` as `priceAtAddition`.
5. Insert/update `cart_items` row.
6. Emit `ITEM_ADDED` event.

**Failure cases:**
- Cart is `CHECKED_OUT` or `ABANDONED` → `409`
- `quantity <= 0` → `400`
- `currentQty + quantity > maxQuantityPerCart` → `400` with message stating current and max
- Product inactive or not found → `404`

---

### PUT /api/v1/carts/{cartId}/items/{productId}
Set the quantity of a specific item (0 = remove the item).

**Request Body:**
```json
{
  "quantity": 3
}
```

**Response:**
| Status | Meaning |
|--------|---------|
| `200 OK` | Quantity updated (or item removed if quantity = 0) |
| `400 Bad Request` | Quantity exceeds `maxQuantityPerCart` |
| `404 Not Found` | Cart or item not found |
| `409 Conflict` | Cart not ACTIVE |

**Failure cases:**
- `quantity > maxQuantityPerCart` → `400`
- Item not in cart → `404`
- Cart not ACTIVE → `409`

---

### DELETE /api/v1/carts/{cartId}/items/{productId}
Remove an item from the cart entirely.

**Response:**
| Status | Meaning |
|--------|---------|
| `204 No Content` | Item removed |
| `404 Not Found` | Cart or item not found |
| `409 Conflict` | Cart not ACTIVE |

---

### POST /api/v1/carts/{cartId}/discount
Apply a discount strategy to the cart.

**Request Body:**
```json
{
  "type": "PERCENTAGE",
  "value": 10.0
}
```
Or for BUY_X_GET_Y_FREE:
```json
{
  "type": "BUY_X_GET_Y_FREE",
  "buyCount": 2,
  "freeCount": 1,
  "category": "GROCERIES"
}
```

**Response:**
| Status | Meaning |
|--------|---------|
| `200 OK` | Discount applied; returns updated total |
| `400 Bad Request` | Invalid discount parameters |
| `404 Not Found` | Cart not found |
| `409 Conflict` | Cart not ACTIVE |

**Failure cases:**
- `percentage` outside 0–100 → `400`
- Negative flat amount → `400`
- `buyCount` or `freeCount` ≤ 0 → `400`
- Cart not ACTIVE → `409`

---

### DELETE /api/v1/carts/{cartId}/discount
Remove the currently applied discount.

**Response:** `204 No Content`

---

### POST /api/v1/carts/{cartId}/checkout
Finalise the cart and hand off to the order service.

**Response:**
| Status | Meaning |
|--------|---------|
| `200 OK` | Cart checked out; returns final total |
| `400 Bad Request` | Cart is empty |
| `404 Not Found` | Cart not found |
| `409 Conflict` | Cart already checked out or abandoned |

**Happy path:**
1. Validate cart is `ACTIVE` and non-empty.
2. Compute final total (subtotal − discount).
3. Transition `status = CHECKED_OUT`.
4. Emit `CHECKOUT` event.
5. Publish order-creation event to downstream order service.
6. Return `{ "cartId": "CART-1", "total": 89.97 }`.

**Failure cases:**
- Empty cart → `400` (`CartException`: "Cannot checkout an empty cart")
- Cart not ACTIVE → `409`
- Downstream order service unavailable → return `202 Accepted` and enqueue with retry (idempotency key = `cartId`)

---

### GET /api/v1/carts/{cartId}
Retrieve cart details including current total.

**Response:** `200 OK`
```json
{
  "cartId": "CART-1",
  "customer": { "id": "CUST-001", "name": "Alice" },
  "status": "ACTIVE",
  "items": [
    { "productId": "PROD-42", "name": "Laptop", "quantity": 1, "priceAtAddition": 999.00 }
  ],
  "subtotal": 999.00,
  "discount": { "type": "PERCENTAGE", "value": 10.0, "amount": 99.90 },
  "total": 899.10
}
```

| Status | Meaning |
|--------|---------|
| `200 OK` | Cart returned |
| `404 Not Found` | Cart does not exist |

---

## Concurrency & Thread-Safety Notes

| Shared State | Protection | Notes |
|-------------|-----------|-------|
| `ShoppingCartService` singleton | Double-checked locking + `volatile instance` | Correct DCL; `instance` is `volatile` |
| `ShoppingCartService.carts` | `ConcurrentHashMap` | Thread-safe for individual puts/reads |
| `Cart.items` | `ConcurrentHashMap` + `synchronized` methods | `synchronized` on `Cart` serialises all writes; ConcurrentHashMap is redundant (see findings) |
| `Cart.status`, `Cart.discountStrategy` | `volatile` + `synchronized` methods | `volatile` ensures visibility; `synchronized` ensures atomic read-modify-write |
| `Cart.observers` | `CopyOnWriteArrayList` | Safe for concurrent iteration during notification while another thread might add an observer |
| `ShoppingCartService.cartCounter` | `AtomicInteger` | Lock-free ID generation |
| `AbandonedCartAlertObserver.lastActivityTime` | `volatile long` | Single writer (synchronized Cart methods) + volatile ensures reader sees latest value |

**Race condition — `getActiveCartForCustomer()` is not atomic:**  
Two threads creating a cart for the same customer can both find no active cart and both call `createCart()`, resulting in two active carts for one customer. The `ConcurrentHashMap` does not prevent this TOCTOU. Fix: use `computeIfAbsent` with a per-customer lock, or add a `UNIQUE (customer_id)` partial index on `carts` where `status = 'ACTIVE'` and let the DB enforce it at insert time.

---

## Code Review Findings

**Critical**
- `ShoppingCartService.getInstance()` is package-private (no `public` modifier). Callers in other packages cannot get the singleton — the service is effectively inaccessible outside the `services` package. Should be `public`.
- `double` / `float` used for `Product.price` and `CartItem.priceAtAddition`. Floating-point arithmetic causes rounding errors in monetary calculations (e.g., `0.1 + 0.2 ≠ 0.3`). Replace with `BigDecimal`.
- `Customer` and `Product` constructors are package-private, so they cannot be instantiated from outside the `entities` package. The system has no factory, builder, or public constructor — callers have no way to create these objects. Add a `public` static factory method or builder.
- `Cart.checkout()`, `Cart.addItem()`, and other mutating methods are package-private. `ShoppingCartService` is in a separate sub-package (`services`) so it cannot call them. The system cannot be used end-to-end as written.

**Design**
- `Cart.items` is `ConcurrentHashMap` but all access goes through `synchronized` methods on `Cart` — the map is always accessed under `Cart`'s intrinsic lock, so `ConcurrentHashMap` adds overhead without benefit. Use `HashMap` instead.
- `AbandonedCartAlertObserver` records `lastActivityTime` but never sends an alert — there is no scheduled job to detect inactivity. Without a scheduler querying `lastActivityTime`, the observer is a no-op. Either wire up a `ScheduledExecutorService` or move this logic to the DB layer.
- `getActiveCartForCustomer()` is O(n) scan over all carts. For any non-trivial load, this should be an indexed DB query or an in-memory `Map<customerId, cartId>` for the active cart.
- Only one `DiscountStrategy` can be applied at a time. Stacking coupons (e.g., a percentage + a buy-X-get-Y) is a common requirement. Consider a `CompositeDiscountStrategy` that chains multiple strategies.

**Minor**
- `BuyXGetYFreeStrategy` gives free items to the cheapest units (ascending sort), which benefits the customer. This may be the intended behaviour, but it should be documented — interviewers may ask whether the merchant or customer benefits from the tie-breaking rule.
- `CartStatus` has inline comments in the enum body (e.g., `// Cart can be modified`) — these are fine but could be Javadoc to make them visible in IDEs.
- `Cart.getSubtotal()` is package-private while `getTotal()` is public — inconsistent visibility; both are read operations that external callers likely need.

---

## Extension Points

- **Saved-for-later / wishlist:** Add a `SAVED` status between `ACTIVE` and `ABANDONED`; items can be moved between the active cart and a saved list without needing a new entity.
- **Inventory reservation:** Add an `InventoryReservationObserver` that calls the inventory service on `onItemAdded` and releases the reservation on `onItemRemoved` or `onCartAbandoned`; no change to `Cart` needed.
- **Multi-discount stacking:** Replace `DiscountStrategy discountStrategy` (single) with `List<DiscountStrategy> discountStrategies`; `getTotal()` applies them in order; promotional rules (e.g., only one discount per category) are enforced by the strategy composition.
- **Guest cart → account merge:** When a guest checks out and creates an account, merge the anonymous cart by reassigning `cart.customer` to the new customer entity; `ShoppingCartService.createCart()` would need a `mergeGuestCart(guestCartId, customerId)` path.
