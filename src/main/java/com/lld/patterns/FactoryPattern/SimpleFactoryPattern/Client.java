package com.lld.patterns.FactoryPattern.SimpleFactoryPattern_new;

import com.lld.patterns.FactoryPattern.SimpleFactoryPattern_new.factory.BurgerFactory;
import com.lld.patterns.FactoryPattern.SimpleFactoryPattern_new.factory.enums.BurgerTypes;
import com.lld.patterns.FactoryPattern.SimpleFactoryPattern_new.models.Burger;

public class Client {
    public static void main(String[] args) {
        Burger smallBurger = BurgerFactory.getBurgerInstance(BurgerTypes.SMALL);
        smallBurger.prepare();

        System.out.println();

        Burger largeBurger = BurgerFactory.getBurgerInstance(BurgerTypes.LARGE);
        largeBurger.prepare();
    }
}
