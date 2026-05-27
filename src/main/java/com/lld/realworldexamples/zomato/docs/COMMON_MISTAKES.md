# Common Mistakes in LLD Design (What NOT to Do)

This document lists common mistakes made when designing systems like Zomato. Learn from these mistakes!

---

## ❌ Mistake 1: God Object (Too Much Responsibility)

### The Mistake
```java
// BAD: One class does EVERYTHING
public class Zomato {
    // Restaurant operations
    public void searchRestaurants() { }
    public void getMenu() { }
    
    // Cart operations
    public void addToCart() { }
    public void removeFromCart() { }
    
    // Order operations
    public void checkout() { }
    public void pay() { }
    public void sendNotification() { }
}

// This violates Single Responsibility Principle
```

### Why It's Wrong
- **Hard to test** - Can't test restaurant search without payment logic
- **Hard to maintain** - One change breaks everything
- **Hard to extend** - Adding new feature means modifying this class
- **Poor cohesion** - Methods don't relate to each other

### The Right Way
```java
// GOOD: Each class has ONE responsibility
RestaurantManager {
    searchRestaurants() { }
    getMenu() { }
}

CartManager {
    addToCart() { }
    removeFromCart() { }
}

OrderingService {
    checkout() { }  // Orchestrates other services
}
```

---

## ❌ Mistake 2: Everything is Singleton

### The Mistake
```java
// BAD: Making CartManager singleton
public class CartManager {
    private static CartManager instance;
    private static List<CartItem> allUsersCarts;  // ALL USERS SHARE SAME CART!
    
    public static CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }
}

// User 1 adds Burger → all users see Burger in their cart!
```

### Why It's Wrong
- **Shared state problem** - User A's cart gets mixed with User B's cart
- **Data corruption** - One user's actions affect another
- **Not scalable** - Can't handle multiple users

### The Right Way
```java
// GOOD: CartManager is per-user
public class User {
    private CartManager cartManager;  // Each user has their own
    
    public User(String name, String location, CartManager cartManager) {
        this.cartManager = cartManager;  // Injected per-user
    }
}

// User 1 has cartManager1 → User 1's items only
// User 2 has cartManager2 → User 2's items only
```

### Rule of Thumb
```
Singleton: YES
  ├─ Database connection
  ├─ Restaurant list (shared data)
  ├─ Order history (shared data)
  └─ Configuration

Singleton: NO
  ├─ User's cart (per-user)
  ├─ Current session (per-user)
  └─ User's preferences (per-user)
```

---

## ❌ Mistake 3: Tight Coupling (Too Many Dependencies)

### The Mistake
```java
// BAD: Order depends on everything
public class Order {
    private PaymentService paymentService;
    private NotificationService notificationService;
    private RestaurantManager restaurantManager;
    private CartManager cartManager;
    
    public void placeOrder() {
        paymentService.pay();  // Order shouldn't know about payment
        notificationService.notify();  // Order shouldn't know about notifications
        // ...
    }
}

// Order is tightly coupled to all these services
// Hard to test, hard to change
```

### Why It's Wrong
- **Hard to test** - Can't test Order without all dependencies
- **Inflexible** - Order is hardcoded to use specific payment method
- **Violates SRP** - Order shouldn't coordinate payment
- **Hard to mock** - Testing requires mocking everything

### The Right Way
```java
// GOOD: Orchestrator handles coordination
public class OrderingService {
    public Order placeOrder(User user, Cart cart, OrderTypes orderType,
                           String paymentType, String notificationType) {
        // Step 1: Order knows only about itself
        Order order = OrderFactory.createOrder(user, cart, orderType);
        
        // Step 2: OrderingService orchestrates
        IPaymentStrategy payment = PaymentFactory.getPaymentStrategy(paymentType);
        payment.pay(order.getTotalAmt());
        
        // Step 3: Notification is separate
        INotificationStrategy notification = NotificationFactory.getNotificationStrategy(notificationType);
        notification.notify(user, order);
    }
}

// Order is simple, OrderingService coordinates
// Easy to test, easy to change
```

---

## ❌ Mistake 4: No Interfaces (Hard to Extend)

### The Mistake
```java
// BAD: Direct implementation, hard to change
public class CreditCardPaymentService {
    public void pay(double amount) {
        // Credit card payment logic
    }
}

public class OrderingService {
    public void checkout(Order order) {
        CreditCardPaymentService payment = new CreditCardPaymentService();
        payment.pay(order.getTotalAmt());
        // If we want UPI, we have to change this code!
    }
}
```

### Why It's Wrong
- **Hard to extend** - Adding UPI means modifying OrderingService
- **Violates Open/Closed** - Closed for modification, open for extension
- **Hard to test** - Can't mock payment service
- **Tight coupling** - OrderingService knows about CreditCard

### The Right Way
```java
// GOOD: Use interface for flexibility
public interface IPaymentStrategy {
    void pay(double amount);
}

public class CreditCardPaymentStrategy implements IPaymentStrategy {
    public void pay(double amount) { /* ... */ }
}

public class UPIPaymentStrategy implements IPaymentStrategy {
    public void pay(double amount) { /* ... */ }
}

public class OrderingService {
    public void checkout(Order order, String paymentType) {
        IPaymentStrategy payment = PaymentFactory.getPaymentStrategy(paymentType);
        payment.pay(order.getTotalAmt());
        // Add Google Pay? Just create new class + register in factory
        // No modification needed to OrderingService!
    }
}
```

---

## ❌ Mistake 5: Mixing Data Access with Business Logic

