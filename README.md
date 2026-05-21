# LLD Systems - Interview Prep

Low Level Design (LLD) implementations for interview preparation.

## Project Structure

```
lld-systems/
├── src/main/java/com/lld/
│   ├── parkinglot/       # Parking Lot LLD System
│   │   ├── models/       # Domain entities
│   │   ├── services/     # Business logic interfaces
│   │   ├── repository/   # Data access interfaces
│   │   ├── dto/          # Data transfer objects
│   │   └── Main.java     # Entry point (runnable independently)
│   ├── twitter/          # Twitter LLD System (add new systems here)
│   └── ecommerce/        # E-commerce LLD System
└── pom.xml               # Maven configuration
```

## Setup

### Prerequisites
- Java 17 or later
- Maven 3.5+

### Build the Project
```bash
cd /Users/jinay-parekh/Dream11/lld-systems
mvn clean compile
```

## Running Systems

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

## Testing

Tests go in `src/test/java/` with the same package structure as main code.

```bash
mvn test
```

## Adding Dependencies

Edit `pom.xml` to add new libraries (e.g., Gson, Mockito, etc.).
