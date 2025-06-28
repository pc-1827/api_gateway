package com.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreaker;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component("CircuitBreaker") // <-- Add this name
@Slf4j
public class CircuitBreakerGatewayFilterFactory
        extends AbstractGatewayFilterFactory<CircuitBreakerGatewayFilterFactory.Config>
        implements Ordered {

    private ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory;
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    public CircuitBreakerGatewayFilterFactory(
            ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        super(Config.class);
        this.circuitBreakerFactory = circuitBreakerFactory;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("name", "fallbackUri");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            String routeId = route != null ? route.getId() : "unknown";
            String cbName = config.getName() != null ? config.getName() : routeId;

            configureCircuitBreaker(cbName, config);

            // Get ReactiveCircuitBreaker instead of CircuitBreaker
            ReactiveResilience4JCircuitBreaker reactiveCircuitBreaker = (ReactiveResilience4JCircuitBreaker) circuitBreakerFactory
                    .create(cbName);

            // Execute with the reactive circuit breaker
            return reactiveCircuitBreaker.run(
                    chain.filter(exchange).onErrorResume(t -> {
                        log.error("Error during request execution: {}", t.getMessage());
                        return handleFallback(exchange, t, config.getFallbackUri());
                    }),
                    throwable -> {
                        log.error("Circuit breaker triggered: {}", throwable.getMessage());
                        return handleFallback(exchange, throwable, config.getFallbackUri());
                    });
        };
    }

    private void configureCircuitBreaker(String cbName, Config config) {
        boolean circuitBreakerExists = false;
        for (CircuitBreaker cb : circuitBreakerRegistry.getAllCircuitBreakers()) {
            if (cb.getName().equals(cbName)) {
                circuitBreakerExists = true;
                break;
            }
        }

        if (!circuitBreakerExists) {
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.Builder builder = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
                    .custom()
                    .slidingWindowSize(config.getSlidingWindowSize())
                    .failureRateThreshold(config.getFailureRateThreshold())
                    .waitDurationInOpenState(Duration.parse(config.getWaitDurationInOpenState()))
                    .permittedNumberOfCallsInHalfOpenState(config.getPermittedCallsInHalfOpenState())
                    .automaticTransitionFromOpenToHalfOpenEnabled(config.getAutomaticTransition());

            TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                    .timeoutDuration(Duration.ofSeconds(30))
                    .cancelRunningFuture(true)
                    .build();

            circuitBreakerFactory.configure(factoryBuilder -> factoryBuilder
                    .circuitBreakerConfig(builder.build())
                    .timeLimiterConfig(timeLimiterConfig),
                    cbName);
        }
    }

    private Mono<Void> handleFallback(ServerWebExchange exchange, Throwable throwable, String fallbackUri) {
        if (fallbackUri != null) {
            URI uri = exchange.getRequest().getURI();
            String query = uri.getQuery() == null ? "" : "?" + uri.getQuery();
            String path = fallbackUri + query;

            log.info("Redirecting to fallback: {}", path);

            // Create a new request with the fallback path
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .path(path)
                    .build();

            // Create a new exchange with the modified request
            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(request)
                    .build();

            // Return a 503 Service Unavailable status to indicate circuit breaker
            // activation
            exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);

            // Redirect to the fallback endpoint
            return exchange.getResponse().setComplete();
        }

        // If no fallback URI is provided, just return a 503 error
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return 1000; // Execute after other filters
    }

    public static class Config {
        private String name;
        private Integer slidingWindowSize = 100;
        private Float failureRateThreshold = 50f;
        private String waitDurationInOpenState = "PT10S"; // 10 seconds in ISO-8601 format
        private Integer permittedCallsInHalfOpenState = 10;
        private String fallbackUri;
        private Boolean automaticTransition = true;

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getSlidingWindowSize() {
            return slidingWindowSize;
        }

        public void setSlidingWindowSize(Integer slidingWindowSize) {
            this.slidingWindowSize = slidingWindowSize;
        }

        public Float getFailureRateThreshold() {
            return failureRateThreshold;
        }

        public void setFailureRateThreshold(Float failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
        }

        public String getWaitDurationInOpenState() {
            return waitDurationInOpenState;
        }

        public void setWaitDurationInOpenState(String waitDurationInOpenState) {
            this.waitDurationInOpenState = waitDurationInOpenState;
        }

        public Integer getPermittedCallsInHalfOpenState() {
            return permittedCallsInHalfOpenState;
        }

        public void setPermittedCallsInHalfOpenState(Integer permittedCallsInHalfOpenState) {
            this.permittedCallsInHalfOpenState = permittedCallsInHalfOpenState;
        }

        public String getFallbackUri() {
            return fallbackUri;
        }

        public void setFallbackUri(String fallbackUri) {
            this.fallbackUri = fallbackUri;
        }

        public Boolean getAutomaticTransition() {
            return automaticTransition;
        }

        public void setAutomaticTransition(Boolean automaticTransition) {
            this.automaticTransition = automaticTransition;
        }
    }
}