package com.lld.patterns.FactoryPattern;

public class LargeBurger extends Burger{
    @Override
    void prepare() {
        System.out.println("Preparing LARGE burger");
    }
}
