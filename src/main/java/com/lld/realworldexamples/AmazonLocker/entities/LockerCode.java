package com.lld.realworldexamples.AmazonLocker.entities;

public class LockerCode {
    private final String code;
    private final String packageId;
    private final long expirationTime; // epoch milliseconds

    public LockerCode(String code, String packageId, long expirationTime) {
        this.code = code;
        this.packageId = packageId;
        this.expirationTime = expirationTime;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }

    public String getCode() {
        return code;
    }

    public String getPackageId() {
        return packageId;
    }

    public long getExpirationTime() {
        return expirationTime;
    }
}
