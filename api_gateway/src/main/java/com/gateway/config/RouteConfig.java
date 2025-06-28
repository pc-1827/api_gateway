package com.gateway.config;

import com.gateway.filter.*;
import com.gateway.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RouteConfig {

    private final RouteRepository routeRepository;
    private final CircuitBreakerGatewayFilterFactory circuitBreakerFilterFactory;
    private final RedisRateLimiterGatewayFilterFactory rateLimiterFilterFactory;
    private final TimeoutGatewayFilterFactory timeoutFilterFactory;
    private final CustomRetryGatewayFilterFactory retryFilterFactory;
    private final PathRewriteGatewayFilterFactory pathRewriteFilterFactory;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        RouteLocatorBuilder.Builder routesBuilder = builder.routes();
        
        log.info("Starting to load routes...");
        
        // Add a basic route for testing (fixed route builder chain)
        routesBuilder.route("basic-httpbin", r -> r
            .path("/httpbin/**")
            .filters(f -> f.filter(pathRewriteFilterFactory.apply(
                new PathRewriteGatewayFilterFactory.Config() {{
                    setStripPrefix(true);
                    setPrefixSize(1);
                }})))
            .uri("http://httpbin.org"));
        
        routeRepository.findByEnabled(true)
                .doOnNext(route -> {
                    log.info("Loading route: {}", route);
                    
                    // Fix route builder chain - don't reassign 'r'
                    routesBuilder.route(route.getRouteId(), r -> {
                        // First add all predicates
                        var predicateSpec = r.path(route.getPath());
                        
                        if (route.getMethod() != null && !route.getMethod().isEmpty()) {
                            predicateSpec = predicateSpec.and().method(HttpMethod.valueOf(route.getMethod()));
                        }
                        
                        // Then add filters and URI in the right order
                        return predicateSpec.filters(f -> {
                            // Apply path rewrite filter
                            PathRewriteGatewayFilterFactory.Config pathRewriteConfig = new PathRewriteGatewayFilterFactory.Config();
                            pathRewriteConfig.setStripPrefix(true);
                            pathRewriteConfig.setPrefixSize(1);
                            f = f.filter(pathRewriteFilterFactory.apply(pathRewriteConfig));
                            
                            // Apply resilience filters
                            if (route.getCircuitBreaker() != null) {
                                CircuitBreakerGatewayFilterFactory.Config cbConfig = new CircuitBreakerGatewayFilterFactory.Config();
                                cbConfig.setName(route.getCircuitBreaker().getName());
                                cbConfig.setFallbackUri(route.getCircuitBreaker().getFallbackUri());
                                cbConfig.setSlidingWindowSize(route.getCircuitBreaker().getSlidingWindowSize());
                                cbConfig.setFailureRateThreshold(route.getCircuitBreaker().getFailureRateThreshold());
                                cbConfig.setWaitDurationInOpenState(route.getCircuitBreaker().getWaitDurationInOpenState());
                                cbConfig.setPermittedCallsInHalfOpenState(route.getCircuitBreaker().getPermittedCallsInHalfOpenState());
                                cbConfig.setAutomaticTransition(route.getCircuitBreaker().getAutomaticTransition());
                                
                                f = f.filter(circuitBreakerFilterFactory.apply(cbConfig));
                            }
                            
                            if (route.getRateLimiter() != null) {
                                RedisRateLimiterGatewayFilterFactory.Config rlConfig = new RedisRateLimiterGatewayFilterFactory.Config();
                                rlConfig.setReplenishRate(route.getRateLimiter().getReplenishRate());
                                rlConfig.setBurstCapacity(route.getRateLimiter().getBurstCapacity());
                                rlConfig.setRequestedTokens(route.getRateLimiter().getRequestedTokens());
                                
                                f = f.filter(rateLimiterFilterFactory.apply(rlConfig));
                            }
                            
                            if (route.getTimeout() != null) {
                                TimeoutGatewayFilterFactory.Config toConfig = new TimeoutGatewayFilterFactory.Config();
                                toConfig.setTimeout(route.getTimeout().getTimeoutSeconds());
                                toConfig.setCancelRunningFuture(route.getTimeout().getCancelRunningFuture());
                                
                                f = f.filter(timeoutFilterFactory.apply(toConfig));
                            }
                            
                            if (route.getRetry() != null) {
                                CustomRetryGatewayFilterFactory.Config retryConfig = new CustomRetryGatewayFilterFactory.Config();
                                retryConfig.setRetries(route.getRetry().getMaxAttempts());
                                retryConfig.setBackoff(route.getRetry().getBackoffDuration());
                                retryConfig.setExceptions(route.getRetry().getRetryableExceptions());
                                
                                f = f.filter(retryFilterFactory.apply(retryConfig));
                            }
                            
                            return f;
                        })
                        .uri(route.getUri());
                    });
                })
                .doOnComplete(() -> log.info("Route loading completed"))
                .subscribe();

        return routesBuilder.build();
    }
}
