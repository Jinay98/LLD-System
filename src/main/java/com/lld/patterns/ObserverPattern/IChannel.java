package com.lld.patterns.ObserverPattern;

public interface IChannel {
     void notifyAllSubscribers(String videoName);
     void addSubscription(Subscriber subscriber);
     void removeSubscription(String subName);
     void uploadNewVideo(String newVideoName);
}
