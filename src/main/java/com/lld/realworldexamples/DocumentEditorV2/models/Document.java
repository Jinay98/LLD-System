package com.lld.realworldexamples.DocumentEditorV2.models;

import java.util.ArrayList;
import java.util.List;

public class Document {
    private List<DocumentElement> elements = new ArrayList<>();

    public List<DocumentElement> getElements() {
        return elements;
    }
}
