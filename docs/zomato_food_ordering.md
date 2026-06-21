# Zomato (Food Ordering System) — LLD Interview Reference

## System Overview

The Zomato clone allows users to browse restaurants by location, add items to a cart, place orders, and receive notifications. Strategy patterns handle payment (Credit Card, UPI) and notifications (Email, SMS). Factory classes centralize creation of payment and notification strategies. An `OrderingService` orchestrates the full order flow. The system currently lacks persistent storage — all state is in-memory via `OrderManager` and `RestaurantManager` Singletons.

---

## Core Entities

| Entity | Key Fields | Responsibilities |
|--------|-----------|-----------------|
| `User` | `name`, `cardNo`, `mobileNo`, `location`, `cartManager` | Customer; holds a `CartManager` reference |
| `Restaurant` | `id`, `name`, `location`, `menu` (List<FoodItem>) | Registered food venue with a menu |
| `FoodItem` | `id`, `code`, `name`, `price` | One menu item |
| `Cart` | `restaurant`, `cartItems` | Transient shopping basket for one restaurant |
| `CartItem` | `foodItem`, `qty` | Line item in the cart with `getSubTotal()` |
| `Order` | `id`, `user`, `restaurant`, `orderType`, `orderItems`, `totalAmt`, `status`, `orderedAt` | Confirmed purchase record |
| `OrderItem` | `foodItem`, `qty`, `priceAtOrderTime` | Snapshot of item price at order time |

### Enums

| Enum | Values |
|------|--------|
| `OrderTypes` | `ORDER_NOW`, `SCHEDULE_FOR_LATER` |
| `PaymentStrategies` | `CREDIT_CARD`, `UPI` |
| `NotificationStrategies` | `SMS`, `EMAIL` |

---

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | `OrderManager.getInstance()`, `RestaurantManager.getInstance()` | Global registries for orders and restaurants |
| **Strategy** | `IPaymentStrategy` (`CreditCardPaymentStrategy`, `UPIPaymentStrategy`) | Swap payment provider without touching order flow |
| **Strategy** | `INotificationStrategy` (`SMSNotificationStrategy`, `EmailNotificationStrategy`) | Swap notification channel without touching order flow |
| **Factory** | `PaymentFactory`, `NotificationFactory`, `OrderFactory` | Centralize creation; clients pass strategy names as strings |
| **Template Method** (implicit) | `OrderingService.placeOrder()` | Fixed sequence: create → pay → save → notify → clear cart |

---

## Database Schema

