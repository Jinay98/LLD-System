package com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.fries.nonvegetarian;

import com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.fries.Fries;

public class NonVegFries extends Fries {
    @Override
    public void prepare() {
        System.out.println("Non-Veg Fries is prepared");
    }
}
