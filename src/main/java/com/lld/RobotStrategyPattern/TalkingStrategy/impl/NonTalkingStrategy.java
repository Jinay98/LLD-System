package com.lld.RobotStrategyPattern.TalkingStrategy.impl;

import com.lld.RobotStrategyPattern.TalkingStrategy.TalkingStrategy;

public class NonTalkingStrategy implements TalkingStrategy {
    @Override
    public void talk() {
        System.out.println("Doesn't talk at all");
    }
}
