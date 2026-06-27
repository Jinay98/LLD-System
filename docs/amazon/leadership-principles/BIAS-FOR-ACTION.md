# LP: Bias for Action
## Tech 2 — Coding (DSA) Round

---

## What Amazon Is Really Evaluating

> "Speed matters in business. Many decisions and actions are reversible and do not need extensive study. We value calculated risk taking."

**The scoring rubric (Strength signals):**
- Moves quickly on a project, even when some information is lacking
- Understands when to consult with others and when it's not necessary
- **Willing to make small progress toward a solution rather than finding the perfect solution right away**
- Deals with problems quickly so others can make progress
- Carefully considers what is important to get done right away and what can wait

**Concern signals (what marks you down):**
- Moves slower than necessary when faced with incomplete information
- Hesitates longer than necessary when making even small decisions
- Fears making mistakes and doing things that haven't been done before

---

## CRITICAL INSIGHT: What "Bias for Action" means in a DSA Round

In a coding interview, "Bias for Action" does NOT mean writing code blindly.  
It means: **you don't freeze up when you don't immediately know the optimal solution.**

What Amazon wants to see:
1. You verbalize a brute-force approach quickly rather than sitting in silence
2. You move from brute-force to optimized iteratively, not in one giant leap
3. You make a working solution first, then improve — not the reverse
4. You don't wait for the interviewer to guide you when you have enough information to make a move

---

## Primary Story: Walmart Pager Duty — Race Condition Hotfix

### Why This Story Fits Bias for Action

- You were on-call and paged — **you had to make a decision immediately** with incomplete information
- You chose the right first action: manually fix the warehouse associate's issue (unblock them fast)
- You didn't wait for a perfect understanding before acting — you moved
- Then you took calculated follow-up actions: deploy logs (with review), wait for reproduction, then fix

### The STAR Framework for This LP

**SITUATION** (30 seconds)
> "At Walmart, I was on-call when I got paged during a warehouse shift. Associates at our first Atlas-migrated distribution center reported that two freight packages had been assigned to the same physical slot — a double-slotting error. This was during active warehouse operations. Every minute the wrong slot assignment stayed in the system, warehouse staff were blocked from correctly placing freight."

**TASK** (15 seconds)
> "I had to act immediately — but I had incomplete information. The bug was intermittent, there were no useful logs in the new codebase, and I couldn't take the system down. I had to make decisions with what I had."

**ACTION — (60-70% of time)**

*Frame it around speed + calculated decisions:*

> "I made the first decision within minutes of getting paged: I manually corrected the slot assignments in the database. Was this the 'right' engineering solution? No. But it was the right FIRST action — it unblocked the warehouse associates immediately so they could continue operations. I didn't spend 30 minutes trying to understand the root cause first. The associates needed to move freight. I removed that blocker.

> The second decision: do I close the ticket and wait? No — I recognized that an intermittent bug with no logs in a system that was about to expand to 42 more DCs was a high-probability future incident. So I made another fast, calculated decision: I added structured logging and deployed it with a teammate's review. I didn't need a full post-mortem to know we needed observability in this flow. I acted on what I knew.

> The third decision: wait for the bug to reproduce with logs in place. This sounds passive but it was an active choice — I knew I couldn't synthetically reproduce a race condition, and waiting was the right call over guessing at the root cause. Within a few days the logs surfaced the issue: two concurrent requests hitting the same slot recommendation because of a read-before-write race condition.

> From there I designed and shipped the fix: a DB-level unique-key constraint plus a capacity counter. I chose the DB constraint specifically because it's atomic — no application-level mutex could be as reliable. I didn't over-engineer it into a distributed lock or a queue — the constraint was the minimum effective solution."

**RESULT** (20 seconds)
> "Total time from page to permanent fix: a few days — including the wait for log reproduction. The second DC went live without the bug. 42 subsequent DCs rolled out cleanly. The design I chose — DB-level constraint — also turned out to be extensible when slot capacities changed later."

---

## How to Apply This to a DSA Coding Question

When the interviewer gives you a problem, here is your Bias for Action playbook:

### Step 1: Understand, then move — don't freeze
After 1-2 minutes of reading: verbalize what you understand and what approach you'd try first.
> "I think I can brute-force this with a nested loop — that gives me O(n²). Let me code that up first and then we can optimize."

### Step 2: Code the brute-force working solution
Don't wait until you have the O(n log n) insight. Get something working.
> "Here's the brute-force working. Let me trace through the example to verify."

### Step 3: Identify the bottleneck and optimize from there
> "The inner loop is the bottleneck. If I replace this linear scan with a hash map, I get O(n) time and O(n) space. Let me refactor that."

### Step 4: Move to edge cases — don't wait to be asked
> "Let me think about edge cases: empty input, single element, all duplicates, negative numbers..."

**What this shows Amazon:** You treat a DSA problem like a production bug — you make progress toward a working state rather than waiting for the perfect insight before writing a line.

---

## Backup Story: Dream11 Notification v1 → v2 Decision

If the question is: *"Tell me about a time you had to make a decision without having all the information you needed"*:

**Script:**
> "When we were designing the notification system for Dream11's Clubs product, I had to decide how to architect it without knowing exactly what the scale would be. We didn't know how many clubs we'd have, how many members per club, or what notification frequency would look like. I had incomplete data.

> Instead of waiting for scale data we didn't have, I made a set of deliberate decisions based on what I knew: clubs had unbounded membership, CleverTap had rate limits, and we needed retry capability. I designed the architecture around those knowns — the `notification_events` table, checkpointable offset, two-cron separation — and built in extensibility points for unknowns.

