package com.lld.realworldexamples.URLShortner.services;

import com.lld.realworldexamples.URLShortner.enums.EventType;
import com.lld.realworldexamples.URLShortner.models.ShortenedURL;
import com.lld.realworldexamples.URLShortner.observer.Observer;
import com.lld.realworldexamples.URLShortner.repository.URLRepository;
import com.lld.realworldexamples.URLShortner.strategies.KeyGenerationStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class URLShortenerService {
    private static final int MAX_RETRIES = 10;
    private static URLShortenerService INSTANCE = new URLShortenerService();
    private final List<Observer> observers = new ArrayList<>();
    private URLRepository urlRepository;
    private KeyGenerationStrategy keyGenerationStrategy;
    private String domain;

    // Private constructor for Singleton
    private URLShortenerService() {
    }

    public static synchronized URLShortenerService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new URLShortenerService();
        }
        return INSTANCE;
    }

    // Configure the service with dependencies (Dependency Injection)
    public void configure(String domain, URLRepository repository, KeyGenerationStrategy strategy) {
        this.domain = domain;
        this.urlRepository = repository;
        this.keyGenerationStrategy = strategy;
    }

    public String shorten(String longURL) {
        // Check if we've already shortened this URL
        Optional<String> existingKey = urlRepository.findKeyByLongURL(longURL);
        if (existingKey.isPresent()) {
            return domain + existingKey.get();
        }

        // Generate a new key, handling potential collisions
        String shortKey = generateUniqueKey();

        ShortenedURL shortenedURL = new ShortenedURL.Builder(longURL, shortKey).build();
        urlRepository.save(shortenedURL);

        notifyObservers(EventType.URL_CREATED, shortenedURL);

        return domain + shortKey;
    }

    private String generateUniqueKey() {
        for (int i = 0; i < MAX_RETRIES; i++) {
            // The ID is passed but may be ignored by some strategies (like random)
            String potentialKey = keyGenerationStrategy.generateKey(urlRepository.getNextId());
            if (!urlRepository.existsByKey(potentialKey)) {
                return potentialKey; // Found a unique key
            }
        }
        // If we reach here, we failed to generate a unique key after several attempts.
        throw new RuntimeException("Failed to generate a unique short key after " + MAX_RETRIES + " attempts.");
    }

    public Optional<String> resolve(String shortURL) {
        if (!shortURL.startsWith(domain)) {
            return Optional.empty();
        }
        String shortKey = shortURL.replace(domain, "");

        if (urlRepository.existsByKey(shortKey)) {
            ShortenedURL shortenedURL = urlRepository.findByKey(shortKey).get();
            notifyObservers(EventType.URL_ACCESSED, shortenedURL);
            return Optional.of(shortKey);
        }

        return Optional.empty();
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    public void notifyObservers(EventType type, ShortenedURL url) {
        for (Observer observer : observers) {
            observer.update(type, url);
        }
    }
}
