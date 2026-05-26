# LLD Systems - Interview Prep

Low Level Design (LLD) implementations and Design Patterns for interview preparation.

## Project Structure

```
lld-systems/
├── src/main/java/com/lld/
│   ├── patterns/                 # Design Patterns Examples
│   │   └── FactoryPattern/
│   │       ├── SimpleFactoryPattern/     # Static factory approach
│   │       │   ├── models/
│   │       │   │   ├── Burger.java       # Abstract product
│   │       │   │   ├── SmallBurger.java
│   │       │   │   └── LargeBurger.java
│   │       │   ├── factory/
│   │       │   │   ├── BurgerFactory.java
│   │       │   │   └── enums/
│   │       │   │       └── BurgerTypes.java
│   │       │   └── Client.java
│   │       ├── FactoryMethodPattern/     # Subclass-driven creation
│   │       │   ├── models/
│   │       │   │   ├── Burger.java       # Abstract product
│   │       │   │   ├── small/
│   │       │   │   │   ├── SmallBurger.java (abstract)
│   │       │   │   │   ├── RegularSmallBurger.java
│   │       │   │   │   └── PremiumSmallBurger.java
│   │       │   │   └── large/
│   │       │   │       ├── LargeBurger.java (abstract)
│   │       │   │       ├── RegularLargeBurger.java
│   │       │   │       └── PremiumLargeBurger.java
│   │       │   ├── creators/
│   │       │   │   ├── BurgerRestaurant.java (abstract factory method)
│   │       │   │   ├── small/
│   │       │   │   │   ├── SmallRegularBurgerRestaurant.java
│   │       │   │   │   └── SmallPremiumBurgerRestaurant.java
│   │       │   │   └── large/
│   │       │   │       ├── LargeRegularBurgerRestaurant.java
│   │       │   │       └── LargePremiumBurgerRestaurant.java
│   │       │   └── Client.java
│   │       └── AbstractFactoryPattern/   # Family-based creation (multi-product)
│   │           ├── models/
│   │           │   ├── burger/
│   │           │   │   ├── Burger.java (interface)
│   │           │   │   ├── vegetarian/
│   │           │   │   │   └── VegetarianBurger.java
│   │           │   │   └── nonvegetarian/
│   │           │   │       └── NonVegetarianBurger.java
│   │           │   ├── fries/
│   │           │   │   ├── Fries.java (interface)
│   │           │   │   ├── vegetarian/
│   │           │   │   │   └── VegetarianFries.java
│   │           │   │   └── nonvegetarian/
│   │           │   │       └── NonVegetarianFries.java
│   │           │   └── sauce/
│   │           │       ├── Sauce.java (interface)
│   │           │       ├── vegetarian/
│   │           │       │   └── VegetarianSauce.java
│   │           │       └── nonvegetarian/
│   │           │           └── NonVegetarianSauce.java
│   │           ├── factories/
│   │           │   ├── RestaurantFactory.java (abstract factory)
│   │           │   ├── VegRestaurantFactory.java
│   │           │   └── NonVegRestaurantFactory.java
│   │           └── Client.java
│   ├── parkinglot/       # Parking Lot LLD System
│   │   ├── models/       # Domain entities
│   │   ├── services/     # Business logic interfaces
│   │   ├── repository/   # Data access interfaces
│   │   ├── dto/          # Data transfer objects
│   │   └── Main.java     # Entry point (runnable independently)
│   ├── twitter/          # Twitter LLD System
│   └── ecommerce/        # E-commerce LLD System
└── pom.xml               # Maven configuration
```

## Design Patterns

This project includes implementations of classic design patterns with burger restaurant examples for clarity.

### Simple Factory Pattern
**Location:** `src/main/java/com/lld/patterns/FactoryPattern/SimpleFactoryPattern/`

A single factory class that encapsulates object creation logic using conditionals.

**Structure:**
- `BurgerFactory.java` - Static factory method that decides which burger to create
- `models/` - Product classes (Burger, SmallBurger, LargeBurger)
- `factory/` - Factory logic and enums

**Run:**
```bash
java -cp target/classes com.lld.patterns.FactoryPattern.SimpleFactoryPattern.Client
```

**Key Insight:** Client doesn't know product classes; factory hides creation logic.

---

### Factory Method Pattern
**Location:** `src/main/java/com/lld/patterns/FactoryPattern/FactoryMethodPattern/`

Each creator subclass decides how to create its product via an abstract factory method.

**Structure:**
- `BurgerRestaurant.java` - Abstract creator with `createBurger()` factory method
- `creators/` - Concrete restaurants (SmallRegularBurgerRestaurant, etc.)
- `models/` - Product hierarchy (Burger → SmallBurger/LargeBurger → Regular/Premium variants)

**Run:**
```bash
java -cp target/classes com.lld.patterns.FactoryPattern.FactoryMethodPattern.Client
```

**Key Insight:** Delegates object creation to subclasses; each subclass knows what it creates.

---

### Abstract Factory Pattern
**Location:** `src/main/java/com/lld/patterns/FactoryPattern/AbstractFactoryPattern/`

Creates families of related products (e.g., Vegetarian and Non-Vegetarian sets) that work together.

**Structure:**
- `RestaurantFactory.java` - Abstract factory with methods for each product type
- `factories/` - Concrete factories (VegRestaurantFactory, NonVegRestaurantFactory)
- `models/` - Product families (Burger, Fries, Sauce for each diet type)

**Run:**
```bash
java -cp target/classes com.lld.patterns.FactoryPattern.AbstractFactoryPattern.Client
```

**Key Insight:** Ensures product families are consistent; if you order Vegetarian, all items are Vegetarian.

---

## Setup

