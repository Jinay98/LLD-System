# LP: Deliver Results
## Tech 1 — System Design (LLD) Round

---

## What Amazon Is Really Evaluating

> "Leaders focus on the key inputs for their business and deliver them with the right quality and in a timely fashion. Despite setbacks, they rise to the occasion and never settle."

**The scoring rubric (Strength signals):**
- Meets deadlines and expectations with products that are well-executed and high-quality
- Focuses on delivering the most important products
- **Overcomes and persists in the face of obstacles**
- Communicates regularly about the status of projects

**Concern signals (what marks you down):**
- Fails to deliver on required commitments
- Makes excuses for why things cannot be done before a deadline
- Delivers projects late or with missing requirements
- Settles for work products that do not meet expectations

---

## Primary Story: Walmart Double-Slotting (The Pager Incident)

### Why This Story Fits Deliver Results

The Walmart story is perfect for this LP because:
1. You had a **hard constraint** — the first RDC migration had to work. 42 more DCs were watching.
2. You faced **unanticipated obstacles** — no logs, intermittent bug, running production system, couldn't take it down.
3. You **didn't make excuses** — you went in on weekends, added logs, traced root cause, shipped a fix.
4. The **outcome was durable** — 42 more DCs rolled out without this bug.

---

### The STAR Framework for This LP

**SITUATION** (30 seconds)
> "At Walmart, I was on the slotting team — we built the system that assigns physical warehouse locations to inbound freight. Think of it like a seat reservation system for a warehouse. The system ran a smart algorithm to match freight types to optimal zones: medicines in one zone, perishables in another.

> We were in the middle of a major migration — moving 43 distribution centers (DCs) from a legacy codebase to a new one called Atlas. The first DC had just gone live. This was a high-stakes migration: if the first DC had production issues, the entire rollout plan for the remaining 42 would be called into question."

**TASK** (15 seconds)
> "I was on the on-call pager rotation. One evening I got paged — warehouse associates at the first RDC reported that freight was being assigned to slots that were already occupied. The same slot was being given to two different freight packages simultaneously."

**ACTION — (60-70% of time)**

*Frame it around persisting through obstacles and delivering despite setbacks:*

> "I immediately faced my first obstacle: I had to unblock the associates on the warehouse floor without taking down a running production system. I manually corrected the slot assignments in the database — not ideal, but the warehouse couldn't wait. This was the right call to unblock people quickly.

> But then came the harder obstacle: the bug was intermittent and there were no useful logs in the new Atlas codebase for this flow. I had nothing to trace it with. I didn't close the pager ticket and call it done — I knew an intermittent bug with no reproduction path was a ticking time bomb, especially with 42 more DCs about to onboard.

> I added structured logs to the slotting flow — the freight request, the algorithm recommendation, the slot assignment — and deployed with my teammate's code review. This is where I hit obstacle number two: you can't reproduce an intermittent race condition on demand. I had to wait for it to surface again with logs in place.

> When it surfaced again, the logs told the story clearly: two concurrent requests for different freight items were arriving at the same millisecond. The algorithm recommended the same slot for both because it evaluated slot availability based on a snapshot that hadn't yet accounted for the first write. Classic race condition.

> Now I had the root cause. I designed the fix: a DB-level unique key constraint on (slot_id, freight_id, capacity) — not an application-level lock which could still race, but a database constraint that is atomic. I also introduced a capacity counter per slot. Before this, all slots had a capacity of one and it was implicit. I made it explicit — a slot could have capacity N, and each assignment decremented the counter atomically. If two requests hit the same slot simultaneously, only one would succeed; the other would get a unique key violation and retry with a new slot recommendation.

> I did this work over the weekend to make sure it was merged and deployed before the second DC went live."

**RESULT** (20 seconds)
> "The race condition was fixed before the second DC migration. All 42 remaining DCs migrated to Atlas without hitting this issue. The capacity counter design turned out to be prescient — within a few months, the warehouse introduced larger slots that could hold multiple freight packages, and the system handled it without any schema changes. If I had just patched the immediate symptom, we would have had a bigger problem at scale."

---

## Backup Story 1: Dream11 Notification System v2

If the question is about *delivering under a tight deadline* specifically, use the Dream11 story with this framing:

**The pressure context:**
- Clubs and Threads was a new product being aggressively pushed. Engagement was lagging. Product wanted notifications live before the next major cricket season.
- Timeline was tight — a few weeks to design and ship the full notification pipeline.
- We had a dependency on CleverTap's external API with rate limits we couldn't control.

**What you delivered:**
> "I scoped v2 deliberately. There were three notification scopes in the design: ALL_MEMBERS, ALL_MEMBERS_EXCLUDING_ACTOR, and SPECIFIC_USER. SPECIFIC_USER required different plumbing. I made the explicit call to defer it — not cut it, defer it. The schema and code contracts were already extensible for it. This let us ship on time with the high-priority scenarios fully covered.

> We shipped the notification system before the cricket season. The first match day with Clubs notifications live showed a measurable spike in engagement in the product metrics. The deferred scope was enabled in a follow-up sprint with no architectural changes — just a new handler."

