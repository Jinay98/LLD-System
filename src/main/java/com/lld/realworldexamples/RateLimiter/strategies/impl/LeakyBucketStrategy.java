package com.lld.realworldexamples.RateLimiter.strategies.impl;

import com.lld.realworldexamples.RateLimiter.strategies.RateLimitingStrategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Leaky Bucket: requests fill a queue; the queue drains at a fixed rate.
 * If the queue is full when a request arrives, it is rejected.
 *
 * Analogy: a bucket with a small hole at the bottom. Water (requests) pours
 * in from the top at any rate but leaks out at a constant rate. If the bucket
 * overflows, water (requests) spills over and is lost.
 *
 * Key property: output rate is always constant, so this algorithm smooths out
 * bursts — unlike Token Bucket, which allows bursts up to the bucket capacity.
 */
public class LeakyBucketStrategy implements RateLimitingStrategy {

    private final int capacity;             // max queue size (bucket volume)
    private final double leakRatePerSecond; // requests drained per second
    private final Map<String, LeakyBucket> userBuckets = new ConcurrentHashMap<>();

    public LeakyBucketStrategy(int capacity, double leakRatePerSecond) {
        this.capacity = capacity;
        this.leakRatePerSecond = leakRatePerSecond;
    }

    @Override
    public boolean allowRequest(String userId) {
        long currentTime = System.currentTimeMillis();
        userBuckets.putIfAbsent(userId, new LeakyBucket(capacity, currentTime));
        LeakyBucket bucket = userBuckets.get(userId);

        synchronized (bucket) {
            // Step 1: drain the bucket based on how much time has passed
            long elapsedMillis = currentTime - bucket.lastLeakTimestamp;
            long leaked = (long) (elapsedMillis / 1000.0 * leakRatePerSecond);
            if (leaked > 0) {
                bucket.queueSize = Math.max(0, bucket.queueSize - leaked);
                bucket.lastLeakTimestamp = currentTime;
            }

            // Step 2: try to add the new request to the queue
            if (bucket.queueSize < bucket.capacity) {
                bucket.queueSize++;
                return true;
            }
            return false;
        }
    }

    private static class LeakyBucket {
        final int capacity;
        long queueSize;
        long lastLeakTimestamp;

        LeakyBucket(int capacity, long now) {
            this.capacity = capacity;
            this.queueSize = 0;
            this.lastLeakTimestamp = now;
        }
    }
}
