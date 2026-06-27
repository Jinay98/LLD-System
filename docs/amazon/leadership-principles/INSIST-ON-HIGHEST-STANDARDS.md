# LP: Insist on Highest Standards
## Tech 1 — System Design (LLD) Round

---

## What Amazon Is Really Evaluating

> "Leaders have relentlessly high standards — many people may think these are unreasonably high. Leaders are continually raising the bar and drive their teams to deliver high quality products, services, and processes. Leaders ensure that defects do not get sent down the line and that problems are fixed so they stay fixed."

**The scoring rubric (Strength signals they want to see):**
- Sets and commits to goals that are challenging, yet realistic
- Reviews work extensively and offers high-quality feedback
- Communicates and gets agreement on expected standards
- **Builds systems that are scalable and serve customer needs**
- Continually tries to improve processes

**Concern signals (what makes them mark you down):**
- Emphasizes speed over quality for most projects
- Fails to recognize the long-term impact of lower standards
- Accepts quick solutions to problems with minimal follow-up
- Creates or accepts goals that are too easily achievable

---

## Primary Story: Dream11 Notification System v2

### The STAR Framework for This LP

**SITUATION** (set the scene — 30 seconds)
> "At Dream11, we had built Clubs and Threads — WhatsApp-group-like communities and a Twitter-like engagement layer for fantasy sports players. The product team needed notifications to drive engagement: when a contest started in a club, when someone posted, etc. The initial implementation — v1 — was a fire-and-forget call to CleverTap's endpoint inserted inline into business logic across multiple services."

**TASK** (what was your mandate — 15 seconds)
> "My task was to design and implement the notification delivery system properly — one that the team could rely on as we scaled Clubs and added more event types."

**ACTION — this is where you spend 60-70% of time**

*Frame it around raising the standard:*

> "I was unsatisfied with v1 for three specific reasons, each of which I knew would become a production problem at scale — and I made sure the team understood this before we went further.

> **First — no observability.** With fire-and-forget, we had no way to answer: 'Did this club's contest notification actually go out? How many members received it?' At scale, you can't operate a notification system you can't see into. So I designed the `notification_events` table as the single source of truth — every notification event gets a row with a UUID `message_id`, status lifecycle (PENDING → IN_PROGRESS → SUCCESSFUL/FAILED), retry count, failure reason, and `last_successful_offset`.

> **Second — no durability.** If CleverTap returned a 429 or timed out mid-way through a 40,000-member club, v1 would have retried from member zero — re-sending 39,999 push notifications. I designed `last_successful_offset`: after each page of 1,000 members is successfully notified, we checkpoint the offset in the DB. A retry resumes exactly where we left off. I validated this in production: our first large club had 40,000 members. We hit a CleverTap 429 after 39,000 notifications. The retry resumed at offset 39,000. Zero duplicates for those 39,000 members.

> **Third — no extensibility.** v1 hardcoded CleverTap's API URL in business logic. Every new event type meant touching 3-4 services. I designed an internal provider-agnostic endpoint `/v1/notifications/send` — CleverTap is an implementation detail behind it. I also made `event_type` a VARCHAR not an ENUM — adding a new notification type requires zero DB migrations. I separated the publisher cron (30s interval for freshness) from the retry cron (60s interval with backoff) because mixing urgent fresh delivery with slow backoff retry in one cron is a design defect waiting to happen.

> I raised this with the team as a quality gate: we would not ship the Clubs feature with v1. I got alignment from tech lead and product before writing a line of code."

**RESULT** (close strong — 20 seconds)
> "We shipped v2 with Clubs. The notification system has run without any duplicate notifications since launch. When CleverTap goes down, events queue in our DB with full status visibility — we've had 3 retry scenarios since launch, all recovered cleanly. Adding new event types now takes one constant + one handler implementation — no DB migration, no multi-service change. The system served as the pattern for how we'd build all future event-driven pipelines at Dream11."

---

## Backup Story 1: Walmart Double-Slotting Fix

If the interviewer says something like *"tell me about a time you improved quality on something that was already working,"* pivot to the Walmart race condition story but frame it around standards:

**Key framing points:**
- The quick fix was: manually change the slot, done. That would have satisfied most engineers.
- I refused to stop there. A race condition in a system with intermittent behavior, no logs, and 42 more DCs about to migrate meant this WOULD happen again at scale.
- I didn't accept the temporary fix. I added structured logs, traced the root cause, and designed a solution (unique-key constraint + capacity counter) that *also* anticipated future requirements (slots with capacity > 1).
- "Problems are fixed so they stay fixed" — I worked through the weekend to close this before the next DC onboarded.

---

## Backup Story 2: Walmart Inventory Kafka Outbox (Story E)

Use this when the question is about *"improving reliability of a system,"* *"refusing to accept defects,"* or *"insisting on a higher bar when others might have settled."*

**Situation:**
> "At Walmart, the inventory service was one of the most critical services in the ecosystem — it was the source of truth for all inventory state and published Kafka messages to downstream services like pricing, fulfillment, and recommendations. But intermittently, messages weren't getting delivered. Sometimes there was a mismatch. Downstream services would error out, and the root cause was traced back to lost or dropped Kafka events."

**Why this is a standards story:**
The easy answer was: add retry logic on the publisher. That's what most teams would do — patch the publish call, add a try-catch, maybe an SQS fallback. I could have shipped that in a day.

Instead I identified the fundamental design flaw: **we were making two separate writes — one to the DB and one to Kafka — with no atomicity guarantee between them.** If the application crashed between the DB commit and the Kafka publish, the event was lost permanently. Retry logic doesn't solve that — it only helps if the app is still running. The standard I refused to accept: "best-effort delivery for a core inventory event stream."

