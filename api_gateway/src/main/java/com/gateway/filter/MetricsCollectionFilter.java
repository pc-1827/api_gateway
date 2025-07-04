package com.gateway.filter;

import com.gateway.entity.ApiMetric;
import com.gateway.service.ApiMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;

@Component
@Slf4j
public class MetricsCollectionFilter implements GlobalFilter, Ordered {

    private final ApiMetricsService metricsService;

    public MetricsCollectionFilter(ApiMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    try {
                        // Calculate duration
                        long duration = System.currentTimeMillis() - startTime;

                        // Create and save metric
                        ApiMetric metric = createMetric(exchange, duration);
                        metricsService.saveMetric(metric).subscribe();

                    } catch (Exception e) {
                        log.error("Error collecting metrics: {}", e.getMessage(), e);
                    }
                });
    }

    private ApiMetric createMetric(ServerWebExchange exchange, long duration) {
        ServerHttpRequest request = exchange.getRequest();
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        ApiMetric metric = new ApiMetric();
        metric.setTimestamp(LocalDateTime.now());
        metric.setDuration(duration);
        metric.setPath(request.getPath().toString());
        metric.setMethod(request.getMethod().name());

        // Set route information
        if (route != null) {
            metric.setRouteId(route.getId());
            metric.setToService(extractServiceName(route.getUri().toString()));
        }

        // Set client information
        metric.setClientIp(Objects.requireNonNull(request.getRemoteAddress()).getAddress().getHostAddress());
        metric.setUserAgent(request.getHeaders().getFirst("User-Agent"));

        // Set interaction type
        metric.setInteractionType(determineInteractionType(request));

        // Set query parameters
        String query = request.getURI().getQuery();
        metric.setQueryParameters(query != null ? query : "");

        // Set success/failure status
        boolean isSuccessful = exchange.getResponse().getStatusCode() != null &&
                exchange.getResponse().getStatusCode().is2xxSuccessful();
        metric.setSuccess(isSuccessful);
        metric.setStatusCode(
                exchange.getResponse().getStatusCode() != null ? exchange.getResponse().getStatusCode().value() : 0);

        return metric;
    }

    private String extractServiceName(String uri) {
        if (uri == null)
            return "unknown";

        // Extract service name from URI
        // Example: http://product-service:8080 -> product-service
        try {
            String host = java.net.URI.create(uri).getHost();
            return host != null ? host : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String determineInteractionType(ServerHttpRequest request) {
        // Check for custom service name header
        String serviceName = request.getHeaders().getFirst("X-Service-Name");
        return serviceName != null ? "APP_TO_APP" : "USER_TO_APP";
    }

    @Override
    public int getOrder() {
        // Should run after other filters but before the request is sent to the service
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}