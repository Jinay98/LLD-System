package com.lld.patterns.FactoryPattern.AbstractFactoryPattern.factories;

import com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.burger.Burger;
import com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.fries.Fries;
import com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.burger.vegetarian.VegBurger;
import com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.fries.vegetarian.VegFries;

public class VegRestaurantFactory extends RestaurantFactory{
    @Override
    public Burger createBurger() {
        return new VegBurger();
    }

    @Override
    public Fries createFries() {
        return new VegFries();
    }
}
