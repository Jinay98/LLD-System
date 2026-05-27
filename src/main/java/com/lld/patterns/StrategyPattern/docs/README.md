# Strategy Pattern - Pluggable Algorithms

## Overview
The **Strategy Pattern** defines a family of algorithms, encapsulates each, and makes them interchangeable. The strategy pattern lets the algorithm vary independently from clients that use it.

---

## 🎯 Problem This Pattern Solves

### Without Strategy (Brittle If-Else)
```java
class PaymentProcessor {
    void processPayment(String type, double amount) {
        if (type.equals("CREDIT_CARD")) {
            // Credit card logic
            validateCreditCard();
            chargeCard();
            sendReceipt();
        } else if (type.equals("UPI")) {
            // UPI logic
            validateUPI();
            chargeUPI();
            sendOTP();
        } else if (type.equals("WALLET")) {
            // Wallet logic
            validateWallet();
            chargeWallet();
            updateBalance();
        }
        // Add PayPal? Modify this class!
        // Hard to test each path!
        // Methods are tangled together!
    }
}
```

**Problems:**
- Growing if-else chains
- Each addition modifies existing code
- Hard to test individual strategies
- Methods are mixed together

### With Strategy (Clean & Extensible)
```java
interface PaymentStrategy {
    void pay(double amount);
}

class CreditCardStrategy implements PaymentStrategy {
    public void pay(double amount) {
        validateCreditCard();
        chargeCard();
        sendReceipt();
    }
}

class UPIStrategy implements PaymentStrategy {
    public void pay(double amount) {
        validateUPI();
        chargeUPI();
        sendOTP();
    }
}

class PaymentProcessor {
    private PaymentStrategy strategy;
    
    public void processPayment(double amount) {
        strategy.pay(amount);  // Delegate to strategy
    }
    
    public void setStrategy(PaymentStrategy strategy) {
        this.strategy = strategy;  // Swap strategy at runtime
    }
}

// Usage
processor.setStrategy(new CreditCardStrategy());
processor.processPayment(100);

processor.setStrategy(new UPIStrategy());
processor.processPayment(100);

// Add PayPal? Just create new class, no modification!
```

---

## 🏗️ Strategy Pattern Structure

```
┌──────────────────────┐
│  PaymentStrategy     │ (Interface)
│ + pay(amount)        │
└──────────────────────┘
         △ △ △
         │ │ │
    ┌────┘ │ └────┐
    │      │      │
    ▼      ▼      ▼
┌──────────┐ ┌────────┐ ┌──────────┐
│CreditCard│ │  UPI   │ │ Wallet   │
│Strategy  │ │Strategy│ │Strategy  │
├──────────┤ ├────────┤ ├──────────┤
│pay()     │ │pay()   │ │pay()     │
└──────────┘ └────────┘ └──────────┘

┌─────────────────────────┐
│ PaymentProcessor        │ (Context)
├─────────────────────────┤
│ - strategy: Strategy    │
│ + setStrategy()         │
│ + processPayment()      │
│   └─ calls strategy.pay()
└─────────────────────────┘
```

---

## 🎯 When to Use Strategy Pattern

✅ **Use When:**
- Multiple algorithms for same task
- Algorithm selection at runtime
- Want to avoid if-else chains
- Algorithms are likely to change/grow
- Want algorithms to be tested independently

❌ **Don't Use When:**
- Only one algorithm exists
- Algorithm fixed at design time
- Algorithms are simple

---

## 🔄 Strategy vs Other Patterns

| Pattern | Purpose | How |
|---------|---------|-----|
| **Strategy** | Choose algorithm | Interface + different implementations |
| **Factory** | Create object | Factory method returns instance |
| **Decorator** | Add behavior | Wraps object, adds features |
| **State** | Change behavior based on state | Object behaves differently based on internal state |

---

## 💡 Strategy Pattern Benefits

| Benefit | Why |
|---------|-----|
| **Encapsulation** | Each algorithm isolated |
| **Interchangeability** | Swap at runtime |
| **Open/Closed** | Open for extension, closed for modification |
| **Testability** | Test each strategy independently |
| **Single Responsibility** | Each class handles one algorithm |
| **Composition** | Prefer composition over inheritance |

