package com.gateway.controller;

import com.gateway.entity.ApiMetric;
import com.gateway.service.ApiMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@Slf4j
public class ApiMetricsController {

    private final ApiMetricsService apiMetricsService;

    @GetMapping
    public Flux<ApiMetric> getMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String fromService,
            @RequestParam(required = false) String toService) {

        log.debug("Fetching metrics - startDate: {}, endDate: {}, fromService: {}, toService: {}",
                startDate, endDate, fromService, toService);

        return apiMetricsService.getMetrics(startDate, endDate, fromService, toService)
                .doOnError(error -> {
                    log.error("Error fetching metrics: {}", error.getMessage(), error);
                });
    }

    @GetMapping("/summary")
    public Mono<Map<String, Object>> getMetricsSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        return apiMetricsService.getMetricsSummary(startDate, endDate);
    }

    @GetMapping("/service-interactions")
    public Flux<Map<String, Object>> getServiceInteractions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        return apiMetricsService.getServiceInteractions(startDate, endDate);
    }

    @GetMapping("/top-endpoints")
    public Flux<Map<String, Object>> getTopEndpoints(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "10") int limit) {

        return apiMetricsService.getTopEndpoints(startDate, endDate, limit);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiMetric>> getMetricById(@PathVariable String id) {
        return apiMetricsService.getMetricById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/count")
    public Mono<Long> getMetricsCount() {
        return apiMetricsService.getMetricsCount();
    }

    @GetMapping("/service/{serviceName}")
    public Flux<ApiMetric> getMetricsByService(@PathVariable String serviceName) {
        return apiMetricsService.getMetricsByService(serviceName);
    }
}