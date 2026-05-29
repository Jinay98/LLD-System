package com.lld.realworldexamples.URLShortner.repository.impl;

import com.lld.realworldexamples.URLShortner.models.ShortenedURL;
import com.lld.realworldexamples.URLShortner.repository.URLRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryURLRepository implements URLRepository {
    private final Map<String, ShortenedURL> keyToUrlMap = new ConcurrentHashMap<>();
    private final Map<String, String> longUrlToKeyMap = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1); // Start from 1001

    @Override
    public void save(ShortenedURL url) {
        keyToUrlMap.put(url.getShortKey(), url);
        longUrlToKeyMap.put(url.getLongURL(), url.getShortKey());
    }

    @Override
    public Optional<ShortenedURL> findByKey(String key) {
        ShortenedURL url = keyToUrlMap.get(key);
        return Optional.ofNullable(url);
    }

    @Override
    public Optional<String> findKeyByLongURL(String longURL) {
        return Optional.ofNullable(longUrlToKeyMap.get(longURL));
    }

    @Override
    public long getNextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public boolean existsByKey(String key) {
        return keyToUrlMap.containsKey(key);
    }
}
