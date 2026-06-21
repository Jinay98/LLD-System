package com.lld.realworldexamples.AmazonLocker.exceptions;

public class NoAvailableLockerException extends RuntimeException {
    public NoAvailableLockerException(String message) {
        super(message);
    }
}
