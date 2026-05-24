package com.lld.RobotStrategyPattern.FlyingStrategy.impl;

import com.lld.RobotStrategyPattern.FlyingStrategy.FlyingStrategy;

public class NormalFlyingStrategy implements FlyingStrategy {
    @Override
    public void fly() {
        System.out.println("Flying with normal speed");
    }
}
