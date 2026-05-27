# Zomato Food Ordering System - LLD Design

## Overview
This is a **Low-Level Design (LLD)** implementation of a food ordering system similar to Zomato. It demonstrates how to structure a real-world application using proper design patterns and object-oriented principles.

---

## 📖 Design Thought Process

### Step 1: Understand the Problem
**What does a user do?**
1. Search for restaurants by location
2. View restaurant menu (food items)
3. Add items to cart with quantities
4. View cart summary
5. Checkout (select payment method)
6. Payment is processed
7. Order is confirmed
8. Notification is sent
9. Cart is cleared

### Step 2: Identify Domain Models
From the user flow, we identified these **core domain models**:

```
User          - The person placing an order
Restaurant    - Has a location and menu
FoodItem      - Individual menu item with price
Cart          - Shopping cart (composition of User)
CartItem      - Item in cart (FoodItem + quantity)
Order         - Confirmed order
OrderItem     - Item in order (FoodItem + quantity + price at order time)
```

**Key insight:** Each model represents a real-world entity that needs to be stored/managed.

### Step 3: Identify Managers (Operations)
For each model, we asked: **"What operations do we perform on this model?"**

```
Restaurant    → RestaurantManager
               Operations: search by location, get menu, add/remove restaurants

Cart          → CartManager
               Operations: add item, remove item, get total, clear cart, view items

Order         → OrderManager
               Operations: save order, retrieve order, get order by user

User          → (No dedicated manager needed initially; CartManager handles user's cart)
```

**Key insight:** Managers encapsulate all operations on a single model. They prevent direct access to the model.

### Step 4: Identify Cross-Cutting Concerns
Some operations span **multiple models** or require **flexible implementations**:

```
OrderingService
  - Orchestrates: Cart → Order conversion → Payment → Notification → Saving
  - Coordinates: CartManager + OrderFactory + PaymentStrategy + NotificationStrategy + OrderManager

PaymentStrategy (Interface + Implementations)
  - Different payment methods: CreditCard, UPI, Wallet, etc.
  - Strategy pattern allows runtime selection

NotificationStrategy (Interface + Implementations)
  - Different notification channels: SMS, Email, Push
  - Strategy pattern allows runtime selection

OrderFactory
  - Complex object creation: CartItem → OrderItem (captures price at order time)
  - Factory pattern encapsulates creation logic
```

**Key insight:** Not everything fits into "model + manager". We need orchestrators, strategies, and factories for complex operations.

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    CLIENT (Main)                        │
│  (User interactions: search, add to cart, checkout)     │
└────────────────────┬────────────────────────────────────┘
                     │
     ┌───────────────┼───────────────┐
     │               │               │
     ▼               ▼               ▼
RestaurantMgr   CartManager    OrderingService (Orchestrator)
     │               │               │
  (owns)          (owns)       (orchestrates)
     │               │               │
  ┌──▼───┐      ┌───▼───┐    ┌─────▼──────────┐
  │Models│      │Models │    │ Strategies &   │
  ├──────┤      ├───────┤    │ Factories      │
  │Rest. │      │Cart   │    ├────────────────┤
  │Food  │      │CartItem    │OrderFactory    │
  │Item  │      └───────┘    │PaymentFactory  │
  └──────┘                    │NotifFactory    │
                              │PaymentStrategy │
                              │NotifStrategy   │
                              └────────────────┘
                                     │
                              ┌──────▼──────┐
                              │OrderManager │
                              │(saves order)│
                              └─────────────┘
```

---

## 🎯 Key Design Decisions & Why

### Decision 1: Manager for Each Model
**Why?**
- **Encapsulation:** Hide implementation details from client
- **Separation of Concerns:** Each manager has ONE responsibility
- **Testability:** Can test manager independently of other components
- **Maintainability:** Changes to restaurant logic stay in RestaurantManager

**Example:**
```
❌ Bad: Client directly accesses restaurant list
    restaurants.stream().filter(r -> r.location.equals("Delhi"))

✅ Good: RestaurantManager handles it
    RestaurantManager.getInstance().searchRestaurants("Delhi")
```

---

### Decision 2: Singleton Pattern for Managers
**Why?**
- **Shared State:** Only one instance of all restaurants/orders system-wide
- **Global Access:** Any part of code can access via getInstance()
- **Memory Efficient:** Don't create multiple instances

**Tradeoff:**
- Makes code harder to test (global state)
- Can be mocked in tests

---

### Decision 3: CartManager NOT Singleton
**Why?**
- Each user has their own cart
- If we made it singleton, all users would share the same cart
- CartManager is per-user instance

**Pattern:** Composition (User owns CartManager)
```java
User {
    CartManager cartManager;  // Per-user instance
}
```

---

### Decision 4: OrderingService Orchestrates Multiple Managers
**Why?**
- Checkout is a **complex workflow** spanning multiple concerns:
  - Get user's cart (CartManager)
  - Create order from cart (OrderFactory)
  - Process payment (PaymentStrategy)
  - Send notification (NotificationStrategy)
  - Save order (OrderManager)
  - Clear cart (CartManager)

- **Single Responsibility Principle:** No single manager can handle all this
- **Orchestration:** OrderingService coordinates all pieces

---

### Decision 5: Strategy Pattern for Payment & Notification
**Why?**
- Different payment methods exist (CreditCard, UPI, Wallet, etc.)
- Different notification channels exist (SMS, Email, Push)
- At runtime, user selects which one
- **Strategy pattern** allows flexible selection without changing code

```java
// At runtime, based on user choice
IPaymentStrategy strategy = PaymentFactory.getPaymentStrategy("CREDIT_CARD");
strategy.pay(amount);

