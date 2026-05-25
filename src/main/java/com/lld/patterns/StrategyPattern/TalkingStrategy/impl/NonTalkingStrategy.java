package com.lld.patterns.StrategyPattern.TalkingStrategy.impl;

import com.lld.patterns.StrategyPattern.TalkingStrategy.TalkingStrategy;

public class NonTalkingStrategy implements TalkingStrategy {
    @Override
    public void talk() {
        System.out.println("Doesn't talk at all");
    }
}