```sql
-- Users
CREATE TABLE users (
    id              VARCHAR(36)  PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    email           VARCHAR(200) NOT NULL UNIQUE,
    phone           VARCHAR(20)  NOT NULL UNIQUE,
    address         TEXT,
    latitude        DECIMAL(10,7),
    longitude       DECIMAL(10,7),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE INDEX idx_users_location ON users(latitude, longitude);

-- Restaurants
CREATE TABLE restaurants (
    id              VARCHAR(36)  PRIMARY KEY,
    name            VARCHAR(300) NOT NULL,
    address         TEXT         NOT NULL,
    latitude        DECIMAL(10,7) NOT NULL,
    longitude       DECIMAL(10,7) NOT NULL,
    cuisine_type    VARCHAR(100),
    rating          DECIMAL(3,2),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    opening_time    TIME,
    closing_time    TIME,
    avg_delivery_mins INT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_restaurants_location ON restaurants(latitude, longitude);
CREATE FULLTEXT INDEX idx_restaurants_search ON restaurants(name, cuisine_type);

-- Menu items
CREATE TABLE food_items (
    id              VARCHAR(36)  PRIMARY KEY,
    restaurant_id   VARCHAR(36)  NOT NULL,
    code            VARCHAR(50)  NOT NULL,
    name            VARCHAR(300) NOT NULL,
    description     TEXT,
    price           DECIMAL(10,2) NOT NULL,
    category        VARCHAR(100),
    is_veg          BOOLEAN      NOT NULL DEFAULT TRUE,
    is_available    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (restaurant_id, code),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
);
CREATE INDEX idx_food_items_restaurant ON food_items(restaurant_id, is_available);

-- Carts (persisted for session recovery)
CREATE TABLE carts (
    id              VARCHAR(36)  PRIMARY KEY,
    user_id         VARCHAR(36)  NOT NULL,
    restaurant_id   VARCHAR(36)  NOT NULL,
    status          ENUM('ACTIVE','CHECKED_OUT','ABANDONED') NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (user_id, status),  -- only one ACTIVE cart per user
    FOREIGN KEY (user_id)       REFERENCES users(id)       ON DELETE CASCADE,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE RESTRICT
);

-- Cart line items
CREATE TABLE cart_items (
    id              VARCHAR(36)  PRIMARY KEY,
    cart_id         VARCHAR(36)  NOT NULL,
    food_item_id    VARCHAR(36)  NOT NULL,
    quantity        INT          NOT NULL,
    added_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (cart_id, food_item_id),
    FOREIGN KEY (cart_id)      REFERENCES carts(id)      ON DELETE CASCADE,
    FOREIGN KEY (food_item_id) REFERENCES food_items(id) ON DELETE RESTRICT,
    CONSTRAINT chk_qty CHECK (quantity > 0)
);

-- Orders
CREATE TABLE orders (
    id              VARCHAR(36)   PRIMARY KEY,
    user_id         VARCHAR(36)   NOT NULL,
    restaurant_id   VARCHAR(36)   NOT NULL,
    order_type      ENUM('ORDER_NOW','SCHEDULE_FOR_LATER') NOT NULL,
    scheduled_for   TIMESTAMP,                     -- only for SCHEDULE_FOR_LATER
    status          ENUM('PENDING','CONFIRMED','PREPARING','OUT_FOR_DELIVERY','DELIVERED','CANCELLED','REFUNDED')
                    NOT NULL DEFAULT 'PENDING',
    total_amount    DECIMAL(10,2) NOT NULL,
    delivery_address TEXT         NOT NULL,
    payment_method  ENUM('CREDIT_CARD','UPI','CASH_ON_DELIVERY') NOT NULL,
    payment_status  ENUM('PENDING','SUCCESS','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    payment_ref     VARCHAR(100),
    ordered_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)       REFERENCES users(id)       ON DELETE RESTRICT,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE RESTRICT
);
CREATE INDEX idx_orders_user       ON orders(user_id, ordered_at DESC);
CREATE INDEX idx_orders_restaurant ON orders(restaurant_id, status);

-- Order line items (snapshot at order time)
CREATE TABLE order_items (
    id                  VARCHAR(36)   PRIMARY KEY,
    order_id            VARCHAR(36)   NOT NULL,
    food_item_id        VARCHAR(36)   NOT NULL,
    food_item_name      VARCHAR(300)  NOT NULL,     -- snapshot
    quantity            INT           NOT NULL,
    price_at_order_time DECIMAL(10,2) NOT NULL,     -- snapshot
    FOREIGN KEY (order_id)     REFERENCES orders(id)      ON DELETE CASCADE,
    FOREIGN KEY (food_item_id) REFERENCES food_items(id)  ON DELETE RESTRICT
);

-- Delivery agents
CREATE TABLE delivery_agents (
    id          VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    phone       VARCHAR(20)  NOT NULL UNIQUE,
    status      ENUM('AVAILABLE','BUSY','OFFLINE') NOT NULL DEFAULT 'OFFLINE',
    latitude    DECIMAL(10,7),
    longitude   DECIMAL(10,7),
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Delivery assignments
CREATE TABLE deliveries (
    id              VARCHAR(36)  PRIMARY KEY,
    order_id        VARCHAR(36)  NOT NULL UNIQUE,
    agent_id        VARCHAR(36)  NOT NULL,
    assigned_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    picked_up_at    TIMESTAMP,
    delivered_at    TIMESTAMP,
    status          ENUM('ASSIGNED','PICKED_UP','DELIVERED','FAILED') NOT NULL DEFAULT 'ASSIGNED',
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (agent_id) REFERENCES delivery_agents(id)
);
```

