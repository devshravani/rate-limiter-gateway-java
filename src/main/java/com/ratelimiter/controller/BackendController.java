package com.ratelimiter.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
public class BackendController {

    // This controls whether the backend should fail
    private boolean failMode = false;

    // Normal backend endpoint
    @GetMapping("/backend/data")
    public String getBackendData() {

        // If failure mode is ON, simulate backend failure
        if (failMode) {
            throw new RuntimeException("Backend Service Failed");
        }

        // Normal response
        return "Hello! Data received from Backend Service.";
    }

    // Endpoint to turn failure mode ON
    @GetMapping("/backend/fail")
    public String enableFailure() {

        failMode = true;

        return "Backend failure mode ON";
    }

    //Endpoint to turn failure mode OFF
    @GetMapping("/backend/recover")
    public String disableFailure()
    {
        failMode = false;

        return "Backend failure mode OFF";
    }
    
}