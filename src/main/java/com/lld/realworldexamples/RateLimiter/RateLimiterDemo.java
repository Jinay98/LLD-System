package com.lld.realworldexamples.RateLimiter;

import com.lld.realworldexamples.RateLimiter.strategies.RateLimitingStrategy;
import com.lld.realworldexamples.RateLimiter.strategies.impl.FixedWindowStrategy;
import com.lld.realworldexamples.RateLimiter.strategies.impl.LeakyBucketStrategy;
import com.lld.realworldexamples.RateLimiter.strategies.impl.SlidingWindowCounterStrategy;
import com.lld.realworldexamples.RateLimiter.strategies.impl.SlidingWindowLogStrategy;
import com.lld.realworldexamples.RateLimiter.strategies.impl.TokenBucketStrategy;

public class RateLimiterDemo {

    public static void main(String[] args) throws InterruptedException {
        String userId = "user123";

        System.out.println("=== Fixed Window (5 req / 10s) ===");
        runDemo(new FixedWindowStrategy(5, 10), userId, 8, 500);

        System.out.println("\n=== Token Bucket (capacity=5, refill=1/s) ===");
        runDemo(new TokenBucketStrategy(5, 1), userId, 8, 300);

        System.out.println("\n=== Leaky Bucket (capacity=5, leak=1/s) ===");
        runDemo(new LeakyBucketStrategy(5, 1.0), userId, 8, 300);

        System.out.println("\n=== Sliding Window Log (5 req / 10s) ===");
        runDemo(new SlidingWindowLogStrategy(5, 10), userId, 8, 500);

        System.out.println("\n=== Sliding Window Counter (5 req / 10s) ===");
        runDemo(new SlidingWindowCounterStrategy(5, 10), userId, 8, 500);
    }

    /**
     * Sends `totalRequests` requests one at a time, sleeping `delayMs` between each.
     * No threads needed — the sleep simulates time passing between requests.
     */
    private static void runDemo(RateLimitingStrategy strategy, String userId,
                                int totalRequests, long delayMs) throws InterruptedException {
        RateLimiterService service = new RateLimiterService(strategy);

        for (int i = 1; i <= totalRequests; i++) {
            boolean allowed = service.handleRequest(userId);
            System.out.printf("  req #%d  (+%dms elapsed) → %s%n",
                    i, (i - 1) * delayMs, allowed ? "ALLOWED" : "REJECTED");
            Thread.sleep(delayMs);
        }
    }
}
