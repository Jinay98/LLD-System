package com.lld.patterns.StrategyPattern;

import com.lld.patterns.StrategyPattern.FlyingStrategy.FlyingStrategy;
import com.lld.patterns.StrategyPattern.FlyingStrategy.impl.NonFlyingStrategy;
import com.lld.patterns.StrategyPattern.FlyingStrategy.impl.NormalFlyingStrategy;
import com.lld.patterns.StrategyPattern.TalkingStrategy.TalkingStrategy;
import com.lld.patterns.StrategyPattern.TalkingStrategy.impl.NonTalkingStrategy;
import com.lld.patterns.StrategyPattern.TalkingStrategy.impl.NormalTakingStrategy;

public class Client {
    public static void main(String[] args) {
        FlyingStrategy nonFlyingStrategy = new NonFlyingStrategy();
        TalkingStrategy nonTalkingStrategy = new NonTalkingStrategy();
        Robot deadRobot = new Robot(nonTalkingStrategy, nonFlyingStrategy);
        deadRobot.fly();
        deadRobot.talk();


        FlyingStrategy flyingStrategy = new NormalFlyingStrategy();
        TalkingStrategy talkingStrategy = new NormalTakingStrategy();
        Robot aliveRobot = new Robot(talkingStrategy, flyingStrategy);
        aliveRobot.fly();
        aliveRobot.talk();
    }
}
