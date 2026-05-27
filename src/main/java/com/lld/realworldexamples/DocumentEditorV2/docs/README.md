# DocumentEditorV2 - Advanced Composite Pattern with Hierarchical Structure

## Overview
An advanced document editor demonstrating **Composite Pattern with hierarchical/tree structure**. Shows how to build complex nested documents where containers can contain other containers (sections containing subsections containing elements).

This is **V2 - evolved from DocumentEditor** with support for nested structures.

---

## 📖 Design Evolution: V1 → V2

### DocumentEditor V1 (Simple Composite)
```
Document
├─ TextElement
├─ TextElement
└─ ImageElement

Simple flat list of elements.
```

### DocumentEditorV2 (Hierarchical Composite)
```
Document
├─ Section (container)
│  ├─ TextElement
│  ├─ TextElement
│  └─ SubSection (nested container)
│     ├─ TextElement
│     └─ ImageElement
│
├─ Section (container)
│  └─ TextElement
│
└─ TextElement
```

**Why V2?**
- Real documents have structure (chapters, sections, subsections)
- Support nesting and hierarchy
- Composite pattern truly shines with trees

---

## 🏗️ Architecture: True Composite Pattern

```
┌──────────────────────┐
│  DocumentComponent   │ (Interface)
├──────────────────────┤
│ + render(): String   │
│ + getSize(): int     │
└──────────────────────┘
         △ △ △
         │ │ │
    ┌────┘ │ └────┐
    │      │      │
    ▼      ▼      ▼
┌────────────┐ ┌──────────────┐ ┌──────────────┐
│ Paragraph  │ │ Image        │ │ Section      │
├────────────┤ ├──────────────┤ ├──────────────┤
│text        │ │path          │ │children[]    │
│render()    │ │render()      │ │render()      │
│getSize()   │ │getSize()     │ │  ← calls     │
└────────────┘ └──────────────┘ │    child     │
                                 │    render() │
                                 │getSize()    │
                                 │  ← sums     │
                                 │    children │
                                 └──────────────┘
```

### Key Difference: Composite Can Contain Components

```java
// V1: Only leaf elements
interface DocumentElement {
    String render();
}

// V2: Components can be leaf OR container
interface DocumentComponent {
    String render();
    int getSize();
}

// Leaf
class Paragraph implements DocumentComponent {
    public String render() { return text; }
    public int getSize() { return text.length(); }
}

// Composite (can contain children)
class Section implements DocumentComponent {
    List<DocumentComponent> children;
    
    public String render() {
        // Delegate to children
        String result = title + "\n";
        for (DocumentComponent child : children) {
            result += child.render() + "\n";
        }
        return result;
    }
    
    public int getSize() {
        // Aggregate from children
        int total = title.length();
        for (DocumentComponent child : children) {
            total += child.getSize();
        }
        return total;
    }
}
```

---

## 🎯 True Composite Benefits

### 1. Tree Structure
```
Can nest containers infinitely deep
Document
└─ Section
   └─ SubSection
      └─ SubSubSection
         └─ Paragraph ← Finally a leaf
```

### 2. Recursive Operations
```
// Single operation on entire tree
String output = document.render();
├─ Calls section.render()
│  ├─ Calls subsection.render()
│  │  ├─ Calls paragraph.render()
│  │  └─ Calls image.render()
│  └─ Combines results
└─ Combines all results
```

### 3. Aggregate Operations
```
int totalSize = document.getSize();
├─ Sums section1.getSize()
├─ Sums section2.getSize()
└─ Returns total

Works for any depth of nesting!
```

---

## 📊 Data Flow

```
1. CREATE LEAF ELEMENTS
   Paragraph("Introduction text")
   Image("/path/to/image.jpg")

2. CREATE CONTAINERS
   Section("Chapter 1")
   SubSection("Section 1.1")

3. BUILD HIERARCHY
   document.addChild(section)
   section.addChild(subsection)
   subsection.addChild(paragraph)
   subsection.addChild(image)

4. RENDER RECURSIVELY
   document.render()
   ├─ section.render()
   │  └─ subsection.render()
   │     ├─ paragraph.render()
   │     └─ image.render()

5. AGGREGATE SIZE
   document.getSize()
   ├─ section.getSize()
   │  └─ subsection.getSize()
   │     ├─ paragraph.getSize()
   │     └─ image.getSize()
```

---

## 🧩 Class Responsibilities

| Class | Responsibility |
|-------|---|
| `DocumentComponent` | Interface for all components (leaf and composite) |
| `Paragraph` | Leaf - renders text |
| `Image` | Leaf - renders image reference |
| `Section` | Composite - contains children, delegates operations |
| `SubSection` | Composite - nested container |
| `Document` | Root composite container |

---

## 🔄 How to Extend

### Add New Leaf Type
```java
public class CodeBlock implements DocumentComponent {
    private String code;
    
    @Override
    public String render() {
        return "```\n" + code + "\n```";
    }
    
    @Override
    public int getSize() { return code.length(); }
}

// Use it
section.addChild(new CodeBlock("public static void main() { }"));
```

### Add New Composite Type
```java
public class Chapter implements DocumentComponent {
    List<DocumentComponent> children;
    String title;
    
    @Override
    public String render() {
        String result = "# " + title + "\n";
        for (DocumentComponent child : children) {
            result += child.render() + "\n";
        }
        return result;
    }
    
    @Override
    public int getSize() {
        int total = title.length();
        for (DocumentComponent child : children) {
            total += child.getSize();
        }
        return total;
    }
    
    public void addChild(DocumentComponent component) {
        children.add(component);
    }
}
```

---

## 💡 Key Takeaways

1. **True Composite:** Containers (composites) can contain other containers
2. **Recursive:** Operations naturally recurse through tree
3. **Uniform Treatment:** Code doesn't care if it's leaf or composite
4. **Aggregate Operations:** Can sum/calculate across entire tree
5. **Scalable:** Works for any depth of nesting
6. **V1 → V2 Evolution:** Shows how patterns grow with requirements

---

## Comparison: V1 vs V2

| Aspect | V1 | V2 |
|--------|----|----|
| **Structure** | Flat list | Tree/Hierarchy |
| **Nesting** | ❌ No | ✅ Yes |
| **Composite Operations** | No | ✅ Yes (aggregate) |
| **Recursion** | ❌ No | ✅ Yes |
| **Real-World** | ❌ Limited | ✅ Documents, UIs, File systems |

