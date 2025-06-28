package com.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RedisRateLimiterConfig {

    @Bean
    @Primary
    public RedisRateLimiter redisRateLimiter() {
        // Create RedisRateLimiter with default values (replenishRate, burstCapacity,
        // requestedTokens)
        // These values can be overridden in specific routes
        return new RedisRateLimiter(5, 10, 1);
    }

    @Bean
    public KeyResolver defaultKeyResolver() {
        // Use path as the default key for rate limiting
        return exchange -> Mono.just(
                exchange.getRequest().getPath().value());
    }
}