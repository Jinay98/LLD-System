package com.lld.patterns.SingletonPattern;

// Thread safe approach by early initialization
public class Singleton3 {
    private static final Singleton3 instance = new Singleton3();

    private Singleton3() {
        System.out.println("Private Singleton constructor has been called");
    }

    public static Singleton3 getInstance(){
        return instance;
    }
}
