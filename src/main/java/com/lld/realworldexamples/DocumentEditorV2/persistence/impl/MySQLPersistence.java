package com.lld.realworldexamples.DocumentEditorV2.persistence.impl;

import com.lld.realworldexamples.DocumentEditorV2.models.Document;
import com.lld.realworldexamples.DocumentEditorV2.persistence.Persistence;

public class MySQLPersistence implements Persistence {
    @Override
    public void save(Document document) {
        System.out.println("Saving document to MYSQL DB");
    }
}
