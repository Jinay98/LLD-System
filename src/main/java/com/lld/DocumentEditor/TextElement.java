package com.lld.DocumentEditor;

public class TextElement extends DocumentElement {
    String content;

    public TextElement() {

    }

    public TextElement(String content) {
        this.content = content;
    }

    @Override
    public String render() {
        return content + "\n";
    }
}
