package com.lld.realworldexamples.AmazonShoppingSystem.exceptions;

public class OutOfStockException extends RuntimeException {
    public OutOfStockException(String message) {
        super(message);
    }
}