### The Mistake
```java
// BAD: RestaurantManager mixes search logic with data access
public class RestaurantManager {
    private List<Restaurant> restaurants;
    
    public void searchRestaurants(String location) {
        // This mixes database logic with search logic
        for (Restaurant r : restaurants) {
            if (location.equalsIgnoreCase(r.getLocation())) {
                System.out.println(r.getId() + "--" + r.getName());
            }
        }
    }
}

// In real system: Add pagination, filtering, sorting → RestaurantManager gets bloated
```

### Why It's Wrong
- **Hard to test** - Can't test search without database
- **Hard to change** - Database switch requires code change
- **Not scalable** - Filtering logic gets complex fast

### The Right Way
```java
// GOOD: Separate concerns
// Layer 1: Data Access
public class RestaurantRepository {
    public List<Restaurant> getAll() { /* access database */ }
    public Restaurant getById(int id) { /* access database */ }
}

// Layer 2: Business Logic
public class RestaurantManager {
    private RestaurantRepository repository;
    
    public List<Restaurant> searchRestaurants(String location) {
        List<Restaurant> all = repository.getAll();
        return all.stream()
            .filter(r -> r.getLocation().equalsIgnoreCase(location))
            .toList();
    }
}

// In real system: Switch database? Change only RestaurantRepository
// Business logic stays the same
```

---

## ❌ Mistake 6: No Clear Data Flow

### The Mistake
```java
// BAD: Unclear who owns the data, who modifies it
public class CartManager {
    public void addItem(FoodItem item) { /* ... */ }
}

public class OrderManager {
    public void processOrder(FoodItem item) { /* modifies item price? */ }
}

public class Order {
    public void addItem(FoodItem item) { /* modifies item? */ }
}

// Who owns FoodItem? Who can modify it? Unclear!
```

### Why It's Wrong
- **Data corruption** - Multiple objects modifying same data
- **Hard to debug** - Where did the price change come from?
- **Race conditions** - Two threads modifying same data
- **Confusion** - Unclear who's responsible

### The Right Way
```java
// GOOD: Clear ownership
CartItem {
    FoodItem foodItem;  // Reference only (can change)
    int quantity;
    getSubtotal() { return foodItem.price * quantity; }
}

OrderItem {
    FoodItem foodItem;  // Reference only
    int quantity;
    double priceAtOrderTime;  // IMMUTABLE - captured at order time
    getSubtotal() { return priceAtOrderTime * quantity; }
}

// Clear: CartItem references current price
// Clear: OrderItem freezes price at order time
// No ambiguity about who owns what
```

---

## ❌ Mistake 7: Ignoring Edge Cases

### The Mistake
```java
// BAD: Happy path only, no edge cases
public class CartManager {
    public void addFoodItemToCart(Cart cart, FoodItem foodItem, int qty) {
        CartItem cartItem = new CartItem(foodItem, qty);
        cart.getCartItems().add(cartItem);  // Always adds, even if item exists!
    }
}

// If user adds Burger twice, they get TWO CartItems instead of qty=2
```

### Why It's Wrong
- **Bugs** - Duplicate items in cart
- **Data inconsistency** - Cart state is wrong
- **Poor UX** - User sees Burger twice instead of qty 2

### The Right Way
```java
// GOOD: Handle edge cases
public void addFoodItemToCart(Cart cart, FoodItem foodItem, int qty) {
    // Check if item already exists
    for (CartItem cartItem : cart.getCartItems()) {
        if (cartItem.getFoodItem().getCode().equalsIgnoreCase(foodItem.getCode())) {
            // Update quantity instead of adding duplicate
            cartItem.setQty(cartItem.getQty() + qty);
            return;
        }
    }
    // Item doesn't exist, add new one
    CartItem newItem = new CartItem(foodItem, qty);
    cart.getCartItems().add(newItem);
}
```

---

## ❌ Mistake 8: Unclear Class Responsibilities

### The Mistake
```java
// BAD: Unclear what these do
public class RestaurantService { }
public class RestaurantManager { }
public class RestaurantHandler { }
public class RestaurantProcessor { }

// What's the difference? Confusion!
```

### Why It's Wrong
- **Naming confusion** - Developer doesn't know which to use
- **Duplicate code** - Developers create their own "handlers"
- **Maintenance nightmare** - Multiple implementations of same logic

### The Right Way
```java
// GOOD: Clear naming conventions

// Manager: Manages operations on a model
public class RestaurantManager {
    List<Restaurant> restaurants;
    searchRestaurants() { }
}

// Service/Orchestrator: Coordinates multiple managers
public class OrderingService {
    OrderFactory, PaymentStrategy, NotificationStrategy
    placeOrder() { }
}

// Factory: Creates complex objects
public class OrderFactory {
    createOrder() { }
}

// Strategy: Different implementations of same interface
public interface IPaymentStrategy { pay(); }
class CreditCardPaymentStrategy implements IPaymentStrategy { }
```

---

## ✅ Checklist: Before You Submit Your Design

```
[ ] Does each class have ONE clear responsibility?
[ ] Is data ownership clear (who owns what)?
[ ] Are there any god objects (too much responsibility)?
[ ] Are singletons used correctly (shared data only)?
[ ] Are there unnecessary dependencies?
[ ] Can each piece be tested independently?
[ ] Is the data flow clear and traceable?
[ ] Are edge cases handled?
[ ] Are naming conventions clear?
[ ] Is the design extensible (easy to add features)?
```

If you can't check all boxes, refactor before submission!

---

## 🎯 Quick Self-Review Questions

When designing a system, ask yourself:

1. **"Can I describe each class in ONE sentence?"**
   - If not → Class has too much responsibility

2. **"Can I test this class independently?"**
   - If not → Too many dependencies

3. **"If I add a new feature, how many files do I change?"**
   - If many → Poor encapsulation

4. **"Can I swap one implementation for another?"**
   - If not → Consider using interfaces

5. **"Who owns this data?"**
   - If unclear → Redesign ownership

