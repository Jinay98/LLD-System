package com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.burger.nonvegetarian;

import com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.burger.Burger;

public class NonVegBurger extends Burger {
    @Override
    public void prepare() {
        System.out.println("Non-Veg Burger is prepared");
    }
}
