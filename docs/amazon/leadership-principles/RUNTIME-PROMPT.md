# RUNTIME PROMPT — Amazon LP Interview Helper
## Paste the interviewer's question below and follow the decision tree

---

## STEP 1: Identify the LP from the question keyword

| If the question contains... | LP | Round |
|---|---|---|
| "quality", "standards", "unsatisfied", "improve quality", "raise the bar" | Insist on Highest Standards | Tech 1 |
| "deadline", "deliver", "obstacle", "setback", "commitment", "results" | Deliver Results | Tech 1 |
| "speed", "quickly", "tight timeline", "limited information", "proactive", "risk" | Bias for Action | Tech 2 |
| "root cause", "complex problem", "dig deep", "details", "understand", "investigate" | Dive Deep | Tech 2 |

---

## STEP 2: Pick your story

```
Was the question about...

A) A quality/standards tradeoff or raising the bar?
   → PRIMARY:  Dream11 Notification v2 (rejected fire-and-forget, 3 specific deficiencies)
   → BACKUP 1: Walmart Race Condition (refused to patch, designed capacity model)
   → BACKUP 2: Walmart Kafka Outbox — Story E (refused best-effort for 5M+ events/day)
   → BACKUP 3: Walmart DB Purge — Story F (refused to tune around root cause)

B) Delivering despite obstacles / hitting a deadline?
   → PRIMARY:  Walmart Pager Duty (unanticipated obstacles: no logs, intermittent, live system)
   → BACKUP 1: Dream11 Notif v2 (deferred scope, shipped on time for cricket season)
   → BACKUP 2: Walmart Kafka Outbox — Story E (cross-team impact, systemic obstacle)
   → BACKUP 3: Walmart DB Purge — Story F (reversed degrading system before crisis)

C) Moving fast with incomplete information / proactive action?
   → PRIMARY:  Walmart Pager Duty (manual fix as first action, no logs, warehouse blocked)
   → BACKUP 1: Dream11 Notif v2 (architecture decisions without scale data)
   → BACKUP 2: Walmart Kafka Outbox — Story E (nobody owned it, I stepped in fast)
   → BACKUP 3: Walmart DB Purge — Story F (caught degradation trend before incident)

D) Digging into root cause / complex technical problem?
   → PRIMARY:  Walmart Race Condition (traced TOCTOU, questioned app-level mutex assumption)
   → BACKUP 1: Dream11 Failure Mode Analysis (mapped all failure paths before schema)
   → BACKUP 2: Walmart Kafka Outbox — Story E (dual-write atomicity gap, not Kafka broker)
   → BACKUP 3: Walmart DB Purge — Story F (round-trip overhead, not batch size)
```

---

## STEP 3: STAR Checklist (speak in this order)

```
[ ] SITUATION — 30 sec max. Set the scene. What company, what system, what was happening.
[ ] TASK — 15 sec. What was YOUR responsibility. Use "I", not "we".
[ ] ACTION — 60-70% of total time. Lead with YOUR decisions. Be specific.
            Name the alternatives you considered and why you rejected them.
            Show the investigative steps or the design choices.
[ ] RESULT — 20 sec. Quantify. Business impact. Durability ("this is still in production").
```

---

## STEP 4: Quick Story Templates

### STORY A — Walmart Race Condition (Pager Duty)