// vs

IPaymentStrategy strategy = PaymentFactory.getPaymentStrategy("UPI");
strategy.pay(amount);
```

---

### Decision 6: Factory Pattern for Order Creation
**Why?**
- Converting CartItem → OrderItem is **complex logic**
  - Must calculate total amount
  - Must capture price at order time (for records)
  - Must handle OrderNow vs ScheduleForLater
  
- **Factory pattern** encapsulates this complexity

```java
Order order = OrderFactory.createOrder(user, cart, orderType);
// Factory handles: conversion, calculation, initialization
```

---

### Decision 7: OrderItem Captures Price at Order Time
**Why?**
- If restaurant changes price later, order record should show price paid at purchase
- `CartItem` has current price
- `OrderItem` has frozen price at order time
- **Immutability:** OrderItem is immutable (reflects what customer actually paid)

```java
CartItem {
    FoodItem foodItem;      // Current price
    int quantity;
}

OrderItem {
    FoodItem foodItem;      // Reference to menu item
    int quantity;
    double priceAtOrderTime; // ← Frozen price when order was placed
}
```

---

## 📊 Data Flow

```
1. USER SEARCHES RESTAURANTS
   Client → RestaurantManager.searchRestaurants("Delhi")
   → Returns List<Restaurant>
   → Display to user

2. USER VIEWS MENU
   Client → RestaurantManager.getMenuItemsForARestaurant(restaurantId)
   → Returns List<FoodItem>
   → Display to user

3. USER ADDS TO CART
   Client → CartManager.addFoodItemToCart(cart, foodItem, qty)
   → CartManager checks if item exists
   → If exists: increment qty
   → If new: add CartItem
   → Display: "Added 2x Burger to cart"

4. USER VIEWS CART
   Client → CartManager.getCartItems(cart)
   Client → CartManager.getTotal(cart)
   → Display cart with total

5. USER CHECKOUT
   Client → OrderingService.placeOrder(user, cart, orderType, paymentType, notificationType)
   
   Inside OrderingService:
   a. OrderFactory.createOrder(user, cart, orderType)
      → Converts CartItems → OrderItems
      → Calculates total
      → Returns Order
   
   b. PaymentFactory.getPaymentStrategy(paymentType)
      → Returns IPaymentStrategy implementation
      → strategy.pay(amount)
   
   c. OrderManager.saveOrder(order)
      → Assigns order ID
      → Stores in list
   
   d. NotificationFactory.getNotificationStrategy(type)
      → Returns INotificationStrategy implementation
      → strategy.notify(user, order)
   
   e. CartManager.clearCart(cart)
      → Empties cart for next order

6. SUCCESS
   Return Order object to client
```

---

## 🧩 Class Responsibilities

| Class | Responsibility | Why Exists |
|-------|---|---|
| `RestaurantManager` | Search restaurants, get menus | Centralize restaurant data operations |
| `CartManager` | Manage user's shopping cart | Encapsulate cart logic (add/remove/total) |
| `OrderManager` | Store and retrieve orders | Persist orders to "database" |
| `OrderingService` | Orchestrate checkout flow | Coordinate multiple pieces into one workflow |
| `OrderFactory` | Convert Cart → Order | Encapsulate complex order creation logic |
| `PaymentFactory` | Create payment strategy | Support multiple payment methods |
| `NotificationFactory` | Create notification strategy | Support multiple notification channels |
| `IPaymentStrategy` | Different payment implementations | Allow runtime selection of payment method |
| `INotificationStrategy` | Different notification implementations | Allow runtime selection of notification channel |

---

## 🔄 How to Extend This System

### Add a new restaurant search criteria (e.g., by cuisine)?
→ Add method to `RestaurantManager`

### Add a new payment method (e.g., Google Pay)?
→ Create new class implementing `IPaymentStrategy`
→ Register in `PaymentFactory`

### Add a new notification channel (e.g., Push Notification)?
→ Create new class implementing `INotificationStrategy`
→ Register in `NotificationFactory`

### Add order status tracking?
→ Add status enum to `Order`
→ Add method to `OrderManager` to update status
→ Trigger notifications on status change

### Add user authentication?
→ Create `UserManager`
→ Add login/register operations
→ Verify user before allowing operations

---

## 💡 Key Takeaways for Interview

1. **Start with user flow** - understand what user does
2. **Identify models** - what entities exist?
3. **Identify managers** - one manager per model
4. **Identify orchestrators** - for workflows spanning multiple models
5. **Use patterns** - Factory, Strategy, Singleton where appropriate
6. **Encapsulate** - hide implementation details behind interfaces/managers
7. **High cohesion** - each class has ONE clear responsibility
8. **Low coupling** - classes don't tightly depend on each other

---

## 🧪 Running the System

```bash
java -cp target/classes com.lld.realworldexamples.zomato.Client
```

**Output shows:** Complete flow from search → add to cart → checkout → payment → notification

