package com.ratelimiter.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private Map<String, TokenBucket> clientBucket =
            new ConcurrentHashMap<>();

    // Free user configuration
    @Value("${rate-limit.free.capacity:5}")
    private double freeCapacity = 5;

    @Value("${rate-limit.free.refill-rate:0.083}")
    private double freeRefillRate = 0.083;

    // Paid user configuration
    @Value("${rate-limit.paid.capacity:50}")
    private double paidCapacity = 50;

    @Value("${rate-limit.paid.refill-rate:0.83}")
    private double paidRefillRate = 0.83;

    // Default user configuration
    @Value("${rate-limit.default.capacity:5}")
    private double defaultCapacity = 5;

    @Value("${rate-limit.default.refill-rate:0.083}")
    private double defaultRefillRate = 0.083;


    public boolean allowRequest(String clientId) {

        TokenBucket bucket = getOrCreateBucket(clientId);

        return bucket.allowRequest();
    }


    public int getRemainingTokens(String clientId) {

        TokenBucket bucket = getOrCreateBucket(clientId);

        return bucket.getRemainingTokens();
    }


    public int getCapacity(String clientId) {

        TokenBucket bucket = getOrCreateBucket(clientId);

        return bucket.getCapacity();
    }


    private TokenBucket getOrCreateBucket(String clientId) {

        return clientBucket.computeIfAbsent(
                clientId,
                id -> {

                    // Paid user
                    if ("paid_user_1".equals(id)) {

                        return new TokenBucket(
                                paidCapacity,
                                paidRefillRate
                        );
                    }

                    // Free user
                    if ("free_user_1".equals(id)) {

                        return new TokenBucket(
                                freeCapacity,
                                freeRefillRate
                        );
                    }

                    // Unknown user gets default limit
                    return new TokenBucket(
                            defaultCapacity,
                            defaultRefillRate
                    );
                }
        );
    }
}