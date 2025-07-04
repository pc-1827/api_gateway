package com.gateway.repository;

import com.gateway.entity.ApiMetric;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public interface ApiMetricRepository extends ReactiveMongoRepository<ApiMetric, String> {

    // Find metrics by services
    @Query("{ $or: [ { 'fromService': ?0 }, { 'toService': ?0 } ] }")
    Flux<ApiMetric> findByFromServiceOrToService(String serviceName);

    // Find metrics by time range and services
    @Query("{ 'timestamp': { $gte: ?0, $lte: ?1 }, $or: [ { 'fromService': { $regex: ?2, $options: 'i' } }, { 'toService': { $regex: ?3, $options: 'i' } } ] }")
    Flux<ApiMetric> findByTimestampBetweenAndServices(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String fromService,
            String toService);

    // Calculate average response time
    @Aggregation(pipeline = {
            "{ $match: { 'timestamp': { $gte: ?0 } } }",
            "{ $group: { _id: null, avgDuration: { $avg: '$duration' } } }"
    })
    Mono<Double> getAverageResponseTime(Instant cutoff);

    // Calculate success rate
    @Aggregation(pipeline = {
            "{ $match: { 'timestamp': { $gte: ?0 } } }",
            "{ $group: { _id: null, successCount: { $sum: { $cond: ['$success', 1, 0] } }, total: { $sum: 1 } } }",
            "{ $project: { _id: 0, successRate: { $divide: ['$successCount', '$total'] } } }"
    })
    Mono<Double> getSuccessRate(Instant cutoff);

    // Get requests per minute
    @Aggregation(pipeline = {
            "{ $match: { 'timestamp': { $gte: ?0 } } }",
            "{ $group: { _id: null, count: { $sum: 1 }, minTimestamp: { $min: '$timestamp' }, maxTimestamp: { $max: '$timestamp' } } }",
            "{ $project: { _id: 0, requestsPerMinute: { $cond: [ { $eq: [{ $subtract: ['$maxTimestamp', '$minTimestamp'] }, 0] }, 0, { $divide: [ { $multiply: ['$count', 60000] }, { $subtract: ['$maxTimestamp', '$minTimestamp'] } ] } ] } } }"
    })
    Mono<Double> getRequestsPerMinute(Instant cutoff);

    // Get top endpoints by usage
    @Aggregation(pipeline = {
            "{ $match: { 'timestamp': { $gte: ?0 } } }",
            "{ $group: { _id: '$path', count: { $sum: 1 }, avgDuration: { $avg: '$duration' } } }",
            "{ $sort: { 'count': -1 } }",
            "{ $limit: 10 }"
    })
    Flux<TopEndpointDTO> getTopEndpoints(Instant cutoff);

    // Get endpoint performance stats
    @Aggregation(pipeline = {
            "{ $match: { 'path': ?0, 'timestamp': { $gte: ?1 } } }",
            "{ $group: { _id: null, min: { $min: '$duration' }, max: { $max: '$duration' }, avg: { $avg: '$duration' }, count: { $sum: 1 } } }"
    })
    Mono<EndpointStatsDTO> getEndpointStats(String path, Instant cutoff);

    // Add this method
    @Query("{ 'timestamp': { $gte: ?0 } }")
    Flux<ApiMetric> findByTimestampGreaterThan(LocalDateTime timestamp);

    interface TopEndpointDTO {
        String getId();

        long getCount();

        double getAvgDuration();
    }

    interface EndpointStatsDTO {
        long getMin();

        long getMax();

        double getAvg();

        long getCount();
    }
}