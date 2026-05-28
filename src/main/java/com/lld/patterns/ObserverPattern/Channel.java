package com.lld.patterns.ObserverPattern;

import java.util.ArrayList;
import java.util.List;

public class Channel implements IChannel {
    List<Subscriber> subscribers = new ArrayList<>();
    private String channelName;
    private String latestVideoName;

    public Channel(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getLatestVideoName() {
        return latestVideoName;
    }

    public void setLatestVideoName(String latestVideoName) {
        this.latestVideoName = latestVideoName;
    }

    @Override
    public void notifyAllSubscribers(String videoName) {
        for (Subscriber subscriber : subscribers) {
            subscriber.update(this);
        }
    }

    public void uploadNewVideo(String newVideoName) {
        latestVideoName = newVideoName;
        System.out.println("Uploading new video:" + newVideoName);
        notifyAllSubscribers(newVideoName);
    }

    public void addSubscription(Subscriber subscriber) {
        subscribers.add(subscriber);
    }

    public void removeSubscription(String subName) {
        subscribers = subscribers.stream().filter(subscriber -> !subscriber.getName().
                equalsIgnoreCase(subName)).toList();
    }
}
