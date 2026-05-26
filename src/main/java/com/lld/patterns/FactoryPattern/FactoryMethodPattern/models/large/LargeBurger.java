package com.lld.patterns.FactoryPattern.FactoryMethodPattern.models.large;

import com.lld.patterns.FactoryPattern.FactoryMethodPattern.models.Burger;

public abstract class LargeBurger extends Burger {
    @Override
    public void prepare() {
        System.out.println("Preparing LARGE burger");
    }
}
