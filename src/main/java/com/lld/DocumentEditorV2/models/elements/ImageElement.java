package com.lld.DocumentEditorV2.models.elements;

import com.lld.DocumentEditorV2.models.DocumentElement;

public class ImageElement extends DocumentElement {
    public ImageElement(int index, String content) {
        super.setIndex(index);
        super.setContent(content);
    }

    @Override
    public String render() {
        return "[IMG] - " + this.getContent();
    }
}
