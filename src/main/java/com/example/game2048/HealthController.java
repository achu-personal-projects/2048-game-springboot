package com.example.game2048;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController implements HealthIndicator {

    @GetMapping("/health")
    public Health health() {
        return Health.up().build();
    }

    @GetMapping("/liveness")
    public Health liveness() {
        // Add custom checks here
        return Health.up().build();
    }

    @GetMapping("/readiness")
    public Health readiness() {
        // Add custom checks here
        return Health.up().build();
    }
}
