package com.lld.realworldexamples.RateLimiter.strategies;

public interface RateLimitingStrategy {
    boolean allowRequest(String userId);
}