**Action:**
> "I designed and implemented the Outbox Pattern. During every business transaction in the inventory service, the same transaction that writes to the inventory table also writes an event row to an outbox table. This is atomic — either both commit or neither does. I then built a new dedicated component called the Rapid Re-layer, whose sole responsibility was to read from this outbox table and publish to Kafka. Separating this responsibility meant the publish side could be independently scaled, monitored, and retried without coupling it to the business transaction.

> I also made the outbox table extensible: it was schema-driven for event type and payload, so adding a new downstream consumer required zero changes to the outbox infrastructure."

**Result:**
> "The inventory service now reliably delivers 5 million-plus Kafka messages every day to all its downstream services. The downstream service errors caused by missed events went to zero. The Rapid Re-layer became the standard pattern for any new event publishing needs in the inventory domain."

---

## Backup Story 3: Walmart DB Purge with Stored Procedures (Story F)

Use this when the question is about *"continuously improving processes"* or *"refusing to accept degrading performance."*

**Situation:**
> "In the inventory service at Walmart, record volume was extremely high — new records were being created faster than the purge process could clean up old ones. The existing purge was a programmatic job: application code would select old records, delete them in batches, sleep, repeat. At its best, it could purge about 150,000 records per run. But the rate of new record creation far exceeded that. The DB was growing without bound, and query performance on the inventory table was degrading as a result."

**Why this is a standards story:**
The existing approach was "working" — it was deleting records, just not fast enough. Many engineers would have tuned the batch size or the sleep interval and moved on. I looked at the architecture and saw a structural inefficiency: every deletion was an application-level round trip. The application had to:
1. Open a connection
2. Send a DELETE query to the DB over the network
3. Wait for the DB to execute it
4. Receive the response
5. Repeat

This overhead multiplied across millions of records is enormous. I refused to tune around the real problem.

**Action:**
> "I moved the purge logic into stored procedures. A stored procedure is pre-compiled and stored directly in the database engine. When called, there is no network round trip per batch — the DB executes the logic locally. This meant we could delete orders of magnitude more records per unit time. The deletion rate exceeded the creation rate for the first time, and the DB size stabilized."

**Result:**
> "The DB started shrinking. Query performance on the inventory table recovered. The stored procedure approach became the pattern for all high-volume cleanup jobs in the inventory service."

---

## Likely Questions — and How to Answer Them

### Q1: "Tell me about a time you were unsatisfied with the way things were. What did you do to change it?"

**Use:** Dream11 Notification v2 — primary story above.

**Key points to hit:**
- Be specific about WHAT was wrong (not vague "it wasn't good enough")
- Show you got ALIGNMENT — you didn't just unilaterally refactor
- Show the OUTCOME was durable, not a one-time fix

**Do NOT say:**
- "I just wanted to do it the right way" (too vague — be specific)
- "My manager asked me to improve it" (ownership must come from YOU)
- "I refactored the code" (say what you improved and why it mattered)

---

### Q2: "Tell me about a time you had to make a decision between standards and delivery. What tradeoffs did you make?"

**Use:** Dream11 Notification v2 — the SPECIFIC_USER scope tradeoff.

**Script:**
> "When I designed the v2 notification system, there were event types like 'member joined' or 'member removed' that only needed to notify a single specific user — not the whole club. I called this SPECIFIC_USER scope. I had the architecture designed for it. But we had limited time before the Clubs feature launch, and the club-wide notifications (ALL_MEMBERS scope) were the core use case that drove engagement.

> I made the explicit call to defer SPECIFIC_USER scope to v3. I documented it as deferred — not dropped — in the design doc, along with the reasoning: the table schema and the scope enum already accommodated it. The code contract was already written to be extensible. This wasn't a compromise of quality — it was a prioritization of which quality work went first. We shipped on time, the core system was solid, and SPECIFIC_USER was a configuration change away from being enabled."

**Key points:**
- Show you CONSCIOUSLY deferred (not overlooked)
- Show the extensibility was already baked in
- Show you documented it

---

### Q3: "Tell me about a time when you worked to improve quality on a product that was already getting good customer feedback."

**Use:** Dream11 GraphQL Rewards Optimization.

**Script:**
> "Our rewards shop in Dream11 was functioning well — users could browse and redeem rewards. But when I looked at the API calls underlying it, I saw 8 to 10 sequential backend calls happening per page load. The product was working fine from a user perspective. But I knew: this was fragile, latency would grow as we added sections, and the architecture made it impossible to add new reward sections without cascading changes.

> I redesigned the backend to accept a `sectionIds` parameter, enabling the frontend to request exactly which sections it needed. Then I parallelized the backend calls using Promise.all. The result: 8-10 sequential BE calls collapsed to 2 parallel calls. This isn't a vanity optimization — it's the difference between a system that scales and one that doesn't."

---

## What NOT to Say (For This LP)

| Don't say | Say instead |
|---|---|
| "I just wanted to do it properly" | Explain specifically what was wrong and the impact of not fixing it |
| "The team decided to..." | Own your contribution — what did YOU advocate for, YOU design, YOU push for? |
| "We were moving fast so we had to..." | Show how you balanced speed AND quality, not one at the expense of the other |
| "It was a good enough solution" | Never frame your solution as "good enough" — show why it was the right solution |
| "I got it done" | Quantify: 0 duplicates, 40K members, 43 DCs, etc. |

---

## Amazon Rubric Self-Check

Before you walk out, make sure your story ticked these boxes:

- [ ] I described a standard I was NOT willing to lower
- [ ] I explained why the lower standard would cause a real problem (not a hypothetical)
- [ ] I showed I got alignment/agreement from others (not a solo hero move)
- [ ] My solution was designed to stay fixed, not patched temporarily
- [ ] I quantified the outcome
