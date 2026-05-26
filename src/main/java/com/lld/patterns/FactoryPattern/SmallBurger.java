package com.lld.patterns.FactoryPattern;

public class SmallBurger extends Burger {

    @Override
    public void prepare() {
        System.out.println("Preparing SMALL burger");
    }
}
