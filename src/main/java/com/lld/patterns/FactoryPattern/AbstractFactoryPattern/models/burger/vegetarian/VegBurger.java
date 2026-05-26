package com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.burger.vegetarian;

import com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.burger.Burger;

public class VegBurger extends Burger {
    @Override
    public void prepare() {
        System.out.println("Veg Burger is prepared");
    }
}
