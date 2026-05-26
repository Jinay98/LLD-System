package com.lld.patterns.FactoryPattern;

public class Client {
    public static void main(String[] args){
        String smallType = "SMALL";
        Burger smallBurger = BurgerFactory.getBurgerInstance(smallType);
        smallBurger.prepare();

        String largeType = "LARGE";
        Burger largeBurger = BurgerFactory.getBurgerInstance(largeType);
        largeBurger.prepare();
    }
}
