package com.ratelimiter.service;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service 
public class RateLimiterService {
    private Map<String, TokenBucket> clientBucket = new ConcurrentHashMap<>();
    public RateLimiterService()
    {
        clientBucket.put(
            "free_user_1",
            new TokenBucket(5, 0.083)
        );

        clientBucket.put(
            "paid_user_1",
         new TokenBucket(50,0.83)
        );
    }

    public boolean allowRequest(String clientId)
    {
        if(!clientBucket.containsKey(clientId))
        {
            clientBucket.put(clientId, new TokenBucket(5, 0.083));
        }
        TokenBucket bucket = clientBucket.computeIfAbsent(clientId, id -> new TokenBucket(5, 0.083));
        return bucket.allowRequest();
    }
}
 