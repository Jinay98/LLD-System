package com.lld.realworldexamples.PubSubSystem;

public interface Subscriber {
    String getId();

    void onMessage(Message message);
}