---

## Backup Story 2: Walmart Inventory Kafka Outbox (Story E)

Use this when the question is about *"delivering reliability to a system with a hard business dependency"* or *"overcoming a systemic obstacle that was impacting other teams."*

**Situation:**
> "At Walmart, our inventory service was the most critical upstream service in the ecosystem — pricing, fulfillment, recommendations all depended on its Kafka event stream. But the system was intermittently dropping or mismatching messages. Downstream teams were filing incidents against us. The business impact was real: fulfillment picking the wrong items, pricing out of sync with inventory."

**Why this is a Deliver Results story:**
- There was real downstream impact — other teams were blocked by our failures
- I stepped in on a problem that was systemic, not just isolated to one team
- The obstacle was architectural (dual-write atomicity gap), not just a bug

**Action + Result:**
> "I designed and built the Outbox Pattern — dual-write in the same DB transaction (inventory table + outbox table), then a dedicated Rapid Re-layer component that reads from the outbox and publishes to Kafka. This separated the responsibility: the business transaction guarantees the event is durably written; the Rapid Re-layer guarantees it's published. The result: 5 million-plus Kafka messages delivered daily, downstream service errors from missed events dropped to zero."

---

## Backup Story 3: Walmart DB Purge (Story F)

Use this when the question is about *"reversing a degrading situation"* or *"delivering a fix when the system is heading toward failure."*

**Situation:**
> "Our inventory DB was growing out of control. New records were arriving faster than our purge process could delete old ones — the programmatic purge was capped at about 150,000 records per run, but we were creating far more than that. Query performance was degrading. Left unchecked, we'd have had table scan performance on our most-queried table."

**Action + Result:**
> "I identified the root cause: every deletion was an application round-trip — connect, send query, wait, receive, repeat. I moved the purge to stored procedures — pre-compiled in the DB engine, no per-batch round trip. The deletion rate jumped dramatically, exceeded the creation rate, and the DB size stabilized. Query performance on the inventory table recovered."

---


## Likely Questions — and How to Answer Them

### Q1: "Tell me about a time when you had significant, unanticipated obstacles to overcome in achieving a key goal."

**Use:** Walmart pager story — primary story above.

**Key points to hit:**
- Name the obstacles specifically: no logs, running production system, intermittent bug
- Show how you worked around each obstacle (manual fix → logs → wait for reproduction → fix root cause)
- End with the scale impact (42 DCs)

**Do NOT say:**
- "It was just a bug and I fixed it" (undersells the obstacles)
- "My team helped me" (fine to mention, but own your contribution clearly)
- Anything that sounds like luck ("luckily it reproduced with the new logs")

---

### Q2: "Give me an example of a time when you delivered an important project under a tight deadline. What sacrifices did you make?"

**Use:** Dream11 Notification v2 OR Walmart weekend work.

**For Dream11:**
> "The sacrifice was deferring SPECIFIC_USER scope. It was in the design — it was right — but we shipped without it. That was a deliberate trade-off I made and communicated. The sacrifice was not quality; the sacrifice was completeness. The shipped system was high quality. It just didn't cover 100% of the planned scope. That's different from shipping something that doesn't work."

**For Walmart:**
> "The sacrifice was my weekend. I came in on Saturday and Sunday to get the fix merged and deployed before the second DC went live. There was no other option — waiting until Monday would have meant the second DC migrated with the bug still in place."

---

### Q3: "Tell me about a time when you or your team were more than halfway to a goal when you realized it may not be the right goal."

**Use:** Dream11 Notification — realizing fire-and-forget was the wrong goal.

**Script:**
> "We were partway through shipping Clubs when I realized the notification approach — fire and forget — was not going to work at the scale we were targeting. We had already built several notification call sites. The instinct was to keep going — we were committed to the approach. But I stopped and laid out the three failure modes: no observability, no durability, and no extensibility. We were building toward a goal that would make the product unreliable. I got alignment to redesign the notification architecture before we went live. The delay was measured in days; the payoff was a notification system that's been production-stable since."

---

## What NOT to Say (For This LP)

| Don't say | Say instead |
|---|---|
| "I worked hard" | Describe the specific obstacles you overcame and how |
| "The team delivered" | Be specific about YOUR contribution to unblocking the team |
| "We almost missed the deadline" | Show how you identified the risk early and course-corrected |
| "It was a learning experience" | Fine to say retrospectively, but lead with the result |
| "We delivered eventually" | Lead with the result: what was delivered, when, and what it enabled |

---

## Amazon Rubric Self-Check

Before you walk out, make sure your story ticked these boxes:

- [ ] I named a specific goal or deliverable that was at risk
- [ ] I described the obstacles concretely (not vaguely "it was hard")
- [ ] I showed I persisted — not gave up or escalated for someone else to solve
- [ ] I communicated status (didn't just disappear and hope it worked out)
- [ ] The result had a clear business impact (42 DCs, season launch, engagement metric)
