package com.gateway.dto;

import com.gateway.model.CircuitBreakerConfig;
import com.gateway.model.RateLimiterConfig;
import com.gateway.model.RetryConfig;
import com.gateway.model.TimeoutConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDTO {
    private String id;
    private String routeId;
    private String path;
    private String method;
    private String uri;
    private Integer order;
    private Map<String, Object> metadata;
    private Map<String, String> filters;
    private boolean enabled;

    // Resilience configurations
    private CircuitBreakerConfig circuitBreaker;
    private RateLimiterConfig rateLimiter;
    private TimeoutConfig timeout;
    private RetryConfig retry;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
