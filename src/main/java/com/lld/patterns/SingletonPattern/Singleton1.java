package com.lld.patterns.SingletonPattern;

//Non-Thread safe approach
public class Singleton1 {
    private static Singleton1 instance;

    private Singleton1() {
        System.out.println("Private Singleton constructor has been called");
    }

    public static Singleton1 getInstance() {
        if (instance == null) {
            instance = new Singleton1();
        }
        return instance;
    }
}
