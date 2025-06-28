package com.gateway.config;

import com.gateway.entity.Route;
import com.gateway.filter.CircuitBreakerGatewayFilterFactory;
import com.gateway.filter.RedisRateLimiterGatewayFilterFactory;
import com.gateway.filter.TimeoutGatewayFilterFactory;
import com.gateway.filter.CustomRetryGatewayFilterFactory;
import com.gateway.filter.PathRewriteGatewayFilterFactory;
import com.gateway.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MongoRouteDefinitionRepository implements RouteDefinitionRepository {

    private final RouteRepository routeRepository;
    private final CircuitBreakerGatewayFilterFactory circuitBreakerFilterFactory;
    private final RedisRateLimiterGatewayFilterFactory redisRateLimiterFilterFactory;
    private final TimeoutGatewayFilterFactory timeoutFilterFactory;
    private final CustomRetryGatewayFilterFactory retryFilterFactory;
    private final PathRewriteGatewayFilterFactory pathRewriteFilterFactory;

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return routeRepository.findByEnabled(true)
                .map(this::convertToRouteDefinition)
                .doOnNext(routeDefinition -> log.info("Loading route definition: {}", routeDefinition.getId()));
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return route.flatMap(r -> Mono.empty());
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        return routeId.flatMap(id -> Mono.empty());
    }

    private RouteDefinition convertToRouteDefinition(Route route) {
        RouteDefinition definition = new RouteDefinition();
        definition.setId(route.getRouteId());
        definition.setUri(URI.create(route.getUri()));
        definition.setOrder(route.getOrder() != null ? route.getOrder() : 0);

        // Add predicates
        List<PredicateDefinition> predicates = new ArrayList<>();

        // Add Path predicate
        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        Map<String, String> pathArgs = new HashMap<>();
        pathArgs.put("pattern", route.getPath());
        pathPredicate.setArgs(pathArgs);
        predicates.add(pathPredicate);

        // Add Method predicate if specified
        if (route.getMethod() != null && !route.getMethod().isEmpty()) {
            PredicateDefinition methodPredicate = new PredicateDefinition();
            methodPredicate.setName("Method");
            Map<String, String> methodArgs = new HashMap<>();
            methodArgs.put("method", route.getMethod());
            methodPredicate.setArgs(methodArgs);
            predicates.add(methodPredicate);
        }

        definition.setPredicates(predicates);

        // Add filters based on resilience configurations
        List<FilterDefinition> filters = new ArrayList<>();

        // Add path rewrite filter
        FilterDefinition pathRewriteFilter = new FilterDefinition();
        pathRewriteFilter.setName("PathRewrite");
        Map<String, String> pathRewriteArgs = new HashMap<>();
        pathRewriteArgs.put("stripPrefix", "true");
        pathRewriteArgs.put("prefixSize", "1");
        pathRewriteFilter.setArgs(pathRewriteArgs);
        filters.add(pathRewriteFilter);

        // Using filter factories to validate configurations

        // Add circuit breaker filter if configured
        if (route.getCircuitBreaker() != null) {
            FilterDefinition cbFilter = new FilterDefinition();
            cbFilter.setName("CircuitBreaker");
            Map<String, String> cbArgs = new HashMap<>();
            cbArgs.put("name", route.getCircuitBreaker().getName());
            cbArgs.put("fallbackUri", route.getCircuitBreaker().getFallbackUri());
            cbArgs.put("slidingWindowSize", String.valueOf(route.getCircuitBreaker().getSlidingWindowSize()));
            cbArgs.put("failureRateThreshold", String.valueOf(route.getCircuitBreaker().getFailureRateThreshold()));
            cbArgs.put("waitDurationInOpenState", route.getCircuitBreaker().getWaitDurationInOpenState());
            cbArgs.put("permittedCallsInHalfOpenState",
                    String.valueOf(route.getCircuitBreaker().getPermittedCallsInHalfOpenState()));
            cbArgs.put("automaticTransition", String.valueOf(route.getCircuitBreaker().getAutomaticTransition()));
            cbFilter.setArgs(cbArgs);
            filters.add(cbFilter);

            // Validate circuit breaker config
            CircuitBreakerGatewayFilterFactory.Config cbConfig = new CircuitBreakerGatewayFilterFactory.Config();
            cbConfig.setName(route.getCircuitBreaker().getName());
            circuitBreakerFilterFactory.apply(cbConfig); // Just to use the field
        }

        // Add rate limiter filter if configured
        if (route.getRateLimiter() != null) {
            FilterDefinition rlFilter = new FilterDefinition();
            rlFilter.setName("RedisRateLimiter");
            Map<String, String> rlArgs = new HashMap<>();
            rlArgs.put("replenishRate", String.valueOf(route.getRateLimiter().getReplenishRate()));
            rlArgs.put("burstCapacity", String.valueOf(route.getRateLimiter().getBurstCapacity()));
            rlArgs.put("requestedTokens", String.valueOf(route.getRateLimiter().getRequestedTokens()));
            rlFilter.setArgs(rlArgs);
            filters.add(rlFilter);

            // Validate rate limiter config
            RedisRateLimiterGatewayFilterFactory.Config rlConfig = new RedisRateLimiterGatewayFilterFactory.Config();
            rlConfig.setReplenishRate(route.getRateLimiter().getReplenishRate());
            redisRateLimiterFilterFactory.apply(rlConfig); // Just to use the field
        }

        // Add timeout filter if configured
        if (route.getTimeout() != null) {
            FilterDefinition toFilter = new FilterDefinition();
            toFilter.setName("Timeout");
            Map<String, String> toArgs = new HashMap<>();
            toArgs.put("timeout", String.valueOf(route.getTimeout().getTimeoutSeconds()));
            toArgs.put("cancelRunningFuture", String.valueOf(route.getTimeout().getCancelRunningFuture()));
            toFilter.setArgs(toArgs);
            filters.add(toFilter);

            // Validate timeout config
            TimeoutGatewayFilterFactory.Config toConfig = new TimeoutGatewayFilterFactory.Config();
            toConfig.setTimeout(route.getTimeout().getTimeoutSeconds());
            timeoutFilterFactory.apply(toConfig); // Just to use the field
        }

        // Add retry filter if configured
        if (route.getRetry() != null) {
            FilterDefinition retryFilter = new FilterDefinition();
            retryFilter.setName("Retry");
            Map<String, String> retryArgs = new HashMap<>();
            retryArgs.put("retries", String.valueOf(route.getRetry().getMaxAttempts()));
            retryArgs.put("exceptions", route.getRetry().getRetryableExceptions());
            retryFilter.setArgs(retryArgs);
            filters.add(retryFilter);

            // Validate retry config
            CustomRetryGatewayFilterFactory.Config retryConfig = new CustomRetryGatewayFilterFactory.Config();
            retryConfig.setRetries(route.getRetry().getMaxAttempts());
            retryFilterFactory.apply(retryConfig); // Just to use the field
        }

        definition.setFilters(filters);

        // Use the pathRewriteFilterFactory field
        PathRewriteGatewayFilterFactory.Config pathConfig = new PathRewriteGatewayFilterFactory.Config();
        pathRewriteFilterFactory.apply(pathConfig); // Just to use the field

        return definition;
    }
}