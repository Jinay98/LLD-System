# Singleton Pattern - Single Instance Control

## Overview
The **Singleton Pattern** ensures a class has **exactly one instance** and provides a global point of access to it.

---

## 🎯 Problem This Pattern Solves

### Without Singleton (Multiple Instances)
```java
DatabaseConnection db1 = new DatabaseConnection();
DatabaseConnection db2 = new DatabaseConnection();
DatabaseConnection db3 = new DatabaseConnection();

// Problem: 3 different connections instead of 1!
// Wasteful, inconsistent state, resource leak
```

### With Singleton (One Instance)
```java
DatabaseConnection db1 = DatabaseConnection.getInstance();
DatabaseConnection db2 = DatabaseConnection.getInstance();
DatabaseConnection db3 = DatabaseConnection.getInstance();

// All refer to SAME instance (db1 == db2 == db3)
// Efficient, consistent, guaranteed single connection
```

---

## 🏗️ Singleton Implementation

### Basic Singleton
```java
public class Singleton {
    private static Singleton instance;
    
    // Private constructor (prevent new)
    private Singleton() {}
    
    // Global access point
    public static Singleton getInstance() {
        if (instance == null) {
            instance = new Singleton();
        }
        return instance;
    }
}

// Usage
Singleton s1 = Singleton.getInstance();
Singleton s2 = Singleton.getInstance();
// s1 == s2 ✓ Same instance
```

### Thread-Safe Singleton (Synchronized)
```java
public class ThreadSafeSingleton {
    private static ThreadSafeSingleton instance;
    
    private ThreadSafeSingleton() {}
    
    // Synchronized to prevent multiple threads creating instance
    public static synchronized ThreadSafeSingleton getInstance() {
        if (instance == null) {
            instance = new ThreadSafeSingleton();
        }
        return instance;
    }
}
```

### Eager Singleton (Created at Class Load)
```java
public class EagerSingleton {
    // Instance created when class loads
    private static EagerSingleton instance = new EagerSingleton();
    
    private EagerSingleton() {}
    
    public static EagerSingleton getInstance() {
        return instance;
    }
}

// Pros: Thread-safe, simple
// Cons: Always created (even if never used)
```

### Lazy Singleton with Double-Check Locking
```java
public class LazyThreadSafeSingleton {
    private static LazyThreadSafeSingleton instance;
    
    private LazyThreadSafeSingleton() {}
    
    public static LazyThreadSafeSingleton getInstance() {
        if (instance == null) {  // First check (no lock)
            synchronized(LazyThreadSafeSingleton.class) {
                if (instance == null) {  // Second check (with lock)
                    instance = new LazyThreadSafeSingleton();
                }
            }
        }
        return instance;
    }
}

// Best: Lazy + thread-safe + efficient
```

---

## ✅ When to Use Singleton

| Use Case | Example |
|----------|---------|
| **Global Configuration** | AppConfig.getInstance() |
| **Database Connection** | DatabaseConnection.getInstance() |
| **Logger** | Logger.getInstance() |
| **Cache** | CacheManager.getInstance() |
| **Connection Pool** | ConnectionPool.getInstance() |

---

## ❌ When NOT to Use Singleton

| Mistake | Why Bad |
|---------|---------|
| **Per-user state** | Don't make CartManager singleton; each user needs their own |
| **Testing** | Hard to mock, creates hidden dependencies |
| **Lazy loading** | Can cause issues if created in wrong thread |
| **Multiple instances needed** | Don't force-fit singleton |

---

## 🧪 Testing Singleton

### Problem: Hard to Test
```java
public class DatabaseTest {
    @Test
    public void testQuery() {
        // Can't control DatabaseConnection instance
        // Can't test with mock/fake database
        // Test depends on real database!
    }
}
```

### Solution: Use Interface + Factory
```java
// Interface makes it mockable
public interface IDatabase {
    ResultSet query(String sql);
}

// Singleton implementation
public class Database implements IDatabase {
    private static Database instance;
    // ...
}

// In test: use mock implementation
public class MockDatabase implements IDatabase {
    public ResultSet query(String sql) {
        // Return test data
    }
}
```

---

## 🔄 Singleton vs Global Variable

| Aspect | Global Variable | Singleton |
|--------|---|---|
| **Access** | `globalVariable` | `Singleton.getInstance()` |
| **Control** | None | Controlled initialization |
| **Inheritance** | ❌ No | ✅ Can extend |
| **Lazy Load** | ❌ No | ✅ Yes |
| **Thread-Safe** | ❌ No | ✅ Can be |

---

## 💡 Key Takeaways

1. **One Instance:** Guaranteed only one exists
2. **Global Access:** Available everywhere via getInstance()
3. **Lazy Creation:** Can delay initialization
4. **Thread-Safe:** Can handle multiple threads (with proper implementation)
5. **Private Constructor:** Prevents accidental `new` calls
6. **Use Carefully:** Can hide dependencies and make testing hard

---

## Common Mistakes

### ❌ Mistake 1: Singleton for Per-User State
```java
// WRONG: All users share same cart!
public class CartManager {
    private static CartManager instance = new CartManager();
    private List<CartItem> items;  // ALL USERS SHARE THIS!
}

// RIGHT: Per-user instance
public class User {
    private CartManager cartManager;  // Each user has own
}
```

### ❌ Mistake 2: Forgetting Thread Safety
```java
// WRONG: Race condition
if (instance == null) {
    instance = new Singleton();  // Two threads might create two instances!
}

// RIGHT: Synchronized
synchronized(Singleton.class) {
    if (instance == null) {
        instance = new Singleton();
    }
}
```

### ❌ Mistake 3: Not Making Constructor Private
```java
// WRONG: Anyone can create new instance
public class BadSingleton {
    public BadSingleton() {}
}

new BadSingleton();  // Oops, another instance!

// RIGHT:
private Singleton() {}  // Prevents this
```

---

## Real-World Examples

- **Database:** Single connection pool
- **Logger:** Single log output stream
- **Config:** Single application configuration
- **Cache:** Single cache manager
- **Thread Pool:** Single executor service
- **RestaurantManager:** Single restaurant database (Zomato example!)

