package com.lld.RobotStrategyPattern;

import com.lld.RobotStrategyPattern.FlyingStrategy.FlyingStrategy;
import com.lld.RobotStrategyPattern.FlyingStrategy.impl.NonFlyingStrategy;
import com.lld.RobotStrategyPattern.FlyingStrategy.impl.NormalFlyingStrategy;
import com.lld.RobotStrategyPattern.TalkingStrategy.TalkingStrategy;
import com.lld.RobotStrategyPattern.TalkingStrategy.impl.NonTalkingStrategy;
import com.lld.RobotStrategyPattern.TalkingStrategy.impl.NormalTakingStrategy;

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
