package com.lld.DocumentEditor;

import java.util.List;

public class Document {
    List<DocumentElement> elements;

    public Document() {

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
