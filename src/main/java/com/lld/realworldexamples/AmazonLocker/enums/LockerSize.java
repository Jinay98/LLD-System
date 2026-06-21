package com.lld.realworldexamples.AmazonLocker.enums;

public enum LockerSize {
    SMALL(20, 25, 30),
    MEDIUM(30, 35, 40),
    LARGE(40, 45, 55),
    XL(55, 60, 70);

    private final double maxHeight;
    private final double maxWidth;
    private final double maxDepth;

    LockerSize(double maxHeight, double maxWidth, double maxDepth) {
        this.maxHeight = maxHeight;
        this.maxWidth = maxWidth;
        this.maxDepth = maxDepth;
    }

    public double getMaxHeight() {
        return maxHeight;
    }

    public double getMaxWidth() {
        return maxWidth;
    }

    public double getMaxDepth() {
        return maxDepth;
    }
}
