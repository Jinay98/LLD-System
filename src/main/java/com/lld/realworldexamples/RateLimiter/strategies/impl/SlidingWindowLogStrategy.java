package com.lld.realworldexamples.RateLimiter.strategies.impl;

import com.lld.realworldexamples.RateLimiter.strategies.RateLimitingStrategy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding Window Log: store the timestamp of every accepted request.
 * On each new request, evict timestamps older than (now - windowSize),
 * then check if remaining count is below the limit.
 *
 * Advantage over Fixed Window: no boundary burst problem.
 *   Fixed window allows 5 requests at t=9s and 5 more at t=11s (both within
 *   their own windows), giving 10 requests in 2 seconds. Sliding Window Log
 *   prevents this because it always looks at the last windowSize seconds.
 *
 * Disadvantage: memory grows with request volume (one entry per request).
 *   Use Sliding Window Counter if memory is a concern.
 */
public class SlidingWindowLogStrategy implements RateLimitingStrategy {

    private final int maxRequests;
    private final long windowSizeInMillis;
    private final Map<String, Deque<Long>> userTimestamps = new ConcurrentHashMap<>();

    public SlidingWindowLogStrategy(int maxRequests, long windowSizeInSeconds) {
        this.maxRequests = maxRequests;
        this.windowSizeInMillis = windowSizeInSeconds * 1000;
    }

    @Override
    public boolean allowRequest(String userId) {
        long currentTime = System.currentTimeMillis();
        userTimestamps.putIfAbsent(userId, new ArrayDeque<>());
        Deque<Long> timestamps = userTimestamps.get(userId);

        synchronized (timestamps) {
            long windowStart = currentTime - windowSizeInMillis;

            // Evict timestamps that have fallen outside the sliding window
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() < maxRequests) {
                timestamps.addLast(currentTime);
                return true;
            }
            return false;
        }
    }
}
