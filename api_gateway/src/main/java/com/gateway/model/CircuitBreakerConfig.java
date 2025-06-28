package com.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CircuitBreakerConfig {
    private String name;
    private Integer slidingWindowSize;
    private Float failureRateThreshold;
    private String waitDurationInOpenState;
    private Integer permittedCallsInHalfOpenState;
    private String fallbackUri;
    private Boolean automaticTransition;
}