package com.gateway.service;

import com.gateway.repository.ApiMetricRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class MetricsCleanupService {

    private final ReactiveMongoTemplate mongoTemplate;
    private final ApiMetricRepository metricRepository;

    @Value("${metrics.collection.retention-days:30}")
    private int retentionDays;

    public MetricsCleanupService(ReactiveMongoTemplate mongoTemplate, 
                                ApiMetricRepository metricRepository) {
        this.mongoTemplate = mongoTemplate;
        this.metricRepository = metricRepository;
    }

    @Scheduled(cron = "0 0 0 * * ?")  // Run at midnight every day
    public void cleanupOldMetrics() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        log.info("Cleaning up metrics older than {}", cutoffDate);

        Query query = new Query();
        query.addCriteria(Criteria.where("timestamp").lt(cutoffDate));

        mongoTemplate.remove(query, "apiMetrics")
            .subscribe(
                result -> log.info("Removed {} old metrics", result.getDeletedCount()),
                error -> log.error("Error removing old metrics: {}", error.getMessage())
            );
    }
}




