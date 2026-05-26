package com.lld.realworldexamples.DocumentEditorV2.persistence;

import com.lld.realworldexamples.DocumentEditorV2.models.Document;

public interface Persistence {
    void save(Document document);
}
