package com.lld.patterns.FactoryPattern.FactoryMethodPattern.creators;

import com.lld.patterns.FactoryPattern.FactoryMethodPattern.models.Burger;

public abstract class BurgerRestaurant {
    public abstract Burger createBurger();

    public void orderBurger() {
        Burger burger = createBurger();
        burger.prepare();
    }
}
