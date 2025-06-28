package com.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component("RedisRateLimiter")
@Slf4j
public class RedisRateLimiterGatewayFilterFactory
        extends AbstractGatewayFilterFactory<RedisRateLimiterGatewayFilterFactory.Config>
        implements Ordered {

    private final ReactiveStringRedisTemplate redisTemplate;

    @Autowired
    public RedisRateLimiterGatewayFilterFactory(ReactiveStringRedisTemplate redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("replenishRate", "burstCapacity", "requestedTokens");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            String routeId = route != null ? route.getId() : "unknown";

            log.debug("Applying rate limiter for route: {}", routeId);

            // Fix: Use execute() and convert Flux to Mono using next()
            return redisTemplate.execute(connection -> connection.ping())
                    .next() // Convert Flux to Mono by taking the first element
                    .defaultIfEmpty("FAILED")
                    .flatMap(pong -> {
                        if (!"PONG".equals(pong)) {
                            // Log but allow request if Redis is unavailable
                            log.warn("Redis unavailable for rate limiting - allowing request");
                            return chain.filter(exchange);
                        }

                        // Create rate limiter key based on route ID and path
                        String clientIp = Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                                .getAddress().getHostAddress();
                        String key = "rate-limit:" + routeId + ":" + clientIp;

                        // Simple token bucket algorithm using Redis
                        return redisTemplate.opsForValue().increment(key)
                                .flatMap(count -> {
                                    // Set key expiration for first request
                                    if (count == 1) {
                                        return redisTemplate.expire(key, java.time.Duration.ofSeconds(1))
                                                .then(Mono.just(count));
                                    }
                                    return Mono.just(count);
                                })
                                .flatMap(count -> {
                                    log.debug("Rate limit count for {}: {}", key, count);

                                    // If count exceeds burst capacity, reject
                                    if (count > config.getBurstCapacity()) {
                                        log.debug("Rate limit exceeded for {}: {} > {}",
                                                key, count, config.getBurstCapacity());
                                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                        return exchange.getResponse().setComplete();
                                    }

                                    // Otherwise proceed with request
                                    return chain.filter(exchange);
                                })
                                .onErrorResume(e -> {
                                    log.error("Error in rate limiting: {}", e.getMessage());
                                    // Allow request on error
                                    return chain.filter(exchange);
                                });
                    });
        };
    }

    @Override
    public int getOrder() {
        return 100; // Execute before circuit breaker
    }

    // Config class
    public static class Config {
        private int replenishRate = 1;
        private int burstCapacity = 2;
        private int requestedTokens = 1;

        // Getters and setters
        public int getReplenishRate() {
            return replenishRate;
        }

        public void setReplenishRate(int replenishRate) {
            this.replenishRate = replenishRate;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }

        public int getRequestedTokens() {
            return requestedTokens;
        }

        public void setRequestedTokens(int requestedTokens) {
            this.requestedTokens = requestedTokens;
        }
    }
}