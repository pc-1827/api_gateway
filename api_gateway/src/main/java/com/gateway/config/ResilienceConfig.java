package com.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

@Configuration
public class ResilienceConfig {

    @Bean
    public ReactiveResilience4JCircuitBreakerFactory reactiveCircuitBreakerFactory(
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry) {
        // Use the non-deprecated constructor with configuration properties
        Resilience4JConfigurationProperties properties = new Resilience4JConfigurationProperties();
        return new ReactiveResilience4JCircuitBreakerFactory(
                circuitBreakerRegistry,
                timeLimiterRegistry,
                properties);
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        return TimeLimiterRegistry.ofDefaults();
    }

    @Bean
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public RedisScript<List<Long>> redisRequestRateLimiterScript() {
        // Using raw type to handle type conversion
        RedisScript script = RedisScript.of(
                "local tokens_key = KEYS[1]\n" +
                        "local timestamp_key = KEYS[2]\n" +
                        "local rate = tonumber(ARGV[1])\n" +
                        "local capacity = tonumber(ARGV[2])\n" +
                        "local now = tonumber(ARGV[3])\n" +
                        "local requested = tonumber(ARGV[4])\n" +
                        "local fill_time = capacity/rate\n" +
                        "local ttl = math.floor(fill_time*2)\n" +
                        "local last_tokens = tonumber(redis.call('get', tokens_key))\n" +
                        "if last_tokens == nil then\n" +
                        "  last_tokens = capacity\n" +
                        "end\n" +
                        "local last_refreshed = tonumber(redis.call('get', timestamp_key))\n" +
                        "if last_refreshed == nil then\n" +
                        "  last_refreshed = 0\n" +
                        "end\n" +
                        "local delta = math.max(0, now-last_refreshed)\n" +
                        "local filled_tokens = math.min(capacity, last_tokens+(delta*rate))\n" +
                        "local allowed = filled_tokens >= requested\n" +
                        "local new_tokens = filled_tokens\n" +
                        "if allowed then\n" +
                        "  new_tokens = filled_tokens-requested\n" +
                        "end\n" +
                        "redis.call('setex', tokens_key, ttl, new_tokens)\n" +
                        "redis.call('setex', timestamp_key, ttl, now)\n" +
                        "return { new_tokens, filled_tokens, capacity }",
                List.class);
        return script;
    }
}