package com.lld.realworldexamples.DocumentEditor;

public class ImageElement extends DocumentElement {

    String filePath;

    public ImageElement() {

    }

    public ImageElement(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public String render() {
        return "[IMG]" + " " + filePath + "\n";
    }
}