```
[SITUATION]
On-call at Walmart. Paged for double-slotting at our first Atlas DC.
Two freight packages getting the same warehouse slot. Warehouse associates blocked.
First of 43 DCs to migrate — high stakes.

[TASK]
I had to resolve the incident AND prevent it from happening in the 42 DCs lined up next.

[ACTION — pick the lens based on LP]

  BIAS FOR ACTION lens:
  → "I had no logs and couldn't take the system down. My first move: manually
     corrected the slot assignments in DB to unblock the associates immediately.
     Calculated risk — I made a targeted, minimal change. Not perfect, right first action.
     Then deployed logs with peer review. Waited for reproduction. Got root cause.
     DB-level unique constraint was the fix — atomic, works across JVM instances."

  DIVE DEEP lens:
  → "Intermittent bug + no logs = needed real data. I added structured logging to
     trace full slot assignment lifecycle: request → snapshot → recommendation → write.
     Logs showed classic TOCTOU: Thread A and B both read 'Slot 7 available' simultaneously.
     Both recommended it. Both wrote to it. Race condition confirmed.
     Questioned fix options: app-level mutex? No — doesn't span JVM instances.
     DB-level unique key constraint — atomic, cross-instance. Chose that.
     Also made slot capacity explicit — capacity was hardcoded as 1 everywhere.
     Made it a DB column. This was prescient — larger slots were needed within months."

  DELIVER RESULTS lens:
  → "Three obstacles: no logs, intermittent bug, couldn't reproduce on demand.
     I worked through each: deployed logs → waited for reproduction → traced root cause.
     Worked through the weekend to get the fix shipped before DC #2 went live.
     DC #2 onboarded clean. 42 subsequent DCs — no double-slotting incident."

  INSIST ON HIGHEST STANDARDS lens:
  → "The quick fix was: patch the slot and close the ticket. That wasn't acceptable.
     I refused to close without a root cause. Added logs. Got data. Traced the race.
     Designed a DB-level constraint instead of an app-level patch.
     Also made capacity dynamic — not because it was asked, but because hardcoded
     capacity of 1 was a design assumption that would break at scale. It did break —
     and we were ready."

[RESULT]
Race condition fixed before DC #2. 42 DCs migrated cleanly.
Capacity model reused when warehouse introduced larger pallet slots (no code change needed).
```

---

### STORY B — Dream11 Notification v2

```
[SITUATION]
At Dream11, building Clubs and Threads — WhatsApp-like groups + Twitter-like feed
for fantasy sports. Notifications needed to drive engagement.
Existing approach (v1): fire-and-forget call to CleverTap inline in business logic.

[TASK]
Design and build a notification delivery system we could rely on at scale.
Tight timeline — Clubs feature launching before cricket season.

[ACTION — pick the lens based on LP]

  INSIST ON HIGHEST STANDARDS lens:
  → "I was unsatisfied with v1 for three specific reasons — not vague, specific:
     1. No observability: couldn't tell if 40K-member club got their notifications
     2. No durability: failure at member 39,999 → retry from member 0 → 39,999 duplicates
     3. No extensibility: CleverTap URL in business logic, every new type touched 3-4 services
     I raised this with the team as a quality gate. We would not ship with v1.
     Designed notification_events table as source of truth, last_successful_offset for
     checkpointing, provider-agnostic /v1/notifications/send endpoint, VARCHAR not ENUM
     for event_type so new types = zero migrations."

  DELIVER RESULTS lens:
  → "Tight timeline, dependency on external API with rate limits we couldn't control.
     I scoped deliberately: deferred SPECIFIC_USER scope (single-user notifications)
     because the schema was already extensible for it — it just needed a handler.
     Shipped ALL_MEMBERS + ALL_MEMBERS_EXCLUDING_ACTOR on time for cricket season.
     First match day with Clubs notifications showed measurable engagement spike.
     SPECIFIC_USER enabled in follow-up sprint with zero architectural changes."

  BIAS FOR ACTION lens:
  → "We didn't have scale data. Didn't know how many clubs, how many members per club.
     I made decisions on knowns: clubs are unbounded in membership, CleverTap has rate limits,
     we need retry capability. Designed around those. Deferred unknowns (SPECIFIC_USER scope).
     Shipped. When the data came in, the deferral was validated — single-user was a
     smaller use case. The core architecture held."

  DIVE DEEP lens:
  → "I catalogued every failure mode before writing the schema.
     Obvious failure: CleverTap 500. Handle with retry.
     Non-obvious 1: JVM crash mid-pagination → SQS redelivery → start from 0 → 39K duplicates.
       Fix: last_successful_offset checkpoint.
     Non-obvious 2: CleverTap 429 → drop? retry immediately? need backoff AND checkpoint.
       Fix: RETRY_ELIGIBLE status + retry cron with configurable interval.
     Non-obvious 3: Two cron instances read same PENDING row simultaneously → duplicate SQS messages.
       Fix: Consumer checks DB for SUCCESSFUL status before paginating when isRetry=true.
     Non-obvious 4: SQS redelivery gives no backoff control.
       Fix: Consumer acknowledges SQS even on failure. Retry is DB-driven, not SQS-driven.
     Each design decision traces to a specific failure mode I analyzed."

[RESULT]
Shipped on time. Zero duplicate notifications since launch.
3 CleverTap outage scenarios since launch — all recovered cleanly via retry.
40K-member club: hit 429 at page 39 → resumed from offset 39K → 0 duplicates for those 39K.
New event types: zero schema migrations, one constant + one handler.
```

