# Slice SDE 2 LLD & HLD Interview Prep Guide

Welcome! This folder contains targeted high-level and low-level system design documents structured specifically for the **Slice SDE 2 (System Design + Low-Level Design) Interview Round**.

Each document strictly follows the structure of your **real-time copilot prompt** (`slice_sde2_claude_prompt_final.md`), detailing clarifying questions, assumptions, short high-level design (HLD), highly detailed PostgreSQL schemas with constraints, index justifications, locking strategies, class diagrams, SOLID design principles, 4 key SQL queries, and senior-level edge cases.

---

## 📚 Interview Design Reference sheets

1. **💳 [Digital Wallet & Ledger System](file:///Users/jinay-parekh/Dream11/lld-systems/docs/slice/digital_wallet_ledger.md)**
   - *Key Focus:* Double-entry bookkeeping, transactional ACID integrity, strict ledger-based balance caches, deadlock prevention when locking multiple rows, and balance drift reconciliations.

2. **🔄 [Payment Aggregator & UPI Gateway Integration](file:///Users/jinay-parekh/Dream11/lld-systems/docs/slice/payment_aggregator_upi.md)**
   - *Key Focus:* Dynamic payment routing, circuit breakers (gateway failover), handling async webhook webpings, state machines for payment transitions, and resolving PENDING transactions via pollers.

3. **💸 [Expense Sharing System (Splitwise-style)](file:///Users/jinay-parekh/Dream11/lld-systems/docs/slice/expense_sharing_splitwise.md)**
   - *Key Focus:* Pairwise user balance trackers, multi-user split logic, split validation algorithms using the Strategy Pattern, precision decimal remainder allocation, and debt simplification algorithms.

4. **✅ [Task Management System](file:///Users/jinay-parekh/Dream11/lld-systems/docs/slice/task_management.md)**
   - *Key Focus:* Task status lifecycles using the State Pattern, optimistic locking checks (`version` column) for low-contention entities, append-only history audits, and background SLA overdue cron systems.

5. **🚚 [Delivery Tracking System](file:///Users/jinay-parekh/Dream11/lld-systems/docs/slice/delivery_tracking.md)**
   - *Key Focus:* Real-time location tracking using high-throughput Redis databases (NoSQL justification), geospatial querying, dead reckoning for locations, geo-fenced transitions, and out-of-order webhook resolutions.

6. **🎫 [Event Booking System (BookMyShow-style)](file:///Users/jinay-parekh/Dream11/lld-systems/docs/slice/event_booking.md)**
   - *Key Focus:* Highly concurrent seat-holds during flash sales using fast Redis temporary TTL locks, atomic seat booking confirmation queries, partial hold failures rollback, and database clean-up daemons.

7. **📅 [Leave Management System](file:///Users/jinay-parekh/Dream11/lld-systems/docs/slice/leave_management.md)**
   - *Key Focus:* Leave balance validation, multi-level approvals using the Chain of Responsibility Pattern, date overlap checking queries, half-day leaf units represented as integers, and monthly balance accruals.

---

## ⚡ Quick Revision Checklist for the LLD Round

Before entering the panel, ensure you can quickly explain these principles aloud:

* **No Floats for Currency:** Always use `BIGINT` in paise (or `DECIMAL(15,2)`). Explain binary floating point limits (`0.1 + 0.2 = 0.30000000000000004`).
* **Optimistic vs. Pessimistic Locking:** Use Pessimistic (`SELECT ... FOR UPDATE`) for high-contention financial balance edits. Use Optimistic (`version` column) for low-contention entities like task states or leave balance requests.
* **Deadlock Prevention:** When locking multiple rows (e.g., wallet transfers), sort the identifiers first using `LEAST(A, B)` and `GREATEST(A, B)` to always lock rows in a consistent order.
* **Append-Only Auditing:** Keep transaction ledgers and status history tables insert-only. Do not run `UPDATE` statements on ledger records.
* **Cursor Pagination:** Use cursor-based (`WHERE id < :cursor_id ORDER BY id DESC LIMIT N`) instead of `OFFSET` pagination for feeds to achieve $O(\log N)$ performance at deep page levels.
* **Transactional Outbox:** Write event logs to an `outbox` table within the same DB transaction to guarantee event publishing atomicity.
