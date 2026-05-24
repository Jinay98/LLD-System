package com.lld.DocumentEditorV2.editor;

import com.lld.DocumentEditorV2.models.Document;
import com.lld.DocumentEditorV2.models.DocumentElement;
import com.lld.DocumentEditorV2.persistence.Persistence;

public class DocumentEditor {
    private Document document;
    private Persistence persistence;
    private DocumentRenderer documentRenderer;

    public DocumentEditor(Document document, Persistence persistence, DocumentRenderer documentRenderer) {
        this.document = document;
        this.persistence = persistence;
        this.documentRenderer = documentRenderer;
    }

    public void addElement(int index, DocumentElement element) {
        this.document.getElements().add(index, element);
    }

    public void removeElement(int index) {
        this.document.getElements().remove(index);
    }

    public void renderDocument() {
        System.out.println(documentRenderer.renderDocumentElements(document));
    }

    public void saveToStorage() {
        this.persistence.save(document);
    }
}
