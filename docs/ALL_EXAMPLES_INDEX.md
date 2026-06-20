# LLD Interview Preparation — Complete Library Index

Comprehensive Low-Level Design study resources covering real-world system design, API modelling, DB schema, design patterns, concurrency analysis, and code review findings.

---

## Interview Reference Documents (new detailed docs)

Each doc covers: entities, design patterns, full DB schema, API endpoints with failure cases, concurrency notes, and code review findings.

| System | Patterns | Complexity | Doc |
|--------|----------|-----------|-----|
| ATM System | Singleton, State, Chain of Responsibility | ⭐⭐⭐ | [atm_system.md](atm_system.md) |
| Movie Booking System | Singleton, Strategy (pricing + payment), Template Method | ⭐⭐⭐⭐ | [movie_booking_system.md](movie_booking_system.md) |
| Splitwise | Singleton, Strategy (split types), Observer | ⭐⭐⭐⭐ | [splitwise.md](splitwise.md) |
| Online Stock Exchange | Singleton, Builder, State, Observer, Strategy, Façade | ⭐⭐⭐⭐⭐ | [online_stock_exchange.md](online_stock_exchange.md) |
| URL Shortener | Singleton, Strategy (key gen), Observer, Repository, Builder | ⭐⭐⭐ | [url_shortener.md](url_shortener.md) |
| Zomato Food Ordering | Singleton, Factory, Strategy (payment + notification) | ⭐⭐⭐⭐ | [zomato_food_ordering.md](zomato_food_ordering.md) |
| Elevator System | Singleton, Strategy (dispatch), Observer, SCAN algorithm | ⭐⭐⭐⭐ | [elevator_system.md](elevator_system.md) |
| Pub-Sub System | Singleton, Observer, Thread Pool | ⭐⭐⭐ | [pub_sub_system.md](pub_sub_system.md) |
| Rate Limiter | Strategy (5 algorithms) | ⭐⭐⭐ | [rate_limiter.md](rate_limiter.md) |
| LRU Cache | Composite Data Structure | ⭐⭐ | [lru_cache.md](lru_cache.md) |
| Vending Machine | Singleton, State | ⭐⭐ | [vending_machine.md](vending_machine.md) |
| Tic-Tac-Toe | Singleton, Strategy (winning), Observer | ⭐⭐ | [tic_tac_toe.md](tic_tac_toe.md) |
| Snakes and Ladders | Builder, Template Method, Inheritance | ⭐ | [snakes_and_ladders.md](snakes_and_ladders.md) |

---

## Guide for Creating New Docs

When a new real-world example is added, follow the template in:

**[guide_for_new_docs.md](guide_for_new_docs.md)**

It contains: section structure, DB schema pitfalls, API design pitfalls, a pattern cheat-sheet, and a pre-publish checklist.

---

## Design Patterns Quick Reference

| Pattern | Used In |
|---------|---------|
| Singleton | ATM, Movie Booking, Splitwise, Stock Exchange, URL Shortener, Zomato, Elevator, PubSub, Vending, TicTacToe |
| Strategy | Movie Booking, Splitwise, Stock Exchange, URL Shortener, Zomato, Elevator, Rate Limiter, TicTacToe |
| State | ATM, Vending Machine, Stock Exchange (Order lifecycle) |
| Observer | Splitwise, Stock Exchange, URL Shortener, Elevator, Pub-Sub, TicTacToe |
| Builder | Stock Exchange (OrderBuilder), Snakes and Ladders (Game), URL Shortener (ShortenedURL) |
| Factory | Zomato (Payment, Notification, Order factories) |
| Chain of Responsibility | ATM (cash denomination dispensing) |
| Repository | URL Shortener (URLRepository) |
| Façade | Stock Exchange (StockBrokerageSystem) |

---

## Recommended Learning Path

### Quick review (30 min)
1. LRU Cache — pure data structure
2. Vending Machine — State pattern
3. Snakes and Ladders — Builder + game loop

### Intermediate (2 hours)
1. ATM System — State + CoR + threading
2. Pub-Sub — Observer + async threading
3. URL Shortener — Repository + Strategy
4. Rate Limiter — 5 algorithms compared

### Advanced (3+ hours)
1. Elevator System — multithreading + SCAN
2. Splitwise — balance ledger + concurrency
3. Movie Booking — seat locking + payment flow
4. Online Stock Exchange — order matching engine
5. Zomato — full e-commerce flow

---

## Key Interview Topics Covered

- **Concurrency:** Every doc has a dedicated "Concurrency & Thread-Safety Notes" section
- **DB Schema:** Complete SQL `CREATE TABLE` blocks with indexes, constraints, and comments in every doc
- **API Design:** Every endpoint includes happy path, failure cases, and HTTP status codes
- **Failure Handling:** TOCTOU races, payment failures, lock expiry, idempotency — all documented
- **Code Review:** Each doc has findings categorized as Critical / Design / Minor
