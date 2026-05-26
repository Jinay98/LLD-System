package com.lld.patterns.FactoryPattern.SimpleFactoryPattern;

public class LargeBurger extends Burger {
    @Override
    public void prepare() {
        System.out.println("Preparing LARGE burger");
    }
}
