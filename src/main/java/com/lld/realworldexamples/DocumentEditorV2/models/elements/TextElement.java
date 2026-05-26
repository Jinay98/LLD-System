package com.lld.realworldexamples.DocumentEditorV2.models.elements;

import com.lld.realworldexamples.DocumentEditorV2.models.DocumentElement;

public class TextElement extends DocumentElement {
    public TextElement(int index, String content) {
        super.setIndex(index);
        super.setContent(content);
    }

    @Override
    public String render() {
        return this.getContent();
    }
}
