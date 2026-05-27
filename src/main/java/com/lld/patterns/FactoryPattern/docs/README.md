# Factory Pattern - Detailed Study

## Overview
The **Factory Pattern** demonstrates how to create objects without specifying their exact classes. Instead of `new ConcreteClass()`, use a factory method to create instances.

---

## 🎯 Problem This Pattern Solves

### Without Factory (Tightly Coupled)
```java
// Main class needs to know about all types
if (type.equals("CAR")) {
    vehicle = new Car();
} else if (type.equals("BIKE")) {
    vehicle = new Bike();
} else if (type.equals("TRUCK")) {
    vehicle = new Truck();
}

// Add new vehicle? Modify main!
// Test? Hard to mock!
```

### With Factory (Decoupled)
```java
// Main class only knows about factory
Vehicle vehicle = VehicleFactory.create(type);

// Add new vehicle? Add implementation + register in factory
// Main class never changes!
// Test? Easy to mock factory!
```

---

## 🏗️ Factory Pattern Structure

```
┌─────────────────────────────────┐
│    VehicleFactory (Factory)     │
│ ┌─────────────────────────────┐ │
│ │ + create(type): Vehicle     │ │
│ │ - if-else logic here        │ │
│ │ - returns concrete instance │ │
│ └─────────────────────────────┘ │
└──────────────┬──────────────────┘
               │
      ┌────────┴────────┐
      │                 │
      ▼                 ▼
   Creates          Creates
┌───────────┐    ┌────────────┐
│   Car     │    │   Truck    │
└───────────┘    └────────────┘

Client ──requests──> Factory ──creates──> ConcreteType
       (doesn't know type)          (client doesn't care)
```

---

## 💡 When to Use Factory Pattern

✅ **Use Factory When:**
- Multiple types to create based on runtime input
- Creation logic is complex
- Object type determined by user/config
- Want to decouple creation from usage

❌ **Don't Use Factory When:**
- Only one type exists
- Simple object creation (`new Object()`)
- Creation is straightforward

---

## 🔄 Types of Factory Patterns

### 1. Simple Factory (Static Factory Method)
```java
public class PaymentFactory {
    public static IPaymentStrategy create(String type) {
        switch(type) {
            case "CREDIT_CARD":
                return new CreditCardPayment();
            case "UPI":
                return new UPIPayment();
            default:
                throw new IllegalArgumentException("Unknown type");
        }
    }
}

// Usage
IPaymentStrategy payment = PaymentFactory.create("UPI");
payment.pay(100);
```

### 2. Factory Method Pattern
```java
abstract class DocumentCreator {
    abstract Document createDocument();
    
    public void processDocument() {
        Document doc = createDocument();  // Subclass decides type
        doc.open();
    }
}

class PDFCreator extends DocumentCreator {
    @Override
    Document createDocument() {
        return new PDFDocument();
    }
}

class WordCreator extends DocumentCreator {
    @Override
    Document createDocument() {
        return new WordDocument();
    }
}
```

### 3. Abstract Factory Pattern
```java
interface UIFactory {
    Button createButton();
    Checkbox createCheckbox();
}

class WindowsUIFactory implements UIFactory {
    @Override
    Button createButton() { return new WindowsButton(); }
    @Override
    Checkbox createCheckbox() { return new WindowsCheckbox(); }
}

class MacUIFactory implements UIFactory {
    @Override
    Button createButton() { return new MacButton(); }
    @Override
    Checkbox createCheckbox() { return new MacCheckbox(); }
}

// Usage
UIFactory factory = OSDetector.isMac() ? new MacUIFactory() : new WindowsUIFactory();
Button button = factory.createButton();
Checkbox checkbox = factory.createCheckbox();
```

---

## 📊 Factory Pattern Benefits

| Benefit | Why |
|---------|-----|
| **Loose Coupling** | Client doesn't know concrete types |
| **Easy to Extend** | Add new type without changing client |
| **Centralized Creation** | All creation logic in one place |
| **Testability** | Can mock factory in tests |
| **Configuration-Based** | Can load types from config |

---

## 🎓 Factory vs Other Patterns

| Pattern | Purpose | When |
|---------|---------|------|
| **Factory** | *Create* objects | Multiple types, runtime selection |
| **Builder** | *Configure* complex objects | Many optional parameters |
| **Singleton** | *Restrict* to one instance | Global state needed |
| **Strategy** | *Select* algorithm | Behavior variants |

---

## 💡 Key Takeaways

1. **Decouple Creation:** Client doesn't know concrete types
2. **Centralize Logic:** All "which type?" logic in factory
3. **Easy to Extend:** Add types without modifying client
4. **Use Interfaces:** Return interface, not concrete class
5. **Configuration-Driven:** Can load types from file/config

---

## Real-World Examples

- **DatabaseFactory:** Creates MySQL, PostgreSQL, MongoDB connections
- **LoggerFactory:** Creates File, Console, Cloud loggers
- **PaymentFactory:** Creates Credit Card, UPI, Wallet payments
- **NotificationFactory:** Creates SMS, Email, Push notifications
- **TransportFactory:** Creates Car, Truck, Bike instances

