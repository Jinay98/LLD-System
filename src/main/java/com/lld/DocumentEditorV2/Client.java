package com.lld.DocumentEditorV2;

import com.lld.DocumentEditorV2.models.Document;
import com.lld.DocumentEditorV2.models.DocumentElement;
import com.lld.DocumentEditorV2.models.elements.TextElement;
import com.lld.DocumentEditorV2.models.elements.NewLineElement;
import com.lld.DocumentEditorV2.models.elements.ImageElement;
import com.lld.DocumentEditorV2.editor.DocumentEditor;
import com.lld.DocumentEditorV2.editor.DocumentRenderer;
import com.lld.DocumentEditorV2.persistence.Persistence;
import com.lld.DocumentEditorV2.persistence.impl.MySQLPersistence;

public class Client {
    public static void main(String[] args) {
        DocumentElement documentElement1 = new TextElement(0, "Dummy Title");
        DocumentElement documentElement2 = new NewLineElement(1);
        DocumentElement documentElement3 = new TextElement(2, "First Para");
        DocumentElement documentElement4 = new NewLineElement(3);
        DocumentElement documentElement5 = new ImageElement(4, "img.jpg");
        DocumentElement documentElement6 = new NewLineElement(5);
        DocumentElement documentElement7 = new TextElement(6, "Last Para");

        Document document = new Document();
        Persistence persistence = new MySQLPersistence();
        DocumentRenderer documentRenderer = new DocumentRenderer();
        DocumentEditor documentEditor = new DocumentEditor(document, persistence, documentRenderer);

        documentEditor.addElement(0, documentElement1);
        documentEditor.addElement(1, documentElement2);
        documentEditor.addElement(2, documentElement3);
        documentEditor.addElement(3, documentElement4);
        documentEditor.addElement(4, documentElement5);
        documentEditor.addElement(5, documentElement6);
        documentEditor.addElement(6, documentElement7);

        documentEditor.removeElement(6);
        documentEditor.addElement(6, documentElement7);

        documentEditor.renderDocument();
        documentEditor.saveToStorage();
    }
}
