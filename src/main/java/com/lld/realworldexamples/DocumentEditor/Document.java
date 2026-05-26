package com.lld.realworldexamples.DocumentEditor;

import java.util.ArrayList;
import java.util.List;

public class Document {
    List<DocumentElement> elements;

    public Document() {
        elements = new ArrayList<>();
    }

    public Document(List<DocumentElement> elements) {
        this.elements = elements;
    }

    public String renderElements() {
        StringBuilder result = new StringBuilder();
        for (DocumentElement element : elements) {
            result.append(element.render());
        }
        return result.toString();
    }
}
