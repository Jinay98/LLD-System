# Documentation Template for New Examples

Use this template to document other LLD examples (DocumentEditor, StrategyPattern, etc.)

---

## File Structure

```
your-example/docs/
├── README.md                      # Main documentation (required)
├── PATTERNS_USED.md              # Pattern explanations (if applicable)
├── COMMON_MISTAKES.md            # Anti-patterns and errors (recommended)
├── THOUGHT_PROCESS.md            # Step-by-step design thinking (optional)
└── EXTENSIONS.md                 # How to extend the system (optional)
```

---

## What Each File Should Contain

### 1. README.md (Main Document)

**Purpose:** Complete overview of the system design

**Sections:**
- [ ] Overview (What is this system?)
- [ ] Design Thought Process (How did you think about design?)
  - [ ] Understand the Problem (User flow)
  - [ ] Identify Models (Domain entities)
  - [ ] Identify Managers (Operations)
  - [ ] Identify Patterns (Factories, Strategies, etc.)
- [ ] Architecture Diagram (ASCII diagram of components)
- [ ] Key Design Decisions & Why (Each decision explained)
- [ ] Data Flow (How data moves through system)
- [ ] Class Responsibilities (Table of who does what)
- [ ] How to Extend (Adding new features)
- [ ] Key Takeaways (Interview tips)
- [ ] Running the System (How to execute)

**Length:** 2-3 pages

---

### 2. PATTERNS_USED.md (Pattern Reference)

**Purpose:** Detailed explanation of each design pattern

**For Each Pattern:**
- [ ] Name & Purpose
- [ ] Where Used (Which classes)
- [ ] Pattern Structure (Code example)
- [ ] Why This Pattern (Pros/Cons)
- [ ] When to Use (Decision criteria)
- [ ] With/Without comparison (Bad way vs Good way)

**Length:** 2-4 pages

---

### 3. COMMON_MISTAKES.md (Anti-Patterns)

**Purpose:** Learn what NOT to do

**For Each Mistake:**
- [ ] The Mistake (What developers usually do wrong)
- [ ] Why It's Wrong (Consequences)
- [ ] The Right Way (Correct approach)
- [ ] Real-World Impact (What happens in production)

**Length:** 2-3 pages

---

### 4. THOUGHT_PROCESS.md (Optional - Design Thinking)

**Purpose:** Show how you approached the design problem

**Sections:**
- [ ] Initial Requirements (What was asked)
- [ ] Problem Analysis (What did you understand)
- [ ] Design Iterations (How did design evolve)
- [ ] Trade-offs (What decisions were hard)
- [ ] Final Design (What you settled on)

**Length:** 1-2 pages

---

### 5. EXTENSIONS.md (Optional - Future Features)

**Purpose:** How to extend system with new features

**Format:**
```
### Feature: [Name]

**How to add:**
1. [Step 1]
2. [Step 2]
3. [Step 3]

**Example:**
[Code example]

**Files to change:**
- [File 1]
- [File 2]
```

**Examples:**
- Add new payment method
- Add user authentication
- Add order status tracking
- Add rating system
- etc.

---

## Writing Tips

### ✅ Do's

- **Be specific** - "Add CartManager class responsible for cart operations"
- **Show code** - Include real code snippets from your implementation
- **Explain why** - Not just "what" but "why" you chose this design
- **Use diagrams** - ASCII art diagrams help visualize architecture
- **Link files** - Reference actual Java files in your codebase
- **Include examples** - Show how to use the system
- **Be beginner-friendly** - Assume reader is new to LLD

### ❌ Don'ts

- **Don't be vague** - "The system manages data" (too generic)
- **Don't skip the why** - "We used Singleton" (why?)
- **Don't over-engineer** - Keep it simple and practical
- **Don't bury insights** - Put key points upfront
- **Don't use jargon** - Explain "Singleton" means "one instance"

---

## Structure for Interview Prep

When studying with these docs:

1. **Start with README.md**
   - Understand the complete picture
   - See how everything connects

2. **Read PATTERNS_USED.md**
   - Learn when to use each pattern
   - See why certain choices were made

3. **Study COMMON_MISTAKES.md**
   - Learn what NOT to do
   - Avoid pitfalls

4. **Write it out by hand**
   - Draw architecture diagrams
   - Write out class relationships
   - Practice explaining to someone else

5. **Practice explaining**
   - "Why is CartManager not a Singleton?"
   - "What would happen if Order had too many responsibilities?"
   - "How would you add a new payment method?"

---

## Quick Checklist: Is Your Documentation Complete?

```
[ ] README.md explains the thought process
[ ] Architecture diagram is clear
[ ] Each design decision has reasoning
[ ] PATTERNS_USED.md explains why patterns were chosen
[ ] COMMON_MISTAKES.md lists what to avoid
[ ] Code examples are real (from actual implementation)
[ ] Someone new to LLD could understand it
[ ] You could explain this in an interview
[ ] Data flow is traced from start to end
[ ] Extensions section shows how to add features
```

---

## Example: Converting Zomato to This Template

```
README.md:
  ✅ Overview
  ✅ Design Thought Process (4 steps)
  ✅ Architecture (ASCII diagram)
  ✅ Key Design Decisions (7 decisions with explanations)
  ✅ Data Flow (6 steps traced)
  ✅ Class Responsibilities (table)
  ✅ How to Extend
  ✅ Key Takeaways
  ✅ Running the System

PATTERNS_USED.md:
  ✅ Singleton (RestaurantManager, OrderManager)
  ✅ Manager (CartManager, RestaurantManager, OrderManager)
  ✅ Factory (OrderFactory, PaymentFactory, NotificationFactory)
  ✅ Strategy (IPaymentStrategy, INotificationStrategy)
  ✅ Orchestrator (OrderingService)
  ✅ Composition (User owns CartManager, Cart owns CartItems)

COMMON_MISTAKES.md:
  ✅ God Object (too much responsibility)
  ✅ Everything is Singleton (per-user state)
  ✅ Tight Coupling (too many dependencies)
  ✅ No Interfaces (hard to extend)
  ✅ Mixing Data Access & Business Logic
  ✅ No Clear Data Flow
  ✅ Ignoring Edge Cases
  ✅ Unclear Class Responsibilities
```

---

## For Your Next Example

Pick another pattern/system from your LLD folder and document it:

1. **Copy this template to the new example**
2. **Follow the structure above**
3. **Use Zomato as reference** for quality/completeness
4. **Focus on the unique patterns** in that example
5. **Explain trade-offs** specific to that design

This becomes your **interview preparation library** - a reference guide showing you understand LLD deeply.