---

## Real-World Examples

### Sorting Strategy
```java
interface SortStrategy {
    void sort(int[] arr);
}

class BubbleSortStrategy implements SortStrategy {
    public void sort(int[] arr) { /* bubble sort */ }
}

class QuickSortStrategy implements SortStrategy {
    public void sort(int[] arr) { /* quick sort */ }
}

class Sorter {
    private SortStrategy strategy;
    public void sort(int[] arr) { strategy.sort(arr); }
}

// Usage
sorter.setStrategy(new QuickSortStrategy());  // For large arrays
sorter.sort(largeArray);

sorter.setStrategy(new BubbleSortStrategy()); // For small arrays
sorter.sort(smallArray);
```

### Compression Strategy
```java
interface CompressionStrategy {
    byte[] compress(byte[] data);
}

class GZipStrategy implements CompressionStrategy {
    public byte[] compress(byte[] data) { /* gzip */ }
}

class ZipStrategy implements CompressionStrategy {
    public byte[] compress(byte[] data) { /* zip */ }
}

class FileCompressor {
    private CompressionStrategy strategy;
    public void compress(File file) {
        byte[] data = readFile(file);
        byte[] compressed = strategy.compress(data);
    }
}
```

### Notification Strategy (Zomato Example)
```java
interface NotificationStrategy {
    void notify(User user, Order order);
}

class SMSStrategy implements NotificationStrategy {
    public void notify(User user, Order order) {
        // Send SMS
    }
}

class EmailStrategy implements NotificationStrategy {
    public void notify(User user, Order order) {
        // Send email
    }
}

// Usage (from Zomato example!)
INotificationStrategy strategy = NotificationFactory.getNotificationStrategy("SMS");
strategy.notify(user, order);
```

---

## 🧪 Testing with Strategy

```java
public class PaymentTest {
    @Test
    public void testCreditCardPayment() {
        // Easy to test individual strategy
        PaymentStrategy strategy = new MockCreditCardStrategy();
        processor.setStrategy(strategy);
        processor.processPayment(100);
        // Verify it worked
    }
    
    @Test
    public void testUPIPayment() {
        // Easy to test different strategy
        PaymentStrategy strategy = new MockUPIStrategy();
        processor.setStrategy(strategy);
        processor.processPayment(100);
        // Verify it worked
    }
}
```

---

## 💡 Key Takeaways

1. **Interchangeable:** Swap algorithms at runtime
2. **No If-Else:** Each algorithm in separate class
3. **Easy to Add:** New algorithm = new class + register
4. **Testable:** Each strategy tested independently
5. **Single Responsibility:** One class = one algorithm
6. **Extensible:** Add new strategies without modifying existing code

---

## Common Mistakes

### ❌ Mistake 1: Strategy in Context Class
```java
// WRONG: Logic mixed in context
class Processor {
    void pay(String type, double amount) {
        if (type.equals("CC")) { /* logic */ }
        if (type.equals("UPI")) { /* logic */ }
    }
}

// RIGHT: Strategy handles logic
class Processor {
    void pay(double amount) {
        strategy.pay(amount);  // Delegate
    }
}
```

### ❌ Mistake 2: Forcing Strategy When Not Needed
```java
// WRONG: Overkill for one algorithm
interface SortStrategy {
    void sort(int[] arr);
}
class OnlyQuickSort implements SortStrategy { }

// RIGHT: Just use simple method if one algorithm
class Sorter {
    void sort(int[] arr) { /* quick sort */ }
}
```

---

## Interview Question Answers

**Q: When do you use Strategy vs Factory?**
A: Factory creates objects. Strategy selects algorithm. They're complementary - Factory might create a Strategy instance based on user choice.

**Q: Can Strategy pattern be combined with others?**
A: Yes! Factory creates Strategies, Decorator wraps Strategies, Builder configures Strategies.

**Q: What's the difference between Strategy and State?**
A: Strategy is externally selected. State changes internally based on object state. Strategy doesn't change object's state; State changes it.