---

## API Modelling

### GET /api/restaurants?location=&cuisineType=&page=
Search restaurants near a location.

**Query Params:** `location` or `lat`/`lng` (required), `cuisineType` (optional), `isVeg` (optional), `minRating` (optional), `page`, `size`

**Response 200:**
```json
{
  "restaurants": [
    {
      "id": "r1",
      "name": "Biryani House",
      "cuisineType": "Indian",
      "rating": 4.2,
      "avgDeliveryMins": 35,
      "isActive": true
    }
  ],
  "page": 1,
  "total": 42
}
```

**Failure Cases:**
- No location provided → 400
- Location string not geocodable → 422
- Restaurants outside delivery radius → filter server-side, not client-side

---

### GET /api/restaurants/{restaurantId}/menu
Get menu for a restaurant.

**Response 200:**
```json
{
  "restaurantId": "r1",
  "categories": [
    {
      "name": "Starters",
      "items": [
        { "id": "f1", "code": "VEG01", "name": "Paneer Tikka", "price": 199.00, "isVeg": true, "isAvailable": true }
      ]
    }
  ]
}
```

**Failure Cases:**
- Restaurant closed → still return menu but mark `isAvailable: false` on all items
- Restaurant not found → 404

---

### POST /api/carts
Create or get active cart.

**Request Body:**
```json
{ "userId": "u1", "restaurantId": "r1" }
```

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Existing active cart returned |
| 201 | New cart created |
| 409 | User has an active cart for a different restaurant — must clear first |
| 404 | User or restaurant not found |

---

### PUT /api/carts/{cartId}/items
Add or update item in cart.

**Request Body:**
```json
{ "foodItemId": "f1", "quantity": 2 }
```

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Cart updated |
| 400 | Quantity ≤ 0 |
| 404 | Cart or food item not found |
| 409 | Food item from different restaurant than cart's restaurant |
| 422 | Food item not currently available |

**Failure Cases:**
- Quantity = 0 → remove item from cart (or return 400 — choose one)
- Item unavailable (restaurant marked it out of stock) → 422 with friendly message
- Cart expired (user inactive for >30 minutes) → 410

---

### POST /api/orders
Place an order.

**Request Body:**
```json
{
  "userId": "u1",
  "cartId": "c1",
  "orderType": "ORDER_NOW",
  "deliveryAddress": "123 Main St",
  "paymentMethod": "CREDIT_CARD",
  "cardNumber": "4111111111111111",
  "idempotencyKey": "uuid-v4"
}
```

**Response 201:**
```json
{
  "orderId": "o1",
  "status": "CONFIRMED",
  "totalAmount": 598.00,
  "estimatedDelivery": "2026-06-20T20:05:00Z"
}
```

**Responses:**
| Code | Meaning |
|------|---------|
| 201 | Order placed and confirmed |
| 400 | Empty cart; missing delivery address |
| 402 | Payment failed |
| 404 | User, cart, or restaurant not found |
| 422 | Restaurant is closed; one or more items no longer available |
| 409 | Idempotency key already used — return existing order |

**Happy Path:**
1. Validate cart is not empty
2. Re-validate each item is still available (prices may have changed — use `priceAtOrderTime` snapshot)
3. Calculate total
4. Process payment
5. Create order + order items (snapshot current prices)
6. Mark cart as `CHECKED_OUT`
7. Assign delivery agent
8. Send notification to user
9. Return 201

**Failure Cases:**
- Payment succeeds but restaurant then marks as closed → refund payment, cancel order
- Item price changed between cart add and order placement → use `priceAtOrderTime` from OrderItem; warn user if price increased
- Same cart ordered twice (double-click) → idempotency key prevents duplicate
- Delivery agent unavailable → mark order `PENDING_ASSIGNMENT`; retry assignment via a background job

