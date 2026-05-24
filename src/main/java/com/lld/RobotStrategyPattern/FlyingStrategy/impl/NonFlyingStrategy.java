package com.lld.RobotStrategyPattern.FlyingStrategy.impl;

import com.lld.RobotStrategyPattern.FlyingStrategy.FlyingStrategy;

public class NonFlyingStrategy implements FlyingStrategy {
    @Override
    public void fly() {
        System.out.println("Doesn't fly at all");
    }
}
