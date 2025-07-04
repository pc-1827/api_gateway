package com.gateway.controller;

import com.gateway.model.MetricPoint;
import com.gateway.service.MetricsAggregator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/metrics/points")
@Slf4j
public class MetricsPointController {

    private final MetricsAggregator metricsAggregator;

    public MetricsPointController(MetricsAggregator metricsAggregator) {
        this.metricsAggregator = metricsAggregator;
    }

    @GetMapping("/{serviceOrRouteId}/current")
    public Mono<ResponseEntity<Map<String, Object>>> getCurrentMetrics(@PathVariable String serviceOrRouteId) {
        log.debug("Getting current metrics for ID: {}", serviceOrRouteId);

        Map<String, Double> metrics = metricsAggregator.getCurrentMetrics(serviceOrRouteId);
        if (metrics.isEmpty()) {
            // Try with route prefix
            metrics = metricsAggregator.getCurrentMetrics("route:" + serviceOrRouteId);
        }

        if (metrics.isEmpty()) {
            log.debug("No metrics found for {}", serviceOrRouteId);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "No metrics found for service/route: " + serviceOrRouteId);
            response.put("availableKeys", metricsAggregator.getAllMetricsKeys());
            return Mono.just(ResponseEntity.ok(response));
        }

        Map<String, Object> response = new HashMap<>(metrics);
        return Mono.just(ResponseEntity.ok(response));
    }

    @GetMapping("/{serviceOrRouteId}/{metric}/history")
    public Mono<ResponseEntity<Object>> getMetricHistory(
            @PathVariable String serviceOrRouteId,
            @PathVariable String metric) {

        log.debug("Getting history for metric {} of {}", metric, serviceOrRouteId);

        Map<String, List<MetricPoint>> history = metricsAggregator.getMetricsHistory(serviceOrRouteId);
        List<MetricPoint> points = history.getOrDefault(metric, Collections.emptyList());

        if (points.isEmpty() && !history.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Metric not found: " + metric);
            response.put("availableMetrics", history.keySet());
            return Mono.just(ResponseEntity.ok(response));
        } else if (history.isEmpty()) {
            // Try with route prefix
            history = metricsAggregator.getMetricsHistory("route:" + serviceOrRouteId);
            points = history.getOrDefault(metric, Collections.emptyList());

            if (points.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "No metrics found for service/route: " + serviceOrRouteId);
                response.put("availableKeys", metricsAggregator.getAllMetricsKeys());
                return Mono.just(ResponseEntity.ok(response));
            }
        }

        return Mono.just(ResponseEntity.ok(points));
    }

    @GetMapping("/{serviceOrRouteId}")
    public Mono<ResponseEntity<Object>> getAllMetrics(@PathVariable String serviceOrRouteId) {
        log.debug("Getting all metrics for {}", serviceOrRouteId);

        Map<String, List<MetricPoint>> history = metricsAggregator.getMetricsHistory(serviceOrRouteId);

        if (history.isEmpty()) {
            // Try with route prefix
            history = metricsAggregator.getMetricsHistory("route:" + serviceOrRouteId);

            if (history.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "No metrics found for service/route: " + serviceOrRouteId);
                response.put("availableKeys", metricsAggregator.getAllMetricsKeys());
                return Mono.just(ResponseEntity.ok(response));
            }
        }

        return Mono.just(ResponseEntity.ok(history));
    }

    @GetMapping("/debug/keys")
    public Mono<ResponseEntity<List<String>>> getAvailableKeys() {
        return Mono.just(ResponseEntity.ok(metricsAggregator.getAllMetricsKeys()));
    }
}