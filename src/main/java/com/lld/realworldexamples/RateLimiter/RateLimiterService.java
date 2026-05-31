package com.lld.realworldexamples.RateLimiter;

import com.lld.realworldexamples.RateLimiter.strategies.RateLimitingStrategy;

public class RateLimiterService {

    private final RateLimitingStrategy rateLimitingStrategy;

    public RateLimiterService(RateLimitingStrategy rateLimitingStrategy) {
        this.rateLimitingStrategy = rateLimitingStrategy;
    }

    public boolean handleRequest(String userId) {
        return rateLimitingStrategy.allowRequest(userId);
    }
}