---

### STORY C — Walmart Kafka Outbox (Story E)

```
[SITUATION]
At Walmart, inventory service = source of truth for all inventory state.
Publishes Kafka messages to pricing, fulfillment, recommendations.
Intermittently dropping or mismatching messages. Downstream teams filing incidents.
Surface symptom: looks like a Kafka reliability issue.

[TASK]
Trace root cause and fix reliably — 5M+ messages/day, cross-team business impact.

[ACTION — pick the lens based on LP]

  INSIST ON HIGHEST STANDARDS lens:
  → "Easy fix: add retry logic on the publisher. I could have shipped that in a day.
     Instead I identified the fundamental flaw: DB write and Kafka publish were two
     separate operations with no atomicity. App crash between them = event permanently lost.
     Standard I refused: 'best-effort delivery for a core inventory event stream.'
     I designed Outbox Pattern: same DB transaction writes to inventory table + outbox table.
     Rapid Re-layer component reads outbox and publishes to Kafka — sole responsibility.
     Made outbox schema-driven so new downstream consumers = zero infrastructure change."

  DIVE DEEP lens:
  → "First hypothesis: Kafka broker. Checked broker metrics — no anomalies. Consumer lag normal.
     Intermittency pattern didn't correlate with Kafka load. Dug into publish side.
     Traced exact code path: inventory DB write → Kafka produce call. Two separate ops.
     No transaction boundary. JVM crash between them = silent event loss. No audit trail.
     This is the dual-write problem. Retry doesn't solve it — process is dead.
     Once I understood the precise failure boundary, Outbox Pattern was the logical answer."

  DELIVER RESULTS lens:
  → "Downstream teams blocked by our failures. Real business impact: fulfillment picking
     wrong items, pricing out of sync. I stepped in cross-team, designed + shipped
     Outbox Pattern, moved quickly without waiting for full design committee review —
     shared design in parallel with stakeholders while building.
     Result: 5M+ messages/day, downstream errors from missed events → zero."

  BIAS FOR ACTION lens:
  → "Nobody owned this cross-team. Symptom looked external (Kafka). I stepped in immediately,
     didn't wait to be assigned. Identified root cause within days. Moved to design + build
     Rapid Re-layer while sharing design for parallel review — didn't let process slow delivery."

[RESULT]
5M+ Kafka messages/day delivered reliably.
Downstream incident rate for inventory events: zero.
Rapid Re-layer became standard pattern for event publishing in inventory domain.
```

---

### STORY D — Walmart DB Purge (Story F)

```
[SITUATION]
Inventory service at Walmart: high record volume, slow query performance.
Programmatic purge: ~150K records per run. New records: far more per run.
DB growing unbounded. Query latency trending up.

[TASK]
Reverse the growth before it becomes a production incident.

[ACTION — pick the lens based on LP]

  DIVE DEEP lens:
  → "Obvious fix: increase batch size. I went deeper: WHY does programmatic purge have a ceiling?
     Every deletion = application round trip: send DELETE over network → DB parses + executes
     → result back over network → application processes → repeat.
     At 150K records (150 round trips at ~1K/batch): network latency ×2 per trip,
     query planning overhead per call, serialization cost. Can't scale past connection
     timeout and memory limits.
     Question I asked: why does the DB need to talk to the application to delete its own data?
     It doesn't. Stored procedure = pre-compiled in DB engine, runs locally, zero round trips."

  INSIST ON HIGHEST STANDARDS lens:
  → "Existing purge was 'working' — just not fast enough. Most engineers would tune batch size.
     I traced the structural inefficiency: application-level round-trip overhead.
     Refused to tune around the real problem. Moved to stored procedures.
     Deletion rate exceeded creation rate. DB stabilized. Standard: system should stay clean,
     not approach a cliff and hope the purge keeps up."

  BIAS FOR ACTION lens:
  → "Caught the degradation trend from query metrics before it became a pager alert.
     Wasn't assigned to look at this. Saw the trend, traced it immediately, moved to fix.
     Stored procedures was a deliberate architectural decision, not just a config change."

  DELIVER RESULTS lens:
  → "DB was heading toward a performance cliff. Query latency degrading on our most-read table.
     I reversed it before it became a production crisis. Deletion rate exceeded creation rate.
     Query performance recovered. Stored procedure pattern adopted for all high-volume
     maintenance jobs in the inventory service."

[RESULT]
Deletion rate exceeded creation rate for first time.
DB size stabilized → query performance on inventory table recovered.
Stored procedure approach: new standard for high-volume maintenance jobs.
```

