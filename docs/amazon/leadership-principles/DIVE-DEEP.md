# LP: Dive Deep
## Tech 2 — Coding (DSA) Round

---

## What Amazon Is Really Evaluating

> "Leaders operate at all levels, stay connected to the details, audit frequently, and are skeptical when metrics and anecdotes differ. No task is beneath them."

**The scoring rubric (Strength signals):**
- Stays connected to the details of projects and programs
- Understands how different groups or systems work together
- **Critically evaluates metrics and data**
- Asks good questions that provide clarity to situations
- Steps in and gets work done
- **Investigates and gets details in order to solve a problem**
- Gathers information to solve a problem, even if it's difficult or time-consuming

**Concern signals (what marks you down):**
- Only holds surface-level understanding of metrics and data
- Does not question assumptions
- Does not understand how different groups or systems work together
- Unable to step in and get work done

---

## CRITICAL INSIGHT: What "Dive Deep" means in a DSA Round

In a coding interview, Dive Deep manifests as:
1. **You trace your algorithm step by step** on an example (you know what's happening internally)
2. **You question your own assumptions** before the interviewer does
3. **You identify edge cases proactively** — not just the happy path
4. **You analyze time and space complexity** with precision, not approximation
5. **You know WHY your solution works**, not just that it does

Amazon is checking: can you go below the surface? Can you look at your own code like a skeptic?

---

## Primary Story: Walmart — Tracing the Race Condition Root Cause

### Why This Story Fits Dive Deep

- You didn't accept the surface explanation ("it's a bug, let's patch it")
- You added logs to get data, then waited for the data to surface the root cause
- You traced the exact execution sequence: two concurrent threads, same slot recommendation, read-before-write gap
- You questioned assumptions: why did this only happen now? (New Atlas codebase hadn't seen this load before)
- You went deep enough to design a fix that also handled a future requirement (variable slot capacity)

### The STAR Framework for This LP

**SITUATION** (30 seconds)
> "At Walmart, after getting paged for a double-slotting incident — two freight packages assigned to the same warehouse location — I needed to understand why this happened. On the surface, it looked like a simple bug: wrong slot was assigned. But the intermittent nature and the timing of when it first appeared told me the surface explanation wasn't the full story."

**TASK** (15 seconds)
> "I needed to root-cause the issue precisely enough to fix it permanently — not patch it. An intermittent race condition that I couldn't reproduce on demand required me to gather real data from production."

**ACTION — (60-70% of time)**

*Frame it around investigation, data gathering, and questioning assumptions:*

> "My first instinct was to question why the bug appeared NOW. The system had been running — but only at one DC, and only on the new Atlas codebase. The legacy system had handled slotting fine. Why was Atlas different? I didn't assume it was an Atlas bug. I considered: maybe this is a load pattern the legacy system never saw at this DC. That question shaped my investigation.

> I added structured logs to trace the full lifecycle of a slot assignment: incoming request, algorithm evaluation, available slots snapshot, slot recommendation, assignment write. I deployed this with my teammate's review. I was methodical about WHAT I logged — I needed to see the snapshot and the write as two discrete events, because my hypothesis was a read-write gap.

> When the bug surfaced with logs in place, I had the data. Here is what the logs showed:
> - Thread A reads slot availability: Slot 7 has capacity 1, currently 0 occupied. Available.
> - Thread B reads slot availability simultaneously: Slot 7 has capacity 1, currently 0 occupied. Available.
> - Thread A recommends Slot 7.
> - Thread B recommends Slot 7.
> - Thread A writes: Slot 7 is now occupied.
> - Thread B writes: Slot 7 is now occupied. — Double assignment.

> That's a classic TOCTOU — time of check to time of use — race condition. The algorithm read the state, computed a recommendation, then wrote — but between read and write, another thread read the same state and made the same recommendation.

> Now I questioned the fix assumptions. The obvious fix was an application-level lock. But I dug one level deeper: would a distributed lock work if we scaled horizontally? Would a mutex work across multiple service instances? No — application-level locks don't span JVM instances. The right level to enforce atomicity was the database.

> I designed a unique key constraint at the DB level on (slot_id, freight_id, capacity). This is atomic — the DB enforces it regardless of how many application instances are running. I also introduced an explicit capacity counter per slot, which made the algorithm's slot selection logic read the counter atomically rather than inferring from a snapshot. A concurrent write would get a constraint violation and retry with a new slot.

> I also questioned: why capacity 1? All slots had implicit capacity of 1. If the business ever needed larger slots, we'd be back here redesigning. I made capacity explicit and dynamic — a table column, not a constant."

**RESULT** (20 seconds)
> "The fix was precise and level-appropriate. Within weeks of shipping, the capacity model proved its value — the warehouse introduced larger pallet slots that could hold 2-3 freight packages. No code change was needed; only a data change. 42 subsequent DCs ran without this incident."

---

## How to Apply This to a DSA Coding Question

In a coding round, Dive Deep looks like this:

### During problem understanding:
> "Before I start, let me clarify my assumptions. You said 'sorted array' — is it sorted ascending or can it be descending? And can there be duplicates?"

### During solution coding:
> "I'm using a HashMap here. The assumption is that key lookup is O(1) average. In the worst case with hash collisions it degrades to O(n), but for typical inputs this is O(1)."

### During tracing:
> "Let me trace through this example step by step. Array is [2, 7, 11, 15], target = 9. At index 0, value is 2, complement is 7. Map is empty, so I store 2 → 0. At index 1, value is 7, complement is 2. Map has 2 → 0. Return [0, 1]. That matches the expected output."

### During complexity analysis:
> "Time complexity: O(n) — one pass through the array. Space complexity: O(n) — in the worst case the map stores all n elements before finding the pair."

### Questioning your own solution:
> "One edge case I should check: what if the target is the same element used twice? For example, array [3, 3], target 6. My current code would store 3 → 0, then at index 1 find 3 in the map — that returns [0, 1] which is correct because indices are different. That works."

---

## Backup Story: Dream11 — Diagnosing Notification Failure Modes

If the question is: *"Tell me about a complex problem where you had to dig into details to figure it out."*

**Script:**
> "When I was designing the v2 notification system at Dream11, I didn't start with the architecture. I started by cataloguing every way the v1 system could fail — and there were more than I initially thought.

> The obvious failure: CleverTap API returns a 500 and the notification is lost. Simple to handle.

> The non-obvious failures: What happens if the consumer processes member 30,000 of 40,000 and then the JVM crashes? The SQS message is redelivered. The consumer starts from member 0. 30,000 members get duplicate push notifications. That's not a visible bug in development — it only surfaces under specific failure conditions at scale.

> What if CleverTap rate-limits us with a 429? We can't just drop the message. We can't retry immediately. We need backoff — but how much? And we need to track WHERE we were when the rate limit hit so we don't resend to already-notified members.

> What if two publisher cron instances run simultaneously due to clock drift? Both read the same PENDING row. Both publish to SQS. The consumer processes it twice. The idempotency guard in the consumer (check for SUCCESSFUL status before processing) catches this — but only if it reads the DB before paginating.

> I traced each of these paths explicitly before writing the schema. That's why `last_successful_offset` exists. That's why the consumer checks DB status when isRetry=true. That's why the SQS message is acknowledged even on failure — because SQS redelivery doesn't give us the backoff control we need. Each design decision maps to a specific failure mode I analyzed."

---

## Likely Questions — and How to Answer Them

### Q1: "Tell me about a situation that required you to dig deep to get to the root cause."

**Use:** Walmart race condition story — primary story above.

**Key points:**
- Name the specific investigative steps (logs added, hypothesis formed, data gathered, sequence traced)
- Show you questioned assumptions ("why NOW?", "why does application-level lock not work?")
- Show the depth of the fix exceeded the depth of the surface symptom

---

### Q2: "Tell me about a complex problem on your team you had to dig into the details to figure out."

**Use:** Dream11 — failure mode analysis OR Walmart race condition.

**Do NOT say:**
- "I read the code" (too vague — what did you look at specifically?)
- "I figured it out eventually" (show the investigative METHOD, not just the outcome)
- "I Googled the solution" (own the analysis — even if you referenced docs, the diagnosis was yours)

---

### Q3: "Walk me through a big problem you helped solve. How did you become aware of it? What information was missing?"

**Use:** Walmart — "I became aware via pager. The missing information was: no logs, intermittent nature, couldn't reproduce on demand. Here's how I filled the gap..."

**Alternative use — Story E (Kafka Outbox):**
> "I became aware via downstream team incidents. The missing information was: we had no visibility into whether individual Kafka messages had been published or not — the publish was fire-and-forget. I had to instrument and trace the lifecycle to understand the gap. The gap was between DB commit and Kafka publish: no atomicity guarantee. Once I understood the failure boundary precisely, the fix — Outbox Pattern — was the logical answer."

---

## Additional Backup Story: Walmart Kafka Dual-Write Root Cause (Story E)

Use this when the question is *"tell me about a complex system you had to understand deeply"* or *"how you identified a non-obvious root cause in a cross-team problem."*

**Situation:**
> "At Walmart, our inventory service was intermittently dropping Kafka messages to downstream services. The surface symptom was downstream teams filing incidents: fulfillment was using stale inventory data, pricing was out of sync. It looked like a Kafka reliability issue — maybe consumer lag, maybe broker flakiness."

**Why this is a Dive Deep story — the root cause was NOT what it looked like:**

> "My first hypothesis: Kafka broker issue. I checked broker metrics — no anomalies. Consumer lag looked normal. The intermittency pattern didn't correlate with Kafka load. So I dug deeper into the publish side.

> I traced the exact code path: the inventory service would write to the inventory DB, then call the Kafka producer. These were two separate operations with no transaction boundary spanning them. If the application process crashed — JVM OOM, deployment restart, host failure — after the DB write but before the Kafka publish, the event was permanently lost. The DB had no record that the publish had failed. The Kafka broker had no record that the publish was attempted. There was literally no audit trail.

> This is the dual-write problem: two separate writes (DB + Kafka) with no atomicity between them. Retry logic doesn't solve it — if the process is dead, there's nothing to retry.

> Once I understood the precise failure boundary, the solution was clear: Outbox Pattern. Write to DB and outbox in the same transaction (atomic). Separate component — Rapid Re-layer — reads the outbox and publishes to Kafka. The outbox is the audit trail. If publish fails, the row stays in the outbox for retry."

**Result:**
> "5M+ Kafka messages/day delivered reliably. Downstream incident rate dropped to zero for inventory-related events. The Rapid Re-layer was built to be general-purpose — any new downstream consumer just subscribes to the same Kafka topic."

---

## Additional Backup Story: Walmart DB Purge Root Cause (Story F)

Use this when the question is about *"using data to identify the right problem"* or *"understanding system-level performance issues deeply."*

**Situation:**
> "In the inventory service, we had a purge job that was supposed to keep the DB from growing unbounded. But the DB was growing. Old programmatic purge: ~150K records deleted per run. New records arriving: far more than 150K per run. The purge was losing the race."

**The surface fix vs. the deep root cause:**

The obvious thing to do: increase the batch size. Delete 500K instead of 150K per run. Most engineers would stop there.

I dug into WHY the programmatic approach had a ceiling:
> "Every deletion in the application-level purge was a round trip:
> 1. Application sends DELETE query over network
> 2. DB engine parses, plans, executes
> 3. DB sends result back over network
> 4. Application processes response
> 5. Loop to next batch
>
> Even with fast network, this overhead adds up. At 150K records, you're doing roughly 150 round trips (assuming 1K per batch). Each trip: network latency × 2, query planning overhead, response serialization. You can increase batch size, but you eventually hit connection timeout limits and memory pressure.
>
> The question I asked: why does the DB need to talk to the application to delete its own data? It doesn't. A stored procedure runs entirely inside the DB engine. No round trips. No re-parsing per call. The deletion logic is pre-compiled. The engine executes it locally."

**Result:**
> "Stored procedure-based purge massively outperformed the programmatic approach. Deletion rate exceeded creation rate. DB size stabilized. Query performance on the inventory table — which had been degrading due to table size — recovered. The stored procedure approach became the standard for all high-volume maintenance jobs in the inventory service."

---


## What NOT to Say (For This LP)

| Don't say | Say instead |
|---|---|
| "I looked at the code and found the bug" | Walk through the investigative steps — what did you look at, what did you rule out, what data told you what |
| "I knew what the problem was" | Show curiosity — you questioned your hypothesis before committing to a fix |
| "I used logs to debug it" | Specify what you logged, what the logs showed, and how that led to the root cause |
| "I asked a senior engineer" | Fine to mention collaboration, but own the analysis yourself |
| "The problem was complex but I solved it" | The complexity IS the story — describe what made it hard and how you navigated that specifically |

---

## Amazon Rubric Self-Check

Before you walk out, make sure your story ticked these boxes:

- [ ] I described a problem where the surface symptom was NOT the root cause
- [ ] I named the specific information I gathered and how I gathered it
- [ ] I showed I questioned my own assumptions (not just assumed I was right)
- [ ] I traced the root cause with precision (not just "there was a bug")
- [ ] The fix addressed the root cause, not just the symptom
- [ ] I mentioned how deep the fix went — it was designed to prevent recurrence
