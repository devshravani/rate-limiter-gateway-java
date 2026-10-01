package com.ratelimiter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CircuitBreakerServiceTest {
    
    @Test 
    public void testCircuitBreakerOpenAfterFiveFailure()
    {
        CircuitBreakerService circuitBreakerService = new CircuitBreakerService();

        //Initially the circuit should be closed
        assertTrue(circuitBreakerService.allowRequest());

        //record 5 failure
        for(int i=1; i<=5; i++)
        {
            circuitBreakerService.recordFailure();
        }
        //circuit should now open
        assertFalse(circuitBreakerService.allowRequest());

        //check the actual state
        assertEquals("OPEN", circuitBreakerService.getState());
    }
}
