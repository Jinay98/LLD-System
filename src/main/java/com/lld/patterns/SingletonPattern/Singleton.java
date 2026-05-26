package com.lld.patterns.SingletonPattern;

public class Singleton {
    private static Singleton instance;

    private Singleton() {
        System.out.println("Private Singleton constructor has been called");
    }

    public static Singleton getInstance() {
        if (instance == null) {
            instance = new Singleton();
        }
        return instance;
    }
}
