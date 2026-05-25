package com.lld.patterns.StrategyPattern.FlyingStrategy.impl;

import com.lld.patterns.StrategyPattern.FlyingStrategy.FlyingStrategy;

public class NormalFlyingStrategy implements FlyingStrategy {
    @Override
    public void fly() {
        System.out.println("Flying with normal speed");
    }
}
