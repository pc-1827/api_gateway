package com.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CustomRetryGatewayFilterFactory extends RetryGatewayFilterFactory {

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("retries", "backoff", "methods", "exceptions", "statuses");
    }

    // The Config parameter type must match the parent class method
    public GatewayFilter apply(CustomRetryGatewayFilterFactory.Config config) {
        RetryConfig retryConfig = new RetryConfig();

        // Set retry attempts
        retryConfig.setRetries(config.getRetries());

        // Configure backoff if specified
        if (config.getBackoff() != null && !config.getBackoff().isEmpty()) {
            String[] parts = config.getBackoff().split(",");
            if (parts.length >= 2) {
                long firstBackoff = Long.parseLong(parts[0].trim());
                // Create durations for min and max
                Duration firstBackoffDuration = Duration.ofMillis(firstBackoff);
                Duration maxBackoff = Duration.ofSeconds(30);

                // Use fixed backoff with max duration
                retryConfig.setBackoff(firstBackoffDuration, maxBackoff, 3, true);
            }
        }

        // Set methods to retry
        if (config.getMethods() != null && !config.getMethods().isEmpty()) {
            Set<HttpMethod> methodSet = Arrays.stream(config.getMethods().split(","))
                    .map(String::trim)
                    .map(HttpMethod::valueOf)
                    .collect(Collectors.toSet());

            HttpMethod[] methodsArray = methodSet.toArray(new HttpMethod[0]);
            retryConfig.setMethods(methodsArray);
        } else {
            // Default to GET
            retryConfig.setMethods(HttpMethod.GET);
        }

        // Set exception types to retry on
        if (config.getExceptions() != null && !config.getExceptions().isEmpty()) {
            Set<Class<? extends Throwable>> exceptions = Arrays.stream(config.getExceptions().split(","))
                    .map(String::trim)
                    .map(this::getExceptionClass)
                    .collect(Collectors.toSet());

            @SuppressWarnings("unchecked")
            Class<? extends Throwable>[] exceptionsArray = exceptions.toArray(new Class[0]);
            retryConfig.setExceptions(exceptionsArray);
        }

        // Set status codes to retry on
        if (config.getStatuses() != null && !config.getStatuses().isEmpty()) {
            Set<HttpStatus> statuses = Arrays.stream(config.getStatuses().split(","))
                    .map(String::trim)
                    .map(HttpStatus::valueOf)
                    .collect(Collectors.toSet());

            HttpStatus[] statusesArray = statuses.toArray(new HttpStatus[0]);
            retryConfig.setStatuses(statusesArray);

            // Use empty series array
            retryConfig.setSeries();
        }

        log.info("Creating retry filter with config: {}", retryConfig);

        return (exchange, chain) -> {
            // Wrap the chain filter call to add retry logging
            return super.apply(retryConfig)
                    .filter(exchange, chain)
                    .doOnError(e -> log.error("Retry attempt failed: {}", e.getMessage()));
        };
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Throwable> getExceptionClass(String className) {
        try {
            return (Class<? extends Throwable>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            log.warn("Exception class not found: {}, using RuntimeException", className);
            return RuntimeException.class;
        }
    }

    // Inner config class to accept configuration options
    public static class Config {
        private Integer retries = 3;
        private String backoff; // format: "firstBackoff,factor" e.g. "100,1.5"
        private String methods; // comma-separated list of HTTP methods
        private String exceptions; // comma-separated list of exception class names
        private String statuses; // comma-separated list of HTTP status codes

        public Integer getRetries() {
            return retries;
        }

        public void setRetries(Integer retries) {
            this.retries = retries;
        }

        public String getBackoff() {
            return backoff;
        }

        public void setBackoff(String backoff) {
            this.backoff = backoff;
        }

        public String getMethods() {
            return methods;
        }

        public void setMethods(String methods) {
            this.methods = methods;
        }

        public String getExceptions() {
            return exceptions;
        }

        public void setExceptions(String exceptions) {
            this.exceptions = exceptions;
        }

        public String getStatuses() {
            return statuses;
        }

        public void setStatuses(String statuses) {
            this.statuses = statuses;
        }
    }
}