package com.ratelimiter.controller;

import com.ratelimiter.model.ApiErrorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.ratelimiter.service.CircuitBreakerService;
import com.ratelimiter.service.RateLimiterService;

@RestController
public class GatewayController {

    private RateLimiterService rateLimiterService;
    private CircuitBreakerService circuitBreakerService;
    private RestTemplate restTemplate;

    @Value("${backend.service.base-url}")
private String backendBaseUrl;

    // Constructor injection
    public GatewayController(
            RateLimiterService rateLimiterService,
            CircuitBreakerService circuitBreakerService,
            RestTemplate restTemplate) {

        this.rateLimiterService = rateLimiterService;
        this.circuitBreakerService = circuitBreakerService;
        this.restTemplate = restTemplate;
    }

    // Main Gateway endpoint
    @GetMapping("/data")
    public ResponseEntity<?> getData(
            @RequestHeader(
                    value = "X-Client-Id",
                    defaultValue = "free_user_1"
            ) String clientId) {

        // Step 1: Check Rate Limiter
        boolean allowed = rateLimiterService.allowRequest(clientId);

        if (!allowed) 
        {

    ApiErrorResponse errorResponse =
            new ApiErrorResponse(
                    429,
                    "Too Many Requests",
                    "Rate limit exceeded. Please try again later."
            );

            return ResponseEntity
            .status(429)
            .body(errorResponse);
        }

        // Step 2: Check Circuit Breaker
        if (!circuitBreakerService.allowRequest()) {

    ApiErrorResponse errorResponse =
            new ApiErrorResponse(
                    503,
                    "Service Unavailable",
                    "Circuit Breaker is OPEN. Please try again later."
            );

    return ResponseEntity
            .status(503)
            .body(errorResponse);
}

        // Step 3: Call Backend Service
        try {

            String backendResponse =
        restTemplate.getForObject(
                backendBaseUrl + "/backend/data",
                String.class
        );

            // Backend request succeeded
            circuitBreakerService.recordSuccess();

            // Get rate limit information
            int remainingTokens =
                    rateLimiterService.getRemainingTokens(clientId);

            int capacity =
                    rateLimiterService.getCapacity(clientId);

            // Return backend response with rate limit headers
            return ResponseEntity
                    .ok()
                    .header(
                            "X-RateLimit-Limit",
                            String.valueOf(capacity)
                    )
                    .header(
                            "X-RateLimit-Remaining",
                            String.valueOf(remainingTokens)
                    )
                    .body(backendResponse);

        } catch (Exception e) {

    circuitBreakerService.recordFailure();

    ApiErrorResponse errorResponse =
            new ApiErrorResponse(
                    503,
                    "Service Unavailable",
                    "Backend service is unavailable."
            );

    return ResponseEntity
            .status(503)
            .body(errorResponse);
}
    }

    // Test failure manually
    @GetMapping("/test-failure")
    public String testFailure() {

        circuitBreakerService.recordFailure();

        return "Failure recorded";
    }

    // Check Circuit Breaker state
    @GetMapping("/status")
    public String getStatus() {

        return "Circuit Breaker State: "
                + circuitBreakerService.getState();
    }
}