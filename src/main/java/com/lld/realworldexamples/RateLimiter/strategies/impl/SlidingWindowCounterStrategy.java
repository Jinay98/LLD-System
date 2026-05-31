package com.lld.realworldexamples.RateLimiter.strategies.impl;

import com.lld.realworldexamples.RateLimiter.strategies.RateLimitingStrategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding Window Counter: approximates the sliding window using two fixed-window
 * buckets — the current window and the previous window.
 *
 * Formula:
 *   estimatedCount = previousWindowCount * (1 - elapsedFraction) + currentWindowCount
 *
 * where elapsedFraction = how far we are into the current window (0.0 → 1.0).
 *
 * Example (windowSize=10s, max=10):
 *   previousWindow [0s-10s]: 8 requests
 *   currentWindow  [10s-20s]: 3 requests
 *   current time = 12s → elapsedFraction = 2/10 = 0.2
 *   estimated = 8 * (1 - 0.2) + 3 = 8 * 0.8 + 3 = 6.4 + 3 = 9.4 → allow
 *
 * Advantage over Log: O(1) memory per user regardless of request volume.
 * Trade-off: the count is an approximation (assumes uniform distribution in prev window).
 */
public class SlidingWindowCounterStrategy implements RateLimitingStrategy {

    private final int maxRequests;
    private final long windowSizeInMillis;
    private final Map<String, WindowData> userWindows = new ConcurrentHashMap<>();

    public SlidingWindowCounterStrategy(int maxRequests, long windowSizeInSeconds) {
        this.maxRequests = maxRequests;
        this.windowSizeInMillis = windowSizeInSeconds * 1000;
    }

    @Override
    public boolean allowRequest(String userId) {
        long currentTime = System.currentTimeMillis();
        userWindows.putIfAbsent(userId, new WindowData(currentTime, windowSizeInMillis));
        WindowData data = userWindows.get(userId);

        synchronized (data) {
            // Advance the window if we've moved past the current window boundary
            long windowsPassed = (currentTime - data.currentWindowStart) / windowSizeInMillis;
            if (windowsPassed >= 2) {
                // More than two windows have elapsed; previous window is irrelevant
                data.previousCount = 0;
                data.currentCount = 0;
                data.currentWindowStart = (currentTime / windowSizeInMillis) * windowSizeInMillis;
            } else if (windowsPassed == 1) {
                // Moved into the next window; current becomes previous
                data.previousCount = data.currentCount;
                data.currentCount = 0;
                data.currentWindowStart += windowSizeInMillis;
            }

            // How far into the current window are we (0.0 to 1.0)
            double elapsedFraction = (double) (currentTime - data.currentWindowStart) / windowSizeInMillis;

            // Weighted estimate: previous window's requests that are still "in" our sliding window
            double estimated = data.previousCount * (1.0 - elapsedFraction) + data.currentCount;

            if (estimated < maxRequests) {
                data.currentCount++;
                return true;
            }
            return false;
        }
    }

    private static class WindowData {
        long currentWindowStart;
        int previousCount;
        int currentCount;

        WindowData(long now, long windowSizeInMillis) {
            // Align window start to a clean boundary (e.g., starts at 0, 10s, 20s, ...)
            this.currentWindowStart = (now / windowSizeInMillis) * windowSizeInMillis;
            this.previousCount = 0;
            this.currentCount = 0;
        }
    }
}
