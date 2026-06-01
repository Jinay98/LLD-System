package com.lld.patterns.SingletonPattern;

public class Singleton4 {
    private static final Object lock = new Object();
    private static volatile Singleton4 instance;

    private Singleton4() {
        System.out.println("Private Singleton constructor has been called");
    }

    public static Singleton4 getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new Singleton4();
                }
            }
        }
        return instance;
    }
}
