# DocumentEditor - Composite Pattern LLD

## Overview
A simple document editor that demonstrates the **Composite Pattern**. The system shows how to structure documents as a tree of elements (text, images) that can be treated uniformly.

---

## 📖 Design Thought Process

### Step 1: Understand the Problem
**What does a user do?**
1. Create a document
2. Add text elements to the document
3. Add image elements to the document
4. Get the complete content (render all elements)

### Step 2: Identify the Pattern Need
**Challenge:** TextElement and ImageElement are different, but we want to treat them the same way.

```
TextElement {
    text: String
}

ImageElement {
    imagePath: String
}

Document {
    elements: List<TextElement, ImageElement>  ← Mixed types!
}
```

**Solution:** Create a common interface and treat both as DocumentElement.

### Step 3: Apply Composite Pattern
```
┌──────────────────┐
│ DocumentElement  │ (Interface/Abstract)
├──────────────────┤
│ render()         │
└──────────────────┘
        △ △
        │ │
   ┌────┘ └────┐
   │            │
┌──────────┐ ┌────────────┐
│TextElem  │ │ImageElem   │
├──────────┤ ├────────────┤
│text      │ │imagePath   │
│render()  │ │render()    │
└──────────┘ └────────────┘

Document contains: List<DocumentElement>
                   ├─ TextElement
                   ├─ TextElement
                   └─ ImageElement

getContent() → iterate all → call render() on each
```

---

## 🏗️ Architecture

```
┌─────────────────────────────────┐
│         Main (Client)           │
│  - Creates elements             │
│  - Creates document             │
│  - Calls editor                 │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│      DocumentEditor             │
│  - Takes Document               │
│  - getContent()                 │
│    └─ iterates all elements     │
│       └─ calls render() on each │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│        Document                 │
│  - List<DocumentElement>        │
│  - Stores all elements          │
└────────────┬────────────────────┘
             │
      ┌──────┴──────┐
      │             │
      ▼             ▼
┌────────────┐ ┌────────────┐
│TextElement │ │ImageElement│
│ render()   │ │ render()   │
└────────────┘ └────────────┘
```

---

## 🎯 Key Design Decision: Composite Pattern

### Why Use Composite?

| Aspect | Reason |
|--------|--------|
| **Common Interface** | TextElement & ImageElement implement DocumentElement |
| **Treat Uniformly** | Document doesn't care what element type it is |
| **Easy to Add** | New element types just implement DocumentElement |
| **Clean Code** | No if-else checking element types |

### The Pattern

```java
// Common interface
interface DocumentElement {
    String render();
}

// Different implementations
class TextElement implements DocumentElement {
    public String render() { return text; }
}

class ImageElement implements DocumentElement {
    public String render() { return "[Image: " + imagePath + "]"; }
}

// Use uniformly
class Document {
    List<DocumentElement> elements;
    
    // Treats all elements the same
    for (DocumentElement elem : elements) {
        elem.render();  // Polymorphism!
    }
}
```

### Without Composite (Bad)
```java
// BAD: Document knows about specific types
class Document {
    List<TextElement> textElements;
    List<ImageElement> imageElements;
    
    String getContent() {
        String result = "";
        for (TextElement e : textElements) result += e.getText();
        for (ImageElement e : imageElements) result += "[Image: " + e.getPath() + "]";
        return result;
    }
}

// New element type? Modify Document class!
```

---

## 📊 Data Flow

```
1. CREATE ELEMENTS
   TextElement("First line")
   TextElement("Second Line")
   ImageElement("/tmp/images")

2. CREATE DOCUMENT
   Document(List<DocumentElement>)
   └─ stores all elements

3. CREATE EDITOR
   DocumentEditor(document)

4. GET CONTENT
   documentEditor.getContent()
   └─ Iterate all elements in document
   └─ Call render() on each
   └─ Combine all results

5. OUTPUT
   Print combined content
```

---

## 🧩 Class Responsibilities

| Class | Responsibility |
|-------|---|
| `DocumentElement` | Interface defining contract for all elements |
| `TextElement` | Renders text content |
| `ImageElement` | Renders image reference |
| `Document` | Container for all elements |
| `DocumentEditor` | Aggregates content from all elements |
| `Main` | Orchestrates the flow |

---

## 🔄 How to Extend

### Add New Element Type
```java
// 1. Create new element implementing DocumentElement
public class TableElement implements DocumentElement {
    private String tableData;
    
    @Override
    public String render() {
        return "[TABLE]\n" + tableData + "\n[/TABLE]";
    }
}

// 2. Add to document
documentElements.add(new TableElement(data));

// 3. DocumentEditor automatically handles it (no changes needed!)
```

### Add Formatting
```java
// Modify DocumentEditor, not the elements
class DocumentEditor {
    public String getContent() {
        String result = "--- Document Start ---\n";
        for (DocumentElement elem : document.getElements()) {
            result += elem.render() + "\n";
        }
        result += "--- Document End ---";
        return result;
    }
}
```

---

## 💡 Key Takeaways

1. **Composite Pattern:** Common interface for different types
2. **Polymorphism:** Treat different elements uniformly
3. **Easy Extension:** New elements just implement interface
4. **Clean Code:** No type-checking or casting needed
5. **Open/Closed:** Open for extension (new elements), closed for modification (DocumentEditor doesn't change)

---

## 🧪 Running the System

```bash
java -cp target/classes com.lld.realworldexamples.DocumentEditor.Main
```

**Output:**
```
Content in Document:

First line
Second Line
[Image: /tmp/images]
```

