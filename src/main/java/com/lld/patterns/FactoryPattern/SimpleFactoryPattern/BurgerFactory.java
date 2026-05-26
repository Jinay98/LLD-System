package com.lld.patterns.FactoryPattern.SimpleFactoryPattern;

import static com.lld.patterns.FactoryPattern.SimpleFactoryPattern.enums.BurgerTypes.LARGE;
import static com.lld.patterns.FactoryPattern.SimpleFactoryPattern.enums.BurgerTypes.SMALL;

public class BurgerFactory {

    public static Burger getBurgerInstance(String type) {
        if (SMALL.name().equalsIgnoreCase(type)) {
            return new SmallBurger();
        } else if (LARGE.name().equalsIgnoreCase(type)) {
            return new LargeBurger();
        }
        return null;
    }
}
