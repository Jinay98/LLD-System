# Zomato LLD Documentation Index

Complete documentation for understanding the Zomato food ordering system design.

---

## 📚 Documentation Files

### 1. **README.md** ⭐ START HERE
**Complete system design explanation**

- Overview of the system
- Step-by-step thought process
- Architecture diagram
- Design decisions with reasoning
- Data flow visualization
- Class responsibilities
- How to extend the system
- Interview tips

**Time to read:** 15-20 minutes

---

### 2. **PATTERNS_USED.md**
**Deep dive into design patterns**

- Singleton Pattern (RestaurantManager, OrderManager)
- Manager Pattern (RestaurantManager, CartManager, OrderManager)
- Factory Pattern (OrderFactory, PaymentFactory, NotificationFactory)
- Strategy Pattern (PaymentStrategy, NotificationStrategy)
- Orchestrator Pattern (OrderingService)
- Composition Pattern (User → CartManager)

**Why each pattern was chosen**
**When to use each pattern**
**When NOT to use each pattern**

**Time to read:** 10-15 minutes

---

### 3. **COMMON_MISTAKES.md**
**Anti-patterns and what NOT to do**

8 Common mistakes:
1. God Object (too much responsibility)
2. Everything is Singleton (wrong scope)
3. Tight Coupling (too many dependencies)
4. No Interfaces (hard to extend)
5. Mixing Data Access & Business Logic
6. No Clear Data Flow
7. Ignoring Edge Cases
8. Unclear Class Responsibilities

**Pre-submission checklist**
**Self-review questions**

**Time to read:** 10 minutes

---

### 4. **DOCUMENTATION_TEMPLATE.md**
**Template for documenting other examples**

- File structure for new examples
- What each documentation file should contain
- Writing tips (do's and don'ts)
- Interview preparation structure
- Quality checklist
- Example: How Zomato maps to template

**Use this when documenting future LLD examples**

**Time to read:** 5-10 minutes

---

## 🎯 How to Use This Documentation

### For Interview Preparation

**Day 1: Understand the System**
1. Read **README.md** (15 min)
2. Look at code structure (10 min)
3. Run the system (2 min)
4. Total: ~30 minutes

**Day 2: Learn the Patterns**
1. Read **PATTERNS_USED.md** (15 min)
2. For each pattern, look at real code (20 min)
3. Practice explaining each pattern (15 min)
4. Total: ~50 minutes

**Day 3: Avoid Mistakes**
1. Read **COMMON_MISTAKES.md** (10 min)
2. Review your past designs (15 min)
3. Check your design against checklist (10 min)
4. Total: ~35 minutes

**Day 4: Practice**
1. Design from scratch (20 min)
2. Use checklist to review (10 min)
3. Practice explaining to someone (15 min)
4. Total: ~45 minutes

---

### For Quick Reference

**"How do I decide where to put a method?"**
→ Read README.md → "Key Design Decisions"

**"What's the difference between Strategy and Factory?"**
→ Read PATTERNS_USED.md → Compare sections

**"What's a common mistake in this area?"**
→ Read COMMON_MISTAKES.md → Find relevant mistake

**"How would I add a new payment method?"**
→ Read README.md → "How to Extend"

**"Should I make CartManager a Singleton?"**
→ Read COMMON_MISTAKES.md → Mistake #2

---

### For Interview

**When asked to design a system similar to Zomato:**

1. **Think like the README thought process**
   - Understand problem
   - Identify models
   - Identify managers
   - Identify patterns

2. **Apply patterns from PATTERNS_USED.md**
   - Use Singleton for shared state
   - Use Manager for each model
   - Use Factory for complex creation
   - Use Strategy for variants

3. **Avoid mistakes from COMMON_MISTAKES.md**
   - Single responsibility
   - Clear ownership
   - Loose coupling
   - Extensibility

4. **Be ready to answer**
   - "Why did you choose this pattern?"
   - "How would you extend this?"
   - "What are the trade-offs?"

---

## 📋 Documentation Structure

```
zomato/docs/
├── INDEX.md                    ← You are here
├── README.md                   ← System design overview
├── PATTERNS_USED.md           ← Pattern reference
├── COMMON_MISTAKES.md         ← Anti-patterns
└── DOCUMENTATION_TEMPLATE.md  ← Template for other examples
```

---

## 🔍 Quick Navigation by Topic

### Understanding the System
- README.md → Overview
- README.md → Design Thought Process
- README.md → Architecture

### Learning Patterns
- PATTERNS_USED.md → All patterns
- README.md → Key Design Decisions

### Avoiding Mistakes
- COMMON_MISTAKES.md → All mistakes
- COMMON_MISTAKES.md → Checklist before submission

### Extending the System
- README.md → How to Extend
- COMMON_MISTAKES.md → Self-review questions

### Documenting Other Examples
- DOCUMENTATION_TEMPLATE.md → Everything

---

## 💡 Key Takeaways (TL;DR)

**Framework for designing systems:**
1. Understand user flow
2. Identify domain models
3. Create manager for each model
4. Use patterns for specific problems
5. Keep responsibilities clear

**Common patterns in LLD:**
- **Singleton:** Global shared state (RestaurantManager)
- **Manager:** Operations on one model (CartManager)
- **Factory:** Complex object creation (OrderFactory)
- **Strategy:** Multiple implementations (PaymentStrategy)
- **Orchestrator:** Coordinate multiple pieces (OrderingService)

**Avoid:**
- God Objects (too much responsibility)
- Wrong use of Singleton (per-user state)
- Tight coupling (too many dependencies)
- No interfaces (hard to extend)

---

## 📞 Questions This Documentation Answers

### Design Questions
- ✅ "How do I design a food ordering system?"
- ✅ "What classes should I create?"
- ✅ "Where should each method go?"
- ✅ "How should classes relate to each other?"
- ✅ "What patterns should I use?"

### Pattern Questions
- ✅ "When should I use Singleton?"
- ✅ "When should I use Factory?"
- ✅ "When should I use Strategy?"
- ✅ "How do I decide between patterns?"

### Extension Questions
- ✅ "How do I add a new payment method?"
- ✅ "How do I add a new notification channel?"
- ✅ "How do I add order status tracking?"

### Interview Questions
- ✅ "Why did you design it this way?"
- ✅ "What are the trade-offs?"
- ✅ "How would you extend this?"
- ✅ "What mistakes should I avoid?"

---

## ✨ This Is Your Interview Preparation Library

By studying this documentation:
- ✅ You understand one complete system deeply
- ✅ You can explain design decisions with reasoning
- ✅ You know when to use each pattern
- ✅ You can identify and fix common mistakes
- ✅ You can extend the system easily
- ✅ You can design similar systems from scratch

**Next step:** Document another example using DOCUMENTATION_TEMPLATE.md and build your library!

