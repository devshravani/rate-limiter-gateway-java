package com.ratelimiter.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class RateLimiterServiceTest {

    @Test
    public void testFreeUserRateLimit() {

        RateLimiterService rateLimiterService =
                new RateLimiterService();

        // First 5 requests should be allowed
        for (int i = 1; i <= 5; i++) {

            boolean allowed =
                    rateLimiterService.allowRequest("free_user_1");

            assertTrue(
                    allowed,
                    "Request " + i + " should be allowed"
            );
        }

        // 6th request should be rejected
        boolean sixthRequest =
                rateLimiterService.allowRequest("free_user_1");

        assertFalse(
                sixthRequest,
                "6th request should be rejected"
        );
    }

    @Test
    public void testPaidUserLimit()
    {
        RateLimiterService rateLimiterService = new RateLimiterService();

        //check that paid user have capacity of of 50
        int capacity = rateLimiterService.getCapacity("paid_user_1");

        assertTrue(capacity == 50, "Paid user capacity should be 50");

        //first req should be allowed
        boolean allowed = rateLimiterService.allowRequest("paid_user_1");       

        assertTrue(allowed, "Paid user req should be allowed");
    }
}