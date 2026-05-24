package com.lld.DocumentEditorV2.models;

public abstract class DocumentElement {
    private int index;
    private String content;

    public void setIndex(int index) {
        this.index = index;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getIndex() {
        return this.index;
    }

    public String getContent() {
        return content;
    }

    public abstract String render();
}
