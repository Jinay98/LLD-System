package com.lld.patterns.FactoryPattern.SimpleFactoryPattern_new.factory;

import com.lld.patterns.FactoryPattern.SimpleFactoryPattern_new.models.Burger;
import com.lld.patterns.FactoryPattern.SimpleFactoryPattern_new.models.SmallBurger;
import com.lld.patterns.FactoryPattern.SimpleFactoryPattern_new.models.LargeBurger;
import com.lld.patterns.FactoryPattern.SimpleFactoryPattern_new.factory.enums.BurgerTypes;

public class BurgerFactory {
    public static Burger getBurgerInstance(BurgerTypes type) {
        if (type == null) {
            throw new IllegalArgumentException("Burger type cannot be null");
        }

        return switch (type) {
            case SMALL -> new SmallBurger();
            case LARGE -> new LargeBurger();
        };
    }
}
