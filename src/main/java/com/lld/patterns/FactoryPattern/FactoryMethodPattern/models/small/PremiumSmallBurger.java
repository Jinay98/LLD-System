package com.lld.patterns.FactoryPattern.FactoryMethodPattern.models.small;

public class PremiumSmallBurger extends SmallBurger {
    @Override
    public void prepare() {
        super.prepare();
        System.out.println("Adding premium toppings with special sauce");
    }
}
