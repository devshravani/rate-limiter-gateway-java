package com.ratelimiter.service;

import org.springframework.stereotype.Service;

@Service 
public class CircuitBreakerService {

    // Three possible states of the Circuit Breaker
    private enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private State state = State.CLOSED;

    // Circuit opens after 5 failures
    private int failureCount = 0;
    private int failureThreshold = 5;

    // Wait 10 seconds before trying HALF_OPEN
    private long openTime = 0;
    private long cooldownTime = 10_000;

    // Allows only one test request in HALF_OPEN state
    private boolean trialRequestInProgress = false;

    public synchronized boolean allowRequest() {

        // CLOSED state
        if (state == State.CLOSED) {
            return true;
        }

        // OPEN state
        if (state == State.OPEN) {

            long currentTime = System.currentTimeMillis();

            // Check whether 10 seconds have passed
            if (currentTime - openTime >= cooldownTime) {

                state = State.HALF_OPEN;
                trialRequestInProgress = false;

                System.out.println("Circuit Breaker HALF_OPEN");
            } else {
                return false;
            }
        }

        // HALF_OPEN state
        if (state == State.HALF_OPEN) {

            // Allow only one trial request
            if (!trialRequestInProgress) {

                trialRequestInProgress = true;

                return true;
            }

            return false;
        }

        return false;
    }

    public synchronized void recordFailure() {

        // HALF_OPEN trial failed
        if (state == State.HALF_OPEN) {

            state = State.OPEN;
            openTime = System.currentTimeMillis();
            trialRequestInProgress = false;

            System.out.println("Circuit Breaker OPEN again");

            return;
        }

        // CLOSED state failure
        failureCount++;

        System.out.println("Failure count = " + failureCount);

        if (failureCount >= failureThreshold) {

            state = State.OPEN;
            openTime = System.currentTimeMillis();

            System.out.println("Circuit Breaker OPEN");
        }
    }

    public synchronized void recordSuccess() {

        // Successful request closes the circuit
        if (state == State.HALF_OPEN) {

            state = State.CLOSED;
            failureCount = 0;
            trialRequestInProgress = false;

            System.out.println("Circuit Breaker CLOSED");
            return;
        }

        // Normal successful request
        failureCount = 0;
    }
    public synchronized String getState()
    {
        return state.toString();
    }
}