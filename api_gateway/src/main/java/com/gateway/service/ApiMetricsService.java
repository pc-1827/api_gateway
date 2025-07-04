package com.gateway.service;

import com.gateway.entity.ApiMetric;
import com.gateway.repository.ApiMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiMetricsService {

    @SuppressWarnings("unchecked")
    private static final Class<Map<String, Object>> OUTPUT_TYPE = (Class<Map<String, Object>>) (Class<?>) Map.class;

    private final ApiMetricRepository apiMetricRepository;
    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public Mono<Void> saveMetric(ApiMetric metric) {
        // Skip saving if it's a health check request
        if (isHealthCheckRequest(metric)) {
            return Mono.empty();
        }
        return apiMetricRepository.save(metric).then();
    }

    private boolean isHealthCheckRequest(ApiMetric metric) {
        // Check if the path contains health endpoint
        return metric.getPath() != null &&
                (metric.getPath().endsWith("/health") ||
                        metric.getPath().endsWith("/health/"));
    }

    public Flux<ApiMetric> getMetrics(LocalDateTime startDate, LocalDateTime endDate,
            String fromService, String toService) {
        // Apply default date range if not provided
        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Normalize service names for query
        String fromServicePattern = fromService != null ? fromService : "";
        String toServicePattern = toService != null ? toService : "";

        return apiMetricRepository.findByTimestampBetweenAndServices(
                start, end, fromServicePattern, toServicePattern);
    }

    public Mono<Map<String, Object>> getMetricsSummary(LocalDateTime startDate, LocalDateTime endDate) {
        Criteria timeCriteria = new Criteria();
        if (startDate != null && endDate != null) {
            timeCriteria = Criteria.where("timestamp").gte(startDate).lte(endDate);
        }

        AggregationOperation match = Aggregation.match(timeCriteria);
        AggregationOperation group = Aggregation.group()
                .count().as("totalRequests")
                .sum("duration").as("totalDuration")
                .avg("duration").as("avgDuration")
                .max("duration").as("maxDuration")
                .min("duration").as("minDuration")
                .sum(ConditionalOperators.when(Criteria.where("success").is(true))
                        .then(1)
                        .otherwise(0))
                .as("successfulRequests")
                .sum(ConditionalOperators.when(Criteria.where("success").is(false))
                        .then(1)
                        .otherwise(0))
                .as("failedRequests");

        TypedAggregation<ApiMetric> aggregation = Aggregation.newAggregation(ApiMetric.class, match, group);
        return reactiveMongoTemplate.aggregate(aggregation, OUTPUT_TYPE)
                .next()
                .defaultIfEmpty(new HashMap<>());
    }

    public Flux<Map<String, Object>> getServiceInteractions(LocalDateTime startDate, LocalDateTime endDate) {
        Criteria timeCriteria = new Criteria();
        if (startDate != null && endDate != null) {
            timeCriteria = Criteria.where("timestamp").gte(startDate).lte(endDate);
        }

        AggregationOperation match = Aggregation.match(timeCriteria);
        AggregationOperation group = Aggregation.group("fromService", "toService")
                .count().as("count")
                .avg("duration").as("avgDuration")
                .sum("duration").as("totalDuration")
                .sum(ConditionalOperators.when(Criteria.where("success").is(true))
                        .then(1)
                        .otherwise(0))
                .as("successCount")
                .sum(ConditionalOperators.when(Criteria.where("success").is(false))
                        .then(1)
                        .otherwise(0))
                .as("failureCount");

        AggregationOperation sort = Aggregation.sort(Sort.Direction.DESC, "count");

        TypedAggregation<ApiMetric> aggregation = Aggregation.newAggregation(ApiMetric.class, match, group, sort);
        return reactiveMongoTemplate.aggregate(aggregation, OUTPUT_TYPE);
    }

    public Flux<Map<String, Object>> getTopEndpoints(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        Criteria timeCriteria = new Criteria();
        if (startDate != null && endDate != null) {
            timeCriteria = Criteria.where("timestamp").gte(startDate).lte(endDate);
        }

        AggregationOperation match = Aggregation.match(timeCriteria);
        AggregationOperation group = Aggregation.group("path")
                .count().as("count")
                .avg("duration").as("avgDuration")
                .addToSet("toService").as("services");

        AggregationOperation sort = Aggregation.sort(Sort.Direction.DESC, "count");
        AggregationOperation limitOp = Aggregation.limit(limit);

        TypedAggregation<ApiMetric> aggregation = Aggregation.newAggregation(ApiMetric.class, match, group, sort,
                limitOp);
        return reactiveMongoTemplate.aggregate(aggregation, OUTPUT_TYPE);
    }

    public Mono<ApiMetric> getMetricById(String id) {
        return apiMetricRepository.findById(id);
    }

    public Mono<Long> getMetricsCount() {
        return apiMetricRepository.count();
    }

    public Flux<ApiMetric> getMetricsByService(String serviceName) {
        return apiMetricRepository.findByFromServiceOrToService(serviceName);
    }
}