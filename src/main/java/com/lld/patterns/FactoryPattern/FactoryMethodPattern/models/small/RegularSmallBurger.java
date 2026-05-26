package com.lld.patterns.FactoryPattern.FactoryMethodPattern.models.small;

public class RegularSmallBurger extends SmallBurger {
    @Override
    public void prepare() {
        super.prepare();
        System.out.println("Adding regular toppings");
    }
}
