package com.lld.patterns.FactoryPattern.AbstractFactoryPattern.factories;

import com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.burger.Burger;
import com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.fries.Fries;

public abstract class RestaurantFactory {
    public abstract Burger createBurger();
    public abstract Fries createFries();
}
