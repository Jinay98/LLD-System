package com.lld.patterns.StrategyPattern.TalkingStrategy.impl;

import com.lld.patterns.StrategyPattern.TalkingStrategy.TalkingStrategy;

public class NormalTakingStrategy implements TalkingStrategy {
    @Override
    public void talk() {
        System.out.println("Talks in a normal speed");
    }
}