---

## STEP 5: Follow-up Guardrails

**If interviewer asks "what would you do differently?"**
- For Walmart: "I'd add alerts on the slot assignment failure rate so we catch intermittent issues before a pager fires. Proactive monitoring instead of reactive paging."
- For Dream11: "I'd instrument notification delivery latency end-to-end — from DB insert to CleverTap confirmation. Right now we track status but not latency per hop."

**If interviewer asks "how did your team react?"**
- For Walmart: "My teammate reviewed the log changes before I deployed. I got a code review, I didn't act unilaterally on a running production system."
- For Dream11: "I got alignment from the tech lead and product before redesigning. It wasn't a solo refactor — the team understood the quality argument."

**If interviewer probes a technical detail you haven't prepared:**
- "That's a great question. Let me think through it." (Pause, actually think, then answer. Don't bluff.)
- If you don't know: "I'd need to look at the specifics of how X works. My approach would be Y, but I'd validate that assumption."

---

## STEP 6: Story Reuse Map

```
Walmart Race Condition story (Story A):
  ✓ Bias for Action        (manual fix as first action, deploy logs fast)
  ✓ Dive Deep              (TOCTOU investigation, DB-level fix rationale)
  ✓ Deliver Results        (weekend work, 42 DCs, unanticipated obstacles)
  ✓ Insist on High Standards (refused to close without root cause, capacity design)

Dream11 Notification v2 story (Story B):
  ✓ Insist on High Standards  (rejected fire-and-forget, 3 specific deficiencies)
  ✓ Deliver Results           (scoped right, shipped on time, engagement metric)
  ✓ Bias for Action           (moved without full scale data, deferred SPECIFIC_USER)
  ✓ Dive Deep                 (failure mode catalog, each design maps to a failure)

Walmart Kafka Outbox story (Story E):
  ✓ Insist on High Standards  (refused best-effort, designed atomic outbox pattern)
  ✓ Deliver Results           (5M+ messages/day, cross-team dependency unblocked)
  ✓ Bias for Action           (nobody owned it, stepped in, moved fast)
  ✓ Dive Deep                 (root cause was NOT Kafka broker — it was dual-write gap)

Walmart DB Purge story (Story F):
  ✓ Dive Deep                 (strongest — round-trip overhead vs. batch size confusion)
  ✓ Insist on High Standards  (refused to tune around root cause)
  ✓ Deliver Results           (reversed degrading system before it became an incident)
  ✓ Bias for Action           (caught trend early, acted before assigned)
```

---

## STEP 7: Numbers to Know

| Fact | Number |
|---|---|
| DCs affected by Walmart migration | 43 total, 1 first live |
| Walmart race condition fix timeline | Days from pager to deployed fix |
| Dream11 Clubs large club size | ~40,000 members |
| Offset at time of CleverTap 429 | 39,000 (resumed from there, 0 duplicates) |
| Dream11 notification latency (max) | ~30 seconds (cron cycle) |
| GraphQL optimization | 8-10 sequential calls → 2 parallel calls |
| Dream11 Moderation concurrency | 3 concurrent LLM calls (semaphore) |
| Dream11 Content moderation skip conditions | 7 |
| Walmart Kafka messages/day (after Outbox fix) | 5M+ reliably delivered |
| Old programmatic purge capacity | ~150K records per run |
| Purge improvement | Deletion rate exceeded creation rate (DB stabilized) |
