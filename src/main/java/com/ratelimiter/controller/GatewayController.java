package com.ratelimiter.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.RestController;

import com.ratelimiter.service.RateLimiterService;
import com.ratelimiter.service.CircuitBreakerService;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController 

public class GatewayController {

  private RateLimiterService rateLimiterService = new RateLimiterService();
  private CircuitBreakerService circuitBreakerService = new CircuitBreakerService();

  private RestTemplate restTemplate= new RestTemplate();

  @GetMapping("/data")

  public ResponseEntity<String> getData( @RequestHeader (value = "X-Client-Id", defaultValue = "free_user_1")String clientId)
  {
    //step 1:check rate limiter
    boolean allowed = rateLimiterService.allowRequest(clientId);

    if(!allowed)
    {

      return ResponseEntity.status(429).body("Too many Request- Rate Limit Exceeded ");
    }
    //step 2:check circuit braker
    if(!circuitBreakerService.allowRequest())
    {
      return ResponseEntity.status(503).body("Service unavailable - Circuit Breaker is OPEN");
    }
    //step 3:request is allowed
    try
    {
    String backendResponse = restTemplate.getForObject("http://localhost:8081/backend/data", String.class);

    return ResponseEntity.ok(backendResponse);
    }
    catch(Exception e)
    {
      circuitBreakerService.recordFailure();
      return ResponseEntity.status(503).body("Backend Service Failed");
    }
  }
  @GetMapping("/test-failure")
  public String testFailure()
  {
    circuitBreakerService.recordFailure();
    return "Failure recorded";
  }

  @GetMapping("/status")
  public String getStatus()
  {
   return "Circuit Breaker State: " +circuitBreakerService.getState();
  }
     
}
