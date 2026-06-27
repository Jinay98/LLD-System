# Amazon LP Interview Guide — Jinay Parekh

> **Rounds:** Tech 1 (LLD + Insist on Highest Standards + Deliver Results) | Tech 2 (DSA + Bias for Action + Dive Deep)

---

## Quick Navigation

| LP | Round | File | Primary Story |
|---|---|---|---|
| **Insist on Highest Standards** | Tech 1 (LLD) | [INSIST-ON-HIGHEST-STANDARDS.md](INSIST-ON-HIGHEST-STANDARDS.md) | Dream11 Notification System v2 |
| **Deliver Results** | Tech 1 (LLD) | [DELIVER-RESULTS.md](DELIVER-RESULTS.md) | Walmart Double-Slotting Race Condition Fix |
| **Bias for Action** | Tech 2 (DSA) | [BIAS-FOR-ACTION.md](BIAS-FOR-ACTION.md) | Walmart Pager Duty — Race Condition Hotfix |
| **Dive Deep** | Tech 2 (DSA) | [DIVE-DEEP.md](DIVE-DEEP.md) | Walmart Root Cause + Dream11 Notification Debug |

---

## Your Story Inventory

### Story A — Walmart: Double-Slotting Race Condition (The Pager Duty Incident)
- **What:** Race condition caused two freight packages to receive the same warehouse slot; found during on-call shift, manually unblocked, then root-caused and fixed with a DB-level unique-key constraint + capacity counter.
- **Fits:** Bias for Action (took action fast under pressure), Dive Deep (no logs → added logs, traced to race condition), Deliver Results (fixed before 42 DCs went live), Insist on Highest Standards (designed extensible capacity model instead of a quick patch)
- **Key numbers:** First of 43 DCs migrating to Atlas; bug fixed before rollout; worked weekends

### Story B — Dream11: Notification System v2 (Outbox + Checkpoint Pattern)
- **What:** Replaced fire-and-forget CleverTap calls with a durable, checkpointable notification pipeline using `notification_events` table, publisher cron, retry cron, and `last_successful_offset`.
- **Fits:** Insist on Highest Standards (rejected fire-and-forget, built extensible system), Deliver Results (shipped under tight timeline with working product driving engagement), Bias for Action (shipped MVP without over-engineering scope initially), Dive Deep (identified every failure mode: duplicates, rate limits, no observability)
- **Key numbers:** Club with 40K members, 39K notified, CleverTap 429 hit at page 39 — resumed from offset 39K, 0 duplicates

### Story C — Dream11: GraphQL Rewards Optimization (8-10 calls to 2 parallel calls)
- **What:** Reduced 8-10 sequential backend calls to 2 parallel calls by adding sectionIds param and using Promise.all.
- **Fits:** Insist on Highest Standards (raised the bar on API efficiency), Bias for Action (quick win, shipped fast)

### Story D — Dream11: Content Moderation Pipeline (Gemini LLM + Semaphore)
- **What:** Two-stage moderation with GetStream + Gemini, semaphore-limited concurrency, exponential backoff, 7 skip conditions.
- **Fits:** Insist on Highest Standards (every edge case handled), Dive Deep (traced failure modes, rate limit behaviour, skip conditions)

### Story E — Walmart: Inventory Service Kafka Reliability (Outbox Pattern)
- **What:** Inventory service (core service at Walmart) was intermittently dropping or mismatching Kafka messages to downstream services, causing downstream errors. Designed and built the Outbox Pattern — dual-write in the same DB transaction + a new "Rapid Re-layer" component that reads from the outbox table and publishes to Kafka reliably.
- **Fits:** **Insist on Highest Standards** (strongest — refused to accept intermittent message loss in a core service handling 5M+ events/day), **Dive Deep** (traced root cause: dual-write atomicity gap between DB write and Kafka publish), **Deliver Results** (downstream errors eliminated, 5M+ messages/day delivered reliably), **Bias for Action** (stepped in on a systemic cross-team problem proactively)
- **Key numbers:** 5M+ Kafka messages/day reliably delivered; downstream service errors eliminated

### Story F — Walmart: DB Purge Performance (Stored Procedures)
- **What:** High record volume caused slow query performance in the inventory service. Old programmatic purge could only delete ~150K records per run — new records were arriving faster than deletion. Moved to stored procedures: pre-compiled at DB engine level, eliminating application round-trip overhead. Deletion rate jumped dramatically, DB stayed clean.
- **Fits:** **Dive Deep** (strongest — understood root cause was round-trip latency overhead, not deletion logic; understood DB engine execution model vs. application-layer execution), **Insist on Highest Standards** (previous approach was "working" but failing at scale — raised the bar), **Deliver Results** (reversed a performance death spiral), **Bias for Action** (identified and fixed a systemic degradation proactively)
- **Key numbers:** Old purge: ~150K records per run. New purge: significantly higher (deletion rate exceeded record creation rate).

---

## Runtime Prompt

> Use RUNTIME-PROMPT.md during the interview to quickly pick the right story and know what to say/not say for each LP.

---

## How to Use This Guide

1. **Before the interview:** Read each LP file top-to-bottom once. Internalize the "What NOT to say" sections — these are your guardrails.
2. **During the interview:** Open RUNTIME-PROMPT.md. Paste the interviewer's question and follow the decision tree.
3. **Story delivery:** Always use STAR format. Keep Situation+Task under 90 seconds. Spend most time on Action. End with measurable Result.
4. **Pivoting:** Every story can be told for 2+ LPs by shifting emphasis. The LP files show exactly what to emphasize for each.
