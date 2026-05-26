package com.lld.patterns.FactoryPattern.FactoryMethodPattern.creators.large;

import com.lld.patterns.FactoryPattern.FactoryMethodPattern.creators.BurgerRestaurant;
import com.lld.patterns.FactoryPattern.FactoryMethodPattern.models.Burger;
import com.lld.patterns.FactoryPattern.FactoryMethodPattern.models.large.PremiumLargeBurger;

public class LargePremiumBurgerRestaurant extends BurgerRestaurant {
    @Override
    public Burger createBurger() {
        return new PremiumLargeBurger();
    }
}
