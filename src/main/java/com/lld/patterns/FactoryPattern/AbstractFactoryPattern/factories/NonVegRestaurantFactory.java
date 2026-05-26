package com.lld.patterns.FactoryPattern.AbstractFactoryPattern.factories;

import com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.burger.Burger;
import com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.fries.Fries;
import com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.burger.nonvegetarian.NonVegBurger;
import com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.fries.nonvegetarian.NonVegFries;

public class NonVegRestaurantFactory extends RestaurantFactory {
    @Override
    public Burger createBurger() {
        return new NonVegBurger();
    }

    @Override
    public Fries createFries() {
        return new NonVegFries();
    }
}
