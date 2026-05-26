package com.lld.patterns.FactoryPattern.FactoryMethodPattern.models.small;

import com.lld.patterns.FactoryPattern.FactoryMethodPattern.models.Burger;

public abstract class SmallBurger extends Burger {
    @Override
    public void prepare() {
        System.out.println("Preparing SMALL burger");
    }
}
