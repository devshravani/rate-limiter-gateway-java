package com.ratelimiter.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private final Map<String, TokenBucket> clientBucket =
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

    // Default configuration for unknown users
    @Value("${rate-limit.default.capacity:5}")
    private double defaultCapacity = 5;

    @Value("${rate-limit.default.refill-rate:0.083}")
    private double defaultRefillRate = 0.083;

    // Client IDs loaded from application.properties
    @Value("${rate-limit.clients.free-user:free_user_1}")
    private String freeUserClientId = "free_user_1";

    @Value("${rate-limit.clients.paid-user:paid_user_1}")
    private String paidUserClientId = "paid_user_1";


    // Check whether a request is allowed
    public boolean allowRequest(String clientId) {

        TokenBucket bucket = getOrCreateBucket(clientId);

        return bucket.allowRequest();
    }


    // Get the number of remaining tokens
    public int getRemainingTokens(String clientId) {

        TokenBucket bucket = getOrCreateBucket(clientId);

        return bucket.getRemainingTokens();
    }


    // Get the maximum token capacity
    public int getCapacity(String clientId) {

        TokenBucket bucket = getOrCreateBucket(clientId);

        return bucket.getCapacity();
    }


    // Find or create the correct token bucket
    private TokenBucket getOrCreateBucket(String clientId) {

        return clientBucket.computeIfAbsent(
                clientId,
                id -> {

                    // Free user
                    if (freeUserClientId.equals(id)) {

                        return new TokenBucket(
                                freeCapacity,
                                freeRefillRate
                        );
                    }

                    // Paid user
                    if (paidUserClientId.equals(id)) {

                        return new TokenBucket(
                                paidCapacity,
                                paidRefillRate
                        );
                    }

                    // Unknown user receives the default limit
                    return new TokenBucket(
                            defaultCapacity,
                            defaultRefillRate
                    );
                }
        );
    }
}
