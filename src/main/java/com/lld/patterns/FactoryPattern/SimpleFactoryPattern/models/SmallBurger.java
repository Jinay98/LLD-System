package com.lld.patterns.FactoryPattern.SimpleFactoryPattern_new.models;

public class SmallBurger extends Burger {
    @Override
    public void prepare() {
        System.out.println("Preparing SMALL burger");
    }
}
