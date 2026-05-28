package com.lld.patterns.ObserverPattern;

public class Subscriber implements ISubscriber {
    private String name;

    public Subscriber(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public void update(Channel channel) {
        System.out.println(channel.getLatestVideoName() + " has been released by " + channel.getChannelName() + ", Notification has been sent to:" + name);

    }

}
