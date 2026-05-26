package com.lld.realworldexamples.DocumentEditor;

public class DocumentEditor {
    public Document doc;
    String renderedContent;

    public DocumentEditor() {

    }

    public DocumentEditor(Document doc) {
        this.doc = doc;
    }

    public String getContent() {
        if (renderedContent == null || renderedContent.isBlank()) {
            renderedContent = doc.renderElements();
        }
        return renderedContent;
    }
}
