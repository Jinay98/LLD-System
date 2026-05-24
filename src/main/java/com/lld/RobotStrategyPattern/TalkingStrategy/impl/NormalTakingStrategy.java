package com.lld.RobotStrategyPattern.TalkingStrategy.impl;

import com.lld.RobotStrategyPattern.TalkingStrategy.TalkingStrategy;

public class NormalTakingStrategy implements TalkingStrategy {
    @Override
    public void talk() {
        System.out.println("Talks in a normal speed");
    }
}
