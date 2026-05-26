package com.lld.realworldexamples.DocumentEditorV2.models;

public abstract class DocumentElement {
    private int index;
    private String content;

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public abstract String render();
}
