package com.lld.DocumentEditorV2.persistence.impl;

import com.lld.DocumentEditorV2.models.Document;
import com.lld.DocumentEditorV2.persistence.Persistence;

public class MySQLPersistence implements Persistence {
    @Override
    public void save(Document document) {
        System.out.println("Saving document to MYSQL DB");
    }
}
