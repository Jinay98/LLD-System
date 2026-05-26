package com.lld.patterns.SingletonPattern;

//Thread safe approach by using synchronized keyword
public class Singleton2 {
    private static Singleton2 instance;

    private Singleton2() {
        System.out.println("Private Singleton constructor has been called");
    }

    synchronized public static Singleton2 getInstance() {
        if (instance == null) {
            instance = new Singleton2();
        }
        return instance;
    }
}

