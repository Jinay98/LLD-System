package com.lld.realworldexamples.AmazonLocker.exceptions;

public class PackageExpiredException extends RuntimeException {
    public PackageExpiredException(String message) {
        super(message);
    }
}
