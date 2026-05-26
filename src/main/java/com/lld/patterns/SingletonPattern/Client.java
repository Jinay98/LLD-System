package com.lld.patterns.SingletonPattern;

public class Client {
    public static void main(String[] args) {
        Singleton instance1 = Singleton.getInstance();
        Singleton instance2 = Singleton.getInstance();

        if (instance1 == instance2) {
            System.out.println("Both the instances point to the same object");
        }
    }
}
