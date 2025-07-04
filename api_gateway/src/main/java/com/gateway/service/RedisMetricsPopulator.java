package com.gateway.service;

import com.gateway.entity.ApiMetric;
import com.gateway.repository.ApiMetricRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class RedisMetricsPopulator {

    private final ApiMetricRepository metricRepository;
    private final MetricsAggregator metricsAggregator;

    public RedisMetricsPopulator(ApiMetricRepository metricRepository,
            MetricsAggregator metricsAggregator) {
        this.metricRepository = metricRepository;
        this.metricsAggregator = metricsAggregator;
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    public void populateRedisMetrics() {
        // Get metrics from the last 5 minutes
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        log.debug("Populating Redis metrics with data since {}", cutoff);

        metricRepository.findByTimestampGreaterThan(cutoff)
                .groupBy(ApiMetric::getToService) // Group by service
                .flatMap(serviceGroup -> {
                    String serviceId = serviceGroup.key();
                    if (serviceId == null || serviceId.isEmpty()) {
                        return Flux.empty();
                    }

                    log.debug("Processing metrics for service: {}", serviceId);

                    return serviceGroup.collectList()
                            .map(metrics -> {
                                // Calculate service metrics
                                Map<String, Double> serviceMetrics = calculateServiceMetrics(metrics);

                                // Store in Redis
                                metricsAggregator.addMetrics(serviceId, serviceMetrics);

                                // Also store by route ID
                                metrics.stream()
                                        .filter(m -> m.getRouteId() != null && !m.getRouteId().isEmpty())
                                        .forEach(metric -> {
                                            Map<String, Double> routeMetrics = new HashMap<>();
                                            routeMetrics.put("duration", (double) metric.getDuration());
                                            routeMetrics.put("statusCode", (double) metric.getStatusCode());
                                            routeMetrics.put("success", metric.isSuccess() ? 1.0 : 0.0);

                                            metricsAggregator.addMetrics("route:" + metric.getRouteId(), routeMetrics);
                                        });

                                return serviceMetrics;
                            });
                })
                .subscribe(
                        metrics -> log.debug("Successfully populated Redis metrics: {}", metrics),
                        error -> log.error("Error populating Redis metrics: {}", error.getMessage()));
    }

    private Map<String, Double> calculateServiceMetrics(java.util.List<ApiMetric> metrics) {
        Map<String, Double> serviceMetrics = new HashMap<>();

        // Calculate average duration
        double avgDuration = metrics.stream()
                .mapToLong(ApiMetric::getDuration)
                .average()
                .orElse(0);
        serviceMetrics.put("avgDuration", avgDuration);

        // Calculate success rate
        double successCount = metrics.stream()
                .filter(ApiMetric::isSuccess)
                .count();
        double successRate = metrics.isEmpty() ? 1.0 : successCount / metrics.size();
        serviceMetrics.put("successRate", successRate);

        // Add request count
        serviceMetrics.put("requestCount", (double) metrics.size());

        return serviceMetrics;
    }
}