---

### GET /api/orders/{orderId}
Get order status.

**Response 200:**
```json
{
  "orderId": "o1",
  "status": "OUT_FOR_DELIVERY",
  "agentName": "Ravi Kumar",
  "estimatedArrival": "2026-06-20T20:05:00Z",
  "items": [...]
}
```

---

### PATCH /api/orders/{orderId}/cancel
Cancel an order.

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Cancelled, refund initiated |
| 400 | Order already out for delivery or delivered |
| 403 | Not the order owner |

---

## Concurrency & Thread-Safety Notes

- `OrderManager` is a plain Singleton with a non-thread-safe lazy-init check (`if (instance == null)`) — **not thread-safe**. Two threads can create two instances. Fix: use `synchronized` or `volatile` double-checked locking.
- `RestaurantManager` has the same thread-safety issue.
- `CartManager` is stateless — thread-safe.
- `OrderingService.placeOrder()` is a static method with no synchronization — if two threads place orders for the same user simultaneously, both can complete. Idempotency key is the defence.
- There is no locking on food item availability — a user can add an item that is marked unavailable between cart add and order placement. Re-validate at order time.
- `OrderManager.orders` is an `ArrayList` — concurrent adds can cause `ArrayIndexOutOfBoundsException`. Fix: use `CopyOnWriteArrayList` or `synchronized`.

---

## Code Review Findings

**Critical:**
- **`OrderManager` and `RestaurantManager` have non-thread-safe Singleton initialization.** No `volatile`, no `synchronized` on the `getInstance()` check. Fix with double-checked locking or eager initialization.
- **`OrderManager.orders` is an `ArrayList` with concurrent writes possible.** Fix: use `CopyOnWriteArrayList` or `Collections.synchronizedList`.
- **`IPaymentStrategy.pay()` has no return value.** There is no way for `OrderingService.placeOrder()` to know if payment failed. Should return a `PaymentResult` or throw a checked exception on failure.
- **No price snapshot validation.** `OrderFactory.getTotalAmount()` re-computes price from `cart.cartItems` using `FoodItem.price` — if the price changed since the item was added to cart, the user is charged the new price without notice.

**Design:**
- `PaymentFactory.getPaymentStrategy()` takes a `String strategy` name — fragile (typo = `RuntimeException`). Use the existing `PaymentStrategies` enum directly.
- `User` carries a `CartManager` as a field — `CartManager` is stateless and should not be per-user. Inject it as a service dependency.
- `OrderingService.placeOrder()` is a static method — makes it untestable and breaks dependency injection. Convert to an instance method with injected dependencies.
- `OrderItem` stores `priceAtOrderTime` — excellent practice. But `CartItem` uses live `FoodItem.price` — these should also be snapshotted when added to cart.

**Minor:**
- `Cart` has no ID — cannot be referenced by ID for updates. Add a UUID field.
- `Order.status` is a `String` — should be an enum for type safety.
- `RestaurantManager.removeRestaurant()` returns an updated list but does NOT update `this.restaurants` — it's a no-op on the Singleton's state. Bug.
- `OrderManager` uses `int id` counter — not UUID; collision risk if ever distributed.

---

## Extension Points

- **Real-time order tracking:** Add a WebSocket endpoint that pushes delivery agent location updates to the user's app.
- **Scheduled orders (`SCHEDULE_FOR_LATER`):** Add a scheduler that picks up `orders WHERE order_type = 'SCHEDULE_FOR_LATER' AND scheduled_for BETWEEN NOW() AND NOW() + INTERVAL 5 MINUTE` and processes them.
- **Promotions / coupons:** Add a `CouponStrategy` to `OrderingService.placeOrder()` that applies a discount after total calculation.
- **Restaurant acceptance:** Add an intermediate `PENDING_ACCEPTANCE` status; restaurant confirms the order before it moves to `PREPARING`.
