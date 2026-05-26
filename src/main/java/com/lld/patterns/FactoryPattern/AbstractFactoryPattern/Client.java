package com.lld.patterns.FactoryPattern.AbstractFactoryPattern;

import com.lld.patterns.FactoryPattern.AbstractFactoryPattern.factories.NonVegRestaurantFactory;
import com.lld.patterns.FactoryPattern.AbstractFactoryPattern.factories.RestaurantFactory;
import com.lld.patterns.FactoryPattern.AbstractFactoryPattern.factories.VegRestaurantFactory;
import com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.burger.Burger;
import com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.fries.Fries;

public class Client {
    public static void main(String[] args) {
        RestaurantFactory vegRestaurantFactory = new VegRestaurantFactory();
        Burger vegBurger = vegRestaurantFactory.createBurger();
        Fries vegFries = vegRestaurantFactory.createFries();
        vegBurger.prepare();
        vegFries.prepare();

        RestaurantFactory nonVegRestaurantFactory = new NonVegRestaurantFactory();
        Burger nonVegBurger = nonVegRestaurantFactory.createBurger();
        Fries nonVegFries = nonVegRestaurantFactory.createFries();
        nonVegBurger.prepare();
        nonVegFries.prepare();


    }
}
