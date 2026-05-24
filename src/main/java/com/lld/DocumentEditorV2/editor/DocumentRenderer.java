package com.lld.DocumentEditorV2.editor;

import com.lld.DocumentEditorV2.models.Document;
import com.lld.DocumentEditorV2.models.DocumentElement;

public class DocumentRenderer {

    public DocumentRenderer() {

    }

    public String renderDocumentElements(Document document) {
        StringBuilder result = new StringBuilder();
        for (DocumentElement documentElement : document.getElements()) {
            result.append(documentElement.render());
        }
        return result.toString();
    }
}
