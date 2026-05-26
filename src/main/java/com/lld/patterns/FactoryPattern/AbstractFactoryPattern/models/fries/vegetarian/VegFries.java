package com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.fries.vegetarian;

import com.lld.patterns.FactoryPattern.AbstractFactoryPattern.models.fries.Fries;

public class VegFries extends Fries {
    @Override
    public void prepare() {
        System.out.println("Veg Fries is prepared");
    }
}
