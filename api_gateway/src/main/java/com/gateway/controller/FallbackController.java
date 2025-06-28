package com.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class FallbackController {

    @GetMapping("/fallback/{service}")
    public Mono<ResponseEntity<Map<String, Object>>> serviceFallback(
            @PathVariable String service,
            @RequestParam(required = false) String error) {

        log.warn("Fallback triggered for service: {}, error: {}", service, error);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Service temporarily unavailable");
        response.put("service", service);

        if (error != null && !error.isEmpty()) {
            response.put("error", error);
        }

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}