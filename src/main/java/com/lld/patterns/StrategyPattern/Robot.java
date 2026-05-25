package com.lld.patterns.StrategyPattern;

import com.lld.patterns.StrategyPattern.FlyingStrategy.FlyingStrategy;
import com.lld.patterns.StrategyPattern.TalkingStrategy.TalkingStrategy;

public class Robot {
    TalkingStrategy talkingStrategy;
    FlyingStrategy flyingStrategy;

    public Robot(TalkingStrategy talkingStrategy, FlyingStrategy flyingStrategy) {
        this.talkingStrategy = talkingStrategy;
        this.flyingStrategy = flyingStrategy;
    }

    void talk() {
        talkingStrategy.talk();
    }

    void fly() {
        flyingStrategy.fly();
    }

}
