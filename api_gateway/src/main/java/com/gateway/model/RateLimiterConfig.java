package com.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimiterConfig {
    private Integer replenishRate;
    private Integer burstCapacity;
    private Integer requestedTokens;
}