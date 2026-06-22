# Design Patterns Used in Zomato LLD

This document explains each design pattern used in this system and why it was chosen.

---

## 1. Singleton Pattern

### Where Used?
- `RestaurantManager`
- `OrderManager`

### Pattern Structure
```java
public class RestaurantManager {
    private static RestaurantManager instance;
    
    private RestaurantManager() {}  // Private constructor
    
    public static RestaurantManager getInstance() {
        if (instance == null) {
            instance = new RestaurantManager();
        }
        return instance;
    }
}
```

### Why This Pattern?

| Aspect | Reason |
|--------|--------|
| **Single Instance** | Only one list of all restaurants in the system |
| **Global Access** | Any part of code can access via `getInstance()` |
| **Memory Efficient** | Don't create multiple copies of restaurant list |
| **Data Consistency** | All parts of system see the same restaurant data |

### When to Use Singleton
✅ Global state that should be shared (database connection, restaurant list, order history)
❌ Per-user state (user's cart should NOT be singleton)

### Real-World Example
```
RestaurantManager - ONE instance for the entire app
CartManager - Per-user instance (NOT singleton)
```

---

## 2. Manager/Service Pattern

### Where Used?
- `RestaurantManager` - manages restaurants
- `CartManager` - manages cart
- `OrderManager` - manages orders

### Pattern Structure
```java
public class RestaurantManager {
    private List<Restaurant> restaurants;  // Owns the data
    
    public void addRestaurant(Restaurant r) { /* ... */ }
    public void searchRestaurants(String location) { /* ... */ }
    public void getMenuItemsForARestaurant(int id, String name) { /* ... */ }
}
```

### Why This Pattern?

| Aspect | Reason |
|--------|--------|
| **Encapsulation** | Hide implementation details (how search works) |
| **Single Responsibility** | RestaurantManager only deals with restaurants |
| **Testability** | Can mock RestaurantManager in tests |
| **Maintainability** | Changes to restaurant logic stay localized |

### Decision Framework: Should I Create a Manager?

```
Question 1: Do I have a domain model (entity)?
  YES → Create a Manager for it

Question 2: Does the manager have ONE clear responsibility?
  YES → Good manager

Question 3: All methods operate on the same data?
  YES → Good cohesion

If YES to all three → Create the manager
```

---

## 3. Factory Pattern

### Where Used?
- `OrderFactory` - creates Order from Cart
- `PaymentFactory` - creates PaymentStrategy
- `NotificationFactory` - creates NotificationStrategy

### Pattern Structure
```java
public class OrderFactory {
    public static Order createOrder(User user, Cart cart, OrderTypes orderType) {
        // Complex creation logic
        List<OrderItem> orderItems = convertCartItemsToOrderItems(cart);
        double total = calculateTotal(cart);
        
        Order order = new Order();
        order.setUser(user);
        order.setOrderItems(orderItems);
        order.setTotalAmt(total);
        order.setStatus("PENDING");
        order.setOrderedAt(Instant.now());
        
        return order;
    }
}
```

### Why This Pattern?

| Aspect | Reason |
|--------|--------|
| **Encapsulation** | Hide complex creation logic |
| **Single Responsibility** | Creation logic stays in factory |
| **Flexibility** | Can create different Order types (OrderNow vs ScheduleForLater) |
| **Reusability** | Other classes can use factory without duplicating logic |

### When to Use Factory
✅ Complex object creation (multiple steps, multiple dependencies)
✅ Multiple types to create based on parameters
✅ Creation logic might change in future
❌ Simple object creation (just `new Object()`)

---

## 4. Strategy Pattern

### Where Used?
- `IPaymentStrategy` with implementations:
  - `CreditCardPaymentStrategy`
  - `UPIPaymentStrategy`
- `INotificationStrategy` with implementations:
  - `SMSNotificationStrategy`
  - `EmailNotificationStrategy`

### Pattern Structure
```java
// Interface (contract)
public interface IPaymentStrategy {
    void pay(double amount);
}

// Different implementations
public class CreditCardPaymentStrategy implements IPaymentStrategy {
    public void pay(double amount) {
        System.out.println("Paid " + amount + " via credit card");
    }
}

public class UPIPaymentStrategy implements IPaymentStrategy {
    public void pay(double amount) {
        System.out.println("Paid " + amount + " via UPI");
    }
}

// Runtime selection via factory
IPaymentStrategy strategy = PaymentFactory.getPaymentStrategy("CREDIT_CARD");
strategy.pay(290.0);
```

### Why This Pattern?

| Aspect | Reason |
|--------|--------|
| **Flexibility** | Change payment method at runtime without changing code |
| **Open/Closed** | Open for extension (add new strategy), closed for modification |
| **Decoupling** | OrderingService doesn't know about specific payment methods |
| **Testability** | Easy to mock/test different strategies |

### When to Use Strategy
✅ Multiple algorithms/implementations for same interface
✅ Selection happens at runtime based on user input
✅ Want to avoid if-else chains
❌ Only one implementation exists

### Without Strategy (Bad)
```java
if (paymentType.equals("CREDIT_CARD")) {
    // Payment logic for credit card
} else if (paymentType.equals("UPI")) {
    // Payment logic for UPI
} else if (paymentType.equals("WALLET")) {
    // Payment logic for wallet
}
// Every time you add payment method, modify this code
```

### With Strategy (Good)
```java
IPaymentStrategy strategy = PaymentFactory.getPaymentStrategy(paymentType);
strategy.pay(amount);
// Add new payment method? Just create new class + register in factory
```

---

## 5. Orchestrator Pattern

### Where Used?
- `OrderingService` - orchestrates the checkout flow

### Pattern Structure
```java
public class OrderingService {
    public static Order placeOrder(User user, Cart cart, OrderTypes orderType,
                                   String paymentStrategyType, String cardDetails,
                                   String notificationStrategyType) {
        
        // Step 1: Create order
        Order order = OrderFactory.createOrder(user, cart, orderType);
        
        // Step 2: Process payment
        IPaymentStrategy paymentStrategy = PaymentFactory.getPaymentStrategy(...);
        paymentStrategy.pay(order.getTotalAmt());
        
        // Step 3: Save order
        OrderManager.getInstance().saveOrder(order);
        
        // Step 4: Send notification
        INotificationStrategy notificationStrategy = NotificationFactory.getNotificationStrategy(...);
        notificationStrategy.notify(user, order);
        
        // Step 5: Clear cart
        user.getCartManager().clearCart(cart);
        
        return order;
    }
}
```

### Why This Pattern?

| Aspect | Reason |
|--------|--------|
| **Coordination** | Orchestrates multiple independent components |
| **Single Responsibility** | OrderingService just coordinates; others do the work |
| **Visibility** | Complete flow is visible in one place |
| **Testability** | Easy to test different scenarios by mocking strategies |

### When to Use Orchestrator
✅ Complex workflows spanning multiple components
✅ Need to coordinate independent pieces
✅ Flow order matters
❌ Simple single-step operations

---

## 6. Composition Pattern

### Where Used?
- User has CartManager
- Cart has CartItems
- Order has OrderItems

### Pattern Structure
```java
public class User {
    private CartManager cartManager;  // Composition (strong ownership)
}

public class Cart {
    private List<CartItem> cartItems;  // Composition
}
```

### Why This Pattern?

| Aspect | Reason |
|--------|--------|
| **Ownership** | User owns its cart (can't exist without user) |
| **Tight Coupling** | CartManager is part of User |
| **Lifecycle** | Cart dies when user dies |

### Composition vs Association
```
Composition (strong): User HAS-A CartManager (can't exist independently)
Association (weak): Order HAS-A Restaurant (restaurant can exist without order)

Use Composition when: Object X can't exist without Y
Use Association when: Object X can exist independently of Y
```

---

## Pattern Decision Tree

```
Need to create complex objects?
  └─ YES → Use Factory
        Example: Order creation (many steps)

Need multiple implementations of same interface?
  └─ YES → Use Strategy
        Example: PaymentStrategy, NotificationStrategy

Need global single instance?
  └─ YES → Use Singleton
        Example: RestaurantManager, OrderManager

Need to manage operations on a domain model?
  └─ YES → Use Manager
        Example: CartManager, RestaurantManager

Need to coordinate multiple components?
  └─ YES → Use Orchestrator
        Example: OrderingService

Need "A owns B" relationship?
  └─ YES → Use Composition
        Example: User owns CartManager
```

---

## 🎯 Key Pattern Combinations in Zomato

### Search Restaurant Flow
```
RestaurantManager (Manager pattern)
  ↓
searchRestaurants(location)
```

### Add to Cart Flow
```
CartManager (Manager pattern)
  ↓
addFoodItemToCart() - checks for existing item
  ↓
CartItem (Composition pattern)
```

### Checkout Flow
```
OrderingService (Orchestrator pattern)
  ├─ OrderFactory (Factory pattern)
  │   └─ Creates Order from Cart
  ├─ PaymentFactory (Factory pattern)
  │   ├─ Creates CreditCardPaymentStrategy (Strategy pattern)
  │   └─ Creates UPIPaymentStrategy (Strategy pattern)
  ├─ OrderManager (Manager pattern)
  │   └─ Saves order
  ├─ NotificationFactory (Factory pattern)
  │   ├─ Creates SMSNotificationStrategy (Strategy pattern)
  │   └─ Creates EmailNotificationStrategy (Strategy pattern)
  └─ CartManager (Manager pattern)
      └─ Clears cart
```

---

## 📚 Further Reading

- **Singleton:** Used for global state, but can make testing hard. Alternatives: Dependency Injection
- **Factory:** Part of Creational patterns. Others: Builder (complex objects with many params)
- **Strategy:** Part of Behavioral patterns. Others: State (different behaviors based on state)
- **Composition:** Part of Structural patterns. Others: Aggregation (weaker relationship)

