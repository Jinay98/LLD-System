package com.lld.DocumentEditorV2.models.elements;

import com.lld.DocumentEditorV2.models.DocumentElement;

public class NewLineElement extends DocumentElement {
    public NewLineElement(int index) {
        super.setIndex(index);
    }

    @Override
    public String render() {
        return "\n";
    }
}