### Prerequisites
- Java 17 or later
- Maven 3.5+

### Build the Project
```bash
cd /Users/jinay-parekh/Dream11/lld-systems
mvn clean compile
```

## Running Design Patterns

Each pattern has its own `Client.java` and demonstrates the pattern independently.

```bash
# Compile once
mvn compile

# Run Simple Factory Pattern
java -cp target/classes com.lld.patterns.FactoryPattern.SimpleFactoryPattern.Client

# Run Factory Method Pattern
java -cp target/classes com.lld.patterns.FactoryPattern.FactoryMethodPattern.Client

# Run Abstract Factory Pattern
java -cp target/classes com.lld.patterns.FactoryPattern.AbstractFactoryPattern.Client
```

---

## Adding a New Pattern

### Step 1: Create Pattern Directory
```bash
mkdir -p src/main/java/com/lld/patterns/FactoryPattern/YourPatternName
cd src/main/java/com/lld/patterns/FactoryPattern/YourPatternName
```

### Step 2: Organize by Responsibility
```
YourPatternName/
├── models/          # Domain products/interfaces
├── factories/       # Factory logic (if applicable)
├── creators/        # Creator/Builder classes (if applicable)
└── Client.java      # Demonstration entry point
```

### Step 3: Create Interfaces/Abstractions First
- Define product interfaces in `models/`
- Define factory interfaces in `factories/`

### Step 4: Implement Concrete Classes
- Concrete products inherit from interfaces
- Concrete factories implement factory interfaces

### Step 5: Create Client Demonstration
```java
public class Client {
    public static void main(String[] args) {
        // Demonstrate your pattern here
    }
}
```

### Step 6: Test
```bash
mvn compile
java -cp target/classes com.lld.patterns.FactoryPattern.YourPatternName.Client
```

---

## Running LLD Systems

Each system has its own `Main.java` and can be run independently.

### Run Parking Lot System
```bash
mvn compile exec:java -Dexec.mainClass="com.lld.parkinglot.Main"
```

Or after compilation:
```bash
java -cp target/classes com.lld.parkinglot.Main
```

### Add a New LLD System

1. Create a new package under `src/main/java/com/lld/`
   ```bash
   mkdir -p src/main/java/com/lld/newsystem/{models,services,repository,dto}
   ```

2. Create `Main.java` in that system's root package

3. Create your models, DTOs, services, and repositories

4. Run: `mvn compile exec:java -Dexec.mainClass="com.lld.newsystem.Main"`

## IntelliJ Setup

1. Open IntelliJ IDEA
2. File → Open → Select `/Users/jinay-parekh/Dream11/lld-systems`
3. Choose "Open as Project"
4. Right-click `pom.xml` → Maven → Reload Project
5. Wait for Maven to download dependencies

### Running from IntelliJ
- Right-click any `Main.java` → Run 'Main.main()'
- Or use: Run → Edit Configurations → add new Maven configuration

## Folder Organization

### models/
Domain entities and core business objects
- `ParkingSpot.java`
- `Vehicle.java`

### dto/
Data Transfer Objects for API/service boundaries
- `ParkingSpotDTO.java`

### services/
Business logic interfaces and implementations
- `ParkingLotService.java`
- `ParkingLotServiceImpl.java` (when you implement)

### repository/
Data access layer (interfaces and implementations)
- `ParkingSpotRepository.java`
- `InMemoryParkingSpotRepository.java` (when you implement)

## Maven Commands

```bash
# Compile
mvn compile

# Run tests
mvn test

# Package as JAR
mvn package

# Clean build artifacts
mvn clean

# Run a specific main class
mvn compile exec:java -Dexec.mainClass="com.lld.parkinglot.Main"
```

## Design Pattern Comparison

| Aspect | Simple Factory | Factory Method | Abstract Factory |
|--------|---|---|---|
| **Location** | Single factory class | Scattered across subclasses | Single factory interface |
| **Products** | One type | One type per family | Multiple types per family |
| **Families** | None | None | Multiple (e.g., Vegetarian, Non-Vegetarian) |
| **Consistency** | No guarantee | No guarantee | **Ensures family consistency** |
| **Extensibility** | Add conditions | Add subclasses | Add factory implementations |
| **Coupling** | Client knows factory | Client knows creator subclasses | Client knows factory interface |
| **Best for** | Simple, limited options | When subclasses decide creation | Related products that work together |

---

## Folder Organization Best Practices

### Models/
- **What:** Domain products, abstractions
- **Structure:** Group by type (burger/, fries/, sauce/)
- **Example:** `models/burger/Burger.java`, `models/burger/vegetarian/VegetarianBurger.java`

### Factories/
- **What:** Factory logic and related enums
- **Structure:** Keep factory interfaces and implementations together
- **Example:** `factories/RestaurantFactory.java`, `factories/VegRestaurantFactory.java`

### Creators/
- **What:** Creator/Builder classes (for Factory Method)
- **Structure:** Group by product type
- **Example:** `creators/BurgerRestaurant.java`, `creators/small/SmallPremiumBurgerRestaurant.java`

### Client/
- **What:** Demonstration and entry point
- **Example:** `Client.java` or `src/test/java/...ClientTest.java`

---

## Testing

Tests go in `src/test/java/` with the same package structure as main code.

```bash
mvn test
```

---

## Adding Dependencies

Edit `pom.xml` to add new libraries (e.g., Gson, Mockito, etc.).

---

## Code Organization Rules

1. **One file per class** - No multiple public classes in one file
2. **Max 800 lines per file** - Extract utilities if larger
3. **High cohesion, low coupling** - Each file has single responsibility
4. **Interfaces before implementations** - Define contracts first
5. **Immutable patterns** - Create new objects, don't mutate existing ones
