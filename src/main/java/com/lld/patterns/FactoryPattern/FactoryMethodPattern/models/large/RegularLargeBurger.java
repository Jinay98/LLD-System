package com.lld.patterns.FactoryPattern.FactoryMethodPattern.models.large;

public class RegularLargeBurger extends LargeBurger {
    @Override
    public void prepare() {
        super.prepare();
        System.out.println("Adding regular toppings");
    }
}
