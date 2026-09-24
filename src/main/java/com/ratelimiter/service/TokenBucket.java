package com.ratelimiter.service;

public class TokenBucket {
    
    private double capacity;
    private double tokens;

    private double refillRate;
    private long lastRefillTime;


    public TokenBucket(double capacity, double refillRate)
    {
        this.capacity= capacity;
        this.tokens= capacity;
        this.refillRate = refillRate;
        this.lastRefillTime= System.currentTimeMillis();   
    }

    public synchronized boolean allowRequest()
    {
        refillTokens();

        if(tokens > 0)
        {
            tokens--;
            return true;
        }
        return false;   
    }
    private void refillTokens()
    {
        long currentTime= System.currentTimeMillis();
        long timePassed= currentTime - lastRefillTime;
        double secondPassed= timePassed /1000.0;
        double newTokens =secondPassed * refillRate;
        tokens=Math.min(capacity, tokens + newTokens );
        lastRefillTime =currentTime;
    }
}
