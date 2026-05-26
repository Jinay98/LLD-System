package com.lld.patterns.FactoryPattern.FactoryMethodPattern.models.large;

public class PremiumLargeBurger extends LargeBurger {
    @Override
    public void prepare() {
        super.prepare();
        System.out.println("Adding premium toppings with special sauce");
    }
}
