package com.lld.RobotStrategyPattern;

import com.lld.RobotStrategyPattern.FlyingStrategy.FlyingStrategy;
import com.lld.RobotStrategyPattern.TalkingStrategy.TalkingStrategy;

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
