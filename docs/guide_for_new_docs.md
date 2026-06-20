# Guide: Creating LLD Interview Docs for New Real-World Examples

Use this as a checklist whenever a new system is added to `realworldexamples/`.

---

## 0. Before You Write

Read every `.java` file in the package. Note:
- Which **design patterns** appear (Singleton, Builder, Strategy, State, Observer, Factory, Chain of Responsibility, etc.)
- The **threading model** (synchronized, ConcurrentHashMap, volatile, ExecutorService)
- Which classes are **mutable** vs **immutable**
- What **custom exceptions** are thrown and where
- Any business rules embedded in code that are easy to miss

---

## 1. Document Structure (follow this order)

### 1.1 Header
```
# System Name — LLD Interview Reference
```

### 1.2 System Overview (3-5 sentences)
- What the system does in plain language
- Scale / typical production concerns (optional for non-trivial systems)

### 1.3 Core Entities
A table or bullet list.  For each entity state:
| Entity | Key Fields | Responsibilities |
| ------ | ---------- | ---------------- |

For enums: list all values and explain the valid transitions.

### 1.4 Design Patterns Used
For each pattern:
- **Pattern name** — which class(es) implement it and **why** it was chosen over simpler alternatives

### 1.5 Database Schema
For **every entity** that would be persisted, write a `CREATE TABLE` block (SQL).
Required for each table:
- Primary key (prefer UUID for distributed systems, auto-increment only if explicitly simple)
- Foreign keys with explicit `ON DELETE` / `ON UPDATE` semantics
- `created_at` / `updated_at` timestamps
- Indexes for every query path
- Enum columns — state which DB type to use (e.g. `ENUM`, `VARCHAR`, or a separate lookup table) and why
- Add a comment after each non-obvious column

### 1.6 API Modelling
One sub-section per API endpoint.  For each endpoint:

```
METHOD /path/{param}
```
| Field | Description |
|-------|-------------|

**Request Body** (if applicable) — JSON example

**Response** — HTTP status codes

**Happy path** — what happens step by step

**Failure cases** — list every edge case and what HTTP status / error body to return

### 1.7 Concurrency & Thread-Safety Notes
- Identify every shared mutable state
- Note which locks protect what
- Highlight any race conditions present in the current code

### 1.8 Code Review Findings
A bulleted list of improvements, grouped by severity:
- **Critical** — correctness bugs, data-loss risk
- **Design** — pattern violations, missing abstractions
- **Minor** — naming, unnecessary mutability, etc.

Each finding should include:
- The exact location (class + line or method name)
- What is wrong
- The recommended fix

### 1.9 Extension Points
2-4 bullet points on how the system can be extended without major refactoring.

---

## 2. Writing Tips

- Use `code blocks` for SQL, JSON, and Java snippets
- The DB schema section is the most important for interviews — be complete
- For every failure case in the API section, explain *why* it fails (race condition? validation? external dependency?)
- Keep entity tables consistent with the actual Java field names
- Call out any mismatch between the in-memory model (Java classes) and what a real DB schema would require
- Where a pattern is present in code, reference the class names by name so the reader can cross-reference

---

## 3. Pattern Cheat-Sheet (quick reference)

| Pattern | When to use | What it solves |
|---------|-------------|----------------|
| Singleton | One shared resource (service, registry) | Prevents duplicate instances; ensures consistent state |
| Builder | Object with many optional fields | Replaces telescoping constructors; enforces required fields |
| Strategy | Multiple algorithms for the same step | Swap behaviour at runtime without if/else chains |
| State | Object behaves differently depending on lifecycle phase | Replaces large switch statements on status enums |
| Observer | Decouple event producer from consumers | Fan-out notifications without tight coupling |
| Factory / Abstract Factory | Centralize creation of a family of related objects | Shields callers from concrete types |
| Chain of Responsibility | Sequential processing with early-exit | Denomination dispensing, middleware pipelines |
| Template Method | Fixed skeleton, variable steps | Reduce duplication across similar workflows |
| Repository | Isolate persistence from domain logic | Makes it easy to swap storage backend |

---

## 4. Common DB Schema Pitfalls

- Storing floating-point money — use `DECIMAL(10,2)` not `DOUBLE`
- Missing soft-delete column (`deleted_at`) for auditable entities
- No `version` column for optimistic locking on high-contention rows (e.g., Seat, Order)
- Not indexing `foreign_key + status` combinations that are queried together
- Storing enums as plain strings without a check constraint
- Missing composite unique constraint (e.g., `(show_id, seat_id)` in bookings)

---

## 5. API Design Pitfalls

- Returning 200 with `{"success": false}` — use proper HTTP status codes
- Not specifying idempotency keys for payment/booking endpoints
- Returning internal exception messages to clients (security leak)
- Missing pagination on list endpoints
- Not documenting rate limits

---

## 6. Checklist Before Publishing

- [ ] Every Java entity has a corresponding DB table
- [ ] Every enum maps to a column definition
- [ ] Each API endpoint lists at least 3 failure cases
- [ ] Concurrency section covers every `synchronized` / `ConcurrentHashMap` in the source
- [ ] Code review section has at least one finding
- [ ] All SQL uses `DECIMAL(10,2)` for monetary values
- [ ] Extension Points section is filled
