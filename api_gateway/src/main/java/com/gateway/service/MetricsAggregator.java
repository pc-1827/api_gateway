package com.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.model.MetricPoint;
import com.gateway.model.TrendAnalysis;
import com.gateway.model.TrendDirection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MetricsAggregator {
    private static final int DEFAULT_MAX_HISTORY_SIZE = 100;
    private static final String METRICS_KEY_PREFIX = "metrics:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public MetricsAggregator(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void addMetrics(String serviceId, Map<String, Double> newMetrics) {
        String redisKey = METRICS_KEY_PREFIX + serviceId;
        long timestamp = System.currentTimeMillis();

        newMetrics.forEach((newMetric, value) -> {
            try {
                // Get existing points
                String jsonPoints = (String) redisTemplate.opsForHash().get(redisKey, newMetric);
                List<MetricPoint> points = jsonPoints != null
                        ? objectMapper.readValue(jsonPoints, new TypeReference<List<MetricPoint>>() {
                        })
                        : new ArrayList<>();

                // Add new point
                points.add(new MetricPoint(newMetric, value, timestamp));

                // Keep only the latest points
                if (points.size() > DEFAULT_MAX_HISTORY_SIZE) {
                    points = points.subList(points.size() - DEFAULT_MAX_HISTORY_SIZE, points.size());
                }

                // Store updated points
                String updatedJson = objectMapper.writeValueAsString(points);
                redisTemplate.opsForHash().put(redisKey, newMetric, updatedJson);
            } catch (JsonProcessingException e) {
                log.error("Error storing new metrics for service {} metric {}: {}",
                        serviceId, newMetric, e.getMessage());
            }
        });
    }

    public Map<String, TrendAnalysis> analyzeTrends(String serviceId) {
        Map<String, TrendAnalysis> trends = new HashMap<>();
        String redisKey = METRICS_KEY_PREFIX + serviceId;

        Map<Object, Object> entries = redisTemplate.opsForHash().entries(redisKey);
        entries.forEach((metric, jsonPoints) -> {
            try {
                List<MetricPoint> points = objectMapper.readValue((String) jsonPoints,
                        new TypeReference<List<MetricPoint>>() {
                        });
                trends.put((String) metric, calculateTrend(points));
            } catch (JsonProcessingException e) {
                log.error("Error analyzing trends for service {} metric {}: {}",
                        serviceId, metric, e.getMessage());
            }
        });

        return trends;
    }

    private TrendAnalysis calculateTrend(List<MetricPoint> points) {
        if (points.size() < 2)
            return new TrendAnalysis(0.0, TrendDirection.STABLE);

        MetricPoint recentPoint = points.get(points.size() - 1);
        MetricPoint previousPoint = points.get(points.size() - 2);
        double recent = recentPoint.getValue();
        double previous = previousPoint.getValue();

        double percentageChange = ((recent - previous) / previous) * 100;

        TrendDirection direction = TrendDirection.STABLE;
        if (percentageChange > 5) {
            direction = TrendDirection.INCREASING;
        } else if (percentageChange < -5) {
            direction = TrendDirection.DECREASING;
        }

        return new TrendAnalysis(percentageChange, direction);
    }

    public Map<String, List<MetricPoint>> getMetricsHistory(String serviceId) {
        Map<String, List<MetricPoint>> history = new HashMap<>();
        String redisKey = METRICS_KEY_PREFIX + serviceId;

        Map<Object, Object> entries = redisTemplate.opsForHash().entries(redisKey);
        entries.forEach((metric, jsonPoints) -> {
            try {
                List<MetricPoint> points = objectMapper.readValue((String) jsonPoints,
                        new TypeReference<List<MetricPoint>>() {
                        });
                history.put((String) metric, points);
            } catch (JsonProcessingException e) {
                log.error("Error retrieving metrics history for service {} metric {}: {}",
                        serviceId, metric, e.getMessage());
            }
        });

        return history;
    }

    public void storeTrendAnalysis(String serviceId, Map<String, TrendAnalysis> trends) {
        try {
            String redisKey = METRICS_KEY_PREFIX + serviceId + ":trends";
            String jsonTrends = objectMapper.writeValueAsString(trends);
            redisTemplate.opsForValue().set(redisKey, jsonTrends);
        } catch (JsonProcessingException e) {
            log.error("Error storing trend analysis for service {}: {}", serviceId, e.getMessage());
        }
    }

    public Map<String, Double> getCurrentMetrics(String serviceId) {
        return getMetricsHistory(serviceId).entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get(e.getValue().size() - 1).getValue()));
    }

    public List<String> getAllMetricsKeys() {
        Set<String> keys = new HashSet<>();

        try {
            Set<String> redisKeys = redisTemplate.keys(METRICS_KEY_PREFIX + "*");

            if (redisKeys != null) {
                redisKeys.forEach(key -> {
                    String keyString = key;
                    // Extract service ID from the key pattern "metrics:serviceId"
                    if (keyString.startsWith(METRICS_KEY_PREFIX)) {
                        keys.add(keyString.substring(METRICS_KEY_PREFIX.length()));
                    }
                });
            }
        } catch (Exception e) {
            log.error("Error getting metrics keys: {}", e.getMessage());
        }

        return new ArrayList<>(keys);
    }
}