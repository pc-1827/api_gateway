package com.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class RedisHealthController {

    private final ReactiveStringRedisTemplate redisTemplate;

    @Autowired
    public RedisHealthController(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/api/redis-health")
    public Mono<ResponseEntity<Map<String, Object>>> checkRedisHealth() {
        // Fix: Convert Flux to Mono using next()
        return redisTemplate.execute(connection -> connection.ping())
                .next() // Convert Flux to Mono by taking the first element
                .defaultIfEmpty("FAILED")
                .flatMap(pong -> {
                    boolean pingSuccess = "PONG".equals(pong);

                    if (pingSuccess) {
                        // If ping successful, create the test key
                        return redisTemplate.opsForValue().set("rate-limiter-check", "ok", Duration.ofMinutes(1))
                                .then(redisTemplate.opsForValue().get("rate-limiter-check"))
                                .map(value -> {
                                    Map<String, Object> response = new HashMap<>();
                                    boolean isHealthy = "ok".equals(value);
                                    response.put("status", isHealthy ? "healthy" : "unhealthy");
                                    response.put("value", value);
                                    response.put("ping", "PONG");
                                    return ResponseEntity.ok(response);
                                });
                    } else {
                        // Ping failed
                        Map<String, Object> response = new HashMap<>();
                        response.put("status", "error");
                        response.put("message", "Redis ping failed");
                        response.put("ping", pong);
                        return Mono.just(ResponseEntity.status(500).body(response));
                    }
                })
                .onErrorResume(e -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "error");
                    response.put("message", e.getMessage());
                    return Mono.just(ResponseEntity.status(500).body(response));
                });
    }

    @GetMapping("/debug/redis-keys")
    public Mono<List<String>> getRedisKeys() {
        return redisTemplate.keys("*").collectList();
    }
}
