package com.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryConfig {
    private Integer maxAttempts;
    private String backoffDuration;
    private String retryableExceptions;
}