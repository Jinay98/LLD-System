package com.lld.patterns.SingletonPattern;


public class Client {
    public static void main(String[] args) {
        Singleton1 instance1 = Singleton1.getInstance();
        Singleton1 instance2 = Singleton1.getInstance();

        if (instance1 == instance2) {
            System.out.println("Both the instances point to the same object");
        }


        Singleton2 obj1 = Singleton2.getInstance();
        Singleton2 obj2 = Singleton2.getInstance();

        if (obj1 == obj2) {
            System.out.println("Both the instances point to the same object");
        }


        Singleton3 temp1 = Singleton3.getInstance();
        Singleton3 temp2 = Singleton3.getInstance();

        if (temp1 == temp2) {
            System.out.println("Both the instances point to the same object");
        }
    }
}
