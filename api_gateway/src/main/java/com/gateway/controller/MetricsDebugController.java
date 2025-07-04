package com.gateway.controller;

import com.gateway.service.MetricsAggregator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/metrics/debug")
@RequiredArgsConstructor
@Slf4j
public class MetricsDebugController {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final MetricsAggregator metricsAggregator;

    @GetMapping("/redis-keys")
    public Mono<ResponseEntity<List<String>>> getRedisKeys() {
        return redisTemplate.keys("*")
                .collectList()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/available-metrics")
    public Mono<ResponseEntity<List<String>>> getAvailableMetrics() {
        return Mono.just(ResponseEntity.ok(metricsAggregator.getAllMetricsKeys()));
    }

    @GetMapping("/redis-entry/{key}")
    public Mono<ResponseEntity<Map<String, Object>>> getRedisEntry(@PathVariable String key) {
        return redisTemplate.opsForHash().entries(key)
                .collectMap(e -> e.getKey().toString(), e -> e.getValue().toString())
                .map(entries -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("key", key);
                    response.put("entries", entries);
                    return ResponseEntity.ok(response);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/status")
    public Mono<ResponseEntity<Map<String, Object>>> getMetricsStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("availableMetrics", metricsAggregator.getAllMetricsKeys());

        return redisTemplate.keys("*")
                .collectList()
                .map(keys -> {
                    status.put("redisKeys", keys);
                    status.put("status", "ok");
                    return ResponseEntity.ok(status);
                });
    }
}