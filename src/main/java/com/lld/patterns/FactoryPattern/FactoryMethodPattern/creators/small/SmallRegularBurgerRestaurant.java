package com.lld.patterns.FactoryPattern.FactoryMethodPattern.creators.small;

import com.lld.patterns.FactoryPattern.FactoryMethodPattern.creators.BurgerRestaurant;
import com.lld.patterns.FactoryPattern.FactoryMethodPattern.models.Burger;
import com.lld.patterns.FactoryPattern.FactoryMethodPattern.models.small.RegularSmallBurger;

public class SmallRegularBurgerRestaurant extends BurgerRestaurant {
    @Override
    public Burger createBurger() {
        return new RegularSmallBurger();
    }
}
