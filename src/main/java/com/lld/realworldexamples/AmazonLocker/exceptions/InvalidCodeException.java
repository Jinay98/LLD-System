package com.lld.realworldexamples.AmazonLocker.exceptions;

public class InvalidCodeException extends RuntimeException {
    public InvalidCodeException(String message) {
        super(message);
    }
}
