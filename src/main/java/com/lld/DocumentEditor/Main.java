package com.lld.DocumentEditor;


import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        DocumentElement textElement1 = new TextElement("First line");
        DocumentElement textElement2 = new TextElement("Second Line");
        DocumentElement imageElement1 = new ImageElement("/tmp/images");
        List<DocumentElement> documentElements = new ArrayList<>();
        documentElements.add(textElement1);
        documentElements.add(textElement2);
        documentElements.add(imageElement1);
        Document document = new Document(documentElements);
        DocumentEditor documentEditor = new DocumentEditor(document);
        String contentInDocument = documentEditor.getContent();
        System.out.println("Content in Document:\n");
        System.out.println(contentInDocument);
    }
}
