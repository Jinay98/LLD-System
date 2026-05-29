package com.lld.realworldexamples.URLShortner.observer;

import com.lld.realworldexamples.URLShortner.enums.EventType;
import com.lld.realworldexamples.URLShortner.models.ShortenedURL;

public interface Observer {
    void update(EventType type, ShortenedURL url);
}