> I deferred the SPECIFIC_USER scope explicitly because it required different plumbing and I didn't have enough data on its usage patterns to justify the extra complexity upfront. That was a calculated risk. When the data came in later (single-user notifications were a smaller use case), my deferral was validated."

---

## Likely Questions — and How to Answer Them

### Q1: "Give me an example of a calculated risk you took where speed was critical."

**Use:** Walmart — the manual DB correction as first action.

**Key framing:**
> "The calculated risk was modifying production data manually. The risk was: I could make it worse. The calculation was: the alternative — warehouse associates being blocked for hours while I investigated — was worse. I made a precise, targeted change: correct the slot assignment for the two affected freight packages. Nothing more. I knew exactly what I was changing and why."

**Do NOT say:**
- "I just went for it" (sounds reckless, not calculated)
- "I checked with my manager first" for the immediate unblocking action (you acted — that's the point)

---

### Q2: "Tell me about a time when you worked against tight deadlines and didn't have time to consider all options."

**Use:** Walmart pager duty — time pressure of warehouse operations.

**Key framing:**
- You had warehouse associates blocked in real-time
- You had incomplete information (no logs)
- You made the minimum effective decision first (manual fix)
- You documented and followed up — bias for action doesn't mean no follow-through

---

### Q3: "Tell me about a time you saw an issue that would impact your team and took a proactive approach."

**Primary use:** Walmart — recognizing that an intermittent bug before 42 DC migrations was a systemic risk.

**Key framing:**
> "I could have closed the pager ticket after the manual fix. The immediate incident was resolved. But I saw the structural risk: 42 more DCs were in queue. An intermittent race condition with no logs that was hard to reproduce would almost certainly surface again at scale. I proactively added logging and stayed on the issue until I had a root cause. That's what separated a band-aid from a solution."

**Alternative use — Story E (Kafka Outbox):**
> "I saw downstream teams filing incidents against us but no one on our team had taken ownership of the root cause. The symptom looked like a Kafka problem from the outside, so it was easy to pass the blame. I proactively stepped in, traced the issue to our own publish path, identified the dual-write atomicity gap, and designed the Outbox pattern solution. I didn't wait to be assigned this — I saw the impact on the broader engineering organization and acted."

**Alternative use — Story F (DB Purge):**
> "I noticed our inventory DB query performance was degrading gradually — it wasn't a pager alert, it wasn't an incident. I was looking at query metrics and saw the trend. I traced it to the DB growing faster than the purge could clean up. Rather than waiting for it to become an incident, I investigated, identified the root cause (programmatic purge round-trip overhead), and moved to stored procedures before it became a crisis."

---

## Additional Backup Story: Walmart Kafka Outbox — Proactive Cross-Team Fix (Story E)

Use when: *"Give me an example of when you moved quickly to solve a problem that was affecting others."*

**Script:**
> "At Walmart, downstream services — fulfillment, pricing, recommendations — were filing incidents due to stale or missing inventory data. From the outside it looked like a Kafka reliability issue. Nobody had traced it to a root cause yet.

> I didn't wait to be assigned the investigation. I started tracing the inventory service's publish path immediately. Within a few days I identified the issue: the publish to Kafka and the write to DB were two independent operations with no atomicity between them. If our process died at the wrong moment, the event was silently lost — no audit trail anywhere.

> I moved quickly: I designed the Outbox Pattern (dual-write in same transaction + Rapid Re-layer component) and started building it without waiting for a full cross-team design review cycle. I shared the design with stakeholders in parallel so they could review while I was building. This let us ship faster without sacrificing review quality.

> Result: 5M+ Kafka messages/day, zero downstream incidents from missed events."

---

## Additional Backup Story: Walmart DB Purge — Acting Before Crisis (Story F)

Use when: *"Tell me about a time you identified a risk early and took action before it became a bigger problem."*

**Script:**
> "I was reviewing query performance metrics for the inventory service — not as part of a formal review, just routine monitoring I did on my own. I saw a slow upward trend in query latency on the inventory table. I traced it to DB size growth: our purge job was deleting ~150K records per run but new records were arriving faster. The DB was growing without bound.

> This wasn't a pager yet. It wasn't an incident. Most people wouldn't have caught it at this stage. But I could see where it was going: eventually the inventory table would be large enough that queries start doing full-table scans, and then we'd have a full production incident.

> I moved immediately. I identified the root cause — application-level round-trip overhead per delete batch — and moved the purge to stored procedures. Deletion rate exceeded creation rate. Problem reversed before it became a crisis."

---


## What NOT to Say (For This LP)

| Don't say | Say instead |
|---|---|
| "I took my time to be thorough" | Show that you moved fast AND were thorough — not one or the other |
| "I waited for more information" | Show what minimum information you needed, then moved |
| "I asked my manager what to do" | You can mention consulting teammates, but YOU drove the decision |
| "I eventually figured it out" | Frame actions as deliberate and sequential, not stumbling upon the answer |
| "I didn't want to make a mistake" | Amazon fears risk-aversion more than mistakes. Own that you made calculated moves. |

---

## Amazon Rubric Self-Check

Before you walk out, make sure your story ticked these boxes:

- [ ] I described a situation where I had INCOMPLETE information when I acted
- [ ] My first action was fast, targeted, and specific — not a complete solution
- [ ] I showed I understood the trade-offs of acting quickly (what could go wrong)
- [ ] I showed follow-through — bias for action isn't abandon-and-move-on
- [ ] I named the business impact of moving quickly (warehouse unblocked, 42 DCs, etc.)
