package com.lld.patterns.ObserverPattern;

public class Client {
    public static void main(String[] args) {
        IChannel channel = new Channel("AIB");
        Subscriber subscriber1 = new Subscriber("Ram");
        Subscriber subscriber2 = new Subscriber("Shyam");
        channel.addSubscription(subscriber1);
        channel.addSubscription(subscriber2);
        channel.uploadNewVideo("First video");

        channel.removeSubscription("Ram");
        channel.uploadNewVideo("Second Video");
    }
}
