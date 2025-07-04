package com.gateway.controller;

import com.gateway.model.MetricAnalysis;
import com.gateway.model.MetricPoint;
import com.gateway.service.AdvancedMetricsAnalyzer;
import com.gateway.service.MetricsAggregator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.*;

@RestController
@RequestMapping("/metrics/analysis")
@Slf4j
public class MetricsAnalyzerController {

    private final AdvancedMetricsAnalyzer metricsAnalyzer;
    private final MetricsAggregator metricsAggregator;

    public MetricsAnalyzerController(
            AdvancedMetricsAnalyzer metricsAnalyzer,
            MetricsAggregator metricsAggregator) {
        this.metricsAnalyzer = metricsAnalyzer;
        this.metricsAggregator = metricsAggregator;
    }

    @GetMapping("/{serviceOrRouteId}")
    public Mono<ResponseEntity<Object>> getMetricAnalysis(
            @PathVariable String serviceOrRouteId,
            @RequestParam(required = false) String metric) {

        log.debug("Getting analysis for service/route {}, metric {}", serviceOrRouteId, metric);

        Map<String, List<MetricPoint>> history = metricsAggregator.getMetricsHistory(serviceOrRouteId);

        if (history.isEmpty()) {
            // Try with route prefix
            history = metricsAggregator.getMetricsHistory("route:" + serviceOrRouteId);
        }

        if (history.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "No metrics found for service/route: " + serviceOrRouteId);
            response.put("availableKeys", metricsAggregator.getAllMetricsKeys());
            return Mono.just(ResponseEntity.ok(response));
        }

        if (metric != null && !metric.isEmpty()) {
            // Analyze specific metric
            List<MetricPoint> points = history.getOrDefault(metric, Collections.emptyList());

            if (points.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Metric not found: " + metric);
                response.put("availableMetrics", history.keySet());
                return Mono.just(ResponseEntity.ok(response));
            }

            MetricAnalysis analysis = metricsAnalyzer.analyzeMetric(points);
            // Use the serializable map instead of the object directly
            return Mono.just(ResponseEntity.ok(analysis.toSerializableMap()));
        } else {
            // Analyze all metrics
            Map<String, Object> analyses = new HashMap<>();
            history.forEach((metricName, points) -> {
                MetricAnalysis analysis = metricsAnalyzer.analyzeMetric(points);
                // Use the serializable map for each analysis
                analyses.put(metricName, analysis.toSerializableMap());
            });

            return Mono.just(ResponseEntity.ok(analyses));
        }
    }

    @GetMapping("/{serviceOrRouteId}/recommendations")
    public Mono<ResponseEntity<Object>> getMetricRecommendations(
            @PathVariable String serviceOrRouteId,
            @RequestParam(required = false) String metric) {

        log.debug("Getting recommendations for service/route {}, metric {}", serviceOrRouteId, metric);

        Map<String, List<MetricPoint>> history = metricsAggregator.getMetricsHistory(serviceOrRouteId);

        if (history.isEmpty()) {
            // Try with route prefix
            history = metricsAggregator.getMetricsHistory("route:" + serviceOrRouteId);
        }

        if (history.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "No metrics found for service/route: " + serviceOrRouteId);
            response.put("availableKeys", metricsAggregator.getAllMetricsKeys());
            return Mono.just(ResponseEntity.ok(response));
        }

        if (metric != null && !metric.isEmpty()) {
            // Get recommendations for specific metric
            List<MetricPoint> points = history.getOrDefault(metric, Collections.emptyList());

            if (points.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Metric not found: " + metric);
                response.put("availableMetrics", history.keySet());
                return Mono.just(ResponseEntity.ok(response));
            }

            MetricAnalysis analysis = metricsAnalyzer.analyzeMetric(points);
            return Mono.just(ResponseEntity.ok(analysis.getRecommendations()));
        } else {
            // Get recommendations for all metrics
            Map<String, List<String>> allRecommendations = new HashMap<>();
            history.forEach((metricName, points) -> {
                MetricAnalysis analysis = metricsAnalyzer.analyzeMetric(points);
                allRecommendations.put(metricName, analysis.getRecommendations());
            });

            return Mono.just(ResponseEntity.ok(allRecommendations));
        }
    }
}