package com.lld.patterns.FactoryPattern.FactoryMethodPattern;

import com.lld.patterns.FactoryPattern.FactoryMethodPattern.creators.BurgerRestaurant;
import com.lld.patterns.FactoryPattern.FactoryMethodPattern.creators.small.SmallPremiumBurgerRestaurant;
import com.lld.patterns.FactoryPattern.FactoryMethodPattern.creators.large.LargeRegularBurgerRestaurant;
import com.lld.patterns.FactoryPattern.FactoryMethodPattern.models.Burger;

public class Client {
    public static void main(String[] args) {
        BurgerRestaurant smallPremiumRestaurant = new SmallPremiumBurgerRestaurant();
        smallPremiumRestaurant.orderBurger();

        System.out.println();

        BurgerRestaurant largeRegularRestaurant = new LargeRegularBurgerRestaurant();
        largeRegularRestaurant.orderBurger();
    }
}
