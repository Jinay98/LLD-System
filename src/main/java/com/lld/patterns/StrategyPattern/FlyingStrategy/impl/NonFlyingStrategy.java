package com.lld.patterns.StrategyPattern.FlyingStrategy.impl;

import com.lld.patterns.StrategyPattern.FlyingStrategy.FlyingStrategy;

public class NonFlyingStrategy implements FlyingStrategy {
    @Override
    public void fly() {
        System.out.println("Doesn't fly at all");
    }
}
