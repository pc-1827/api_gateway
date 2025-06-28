package com.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
@Endpoint(id = "gateway-refresh")
@RequiredArgsConstructor
@Slf4j
public class RouteRefreshEndpoint {

    private final ApplicationEventPublisher eventPublisher;

    @ReadOperation
    public Mono<Map<String, String>> refresh() {
        log.info("Manual refresh of gateway routes triggered");
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));

        Map<String, String> result = new HashMap<>();
        result.put("result", "success");
        result.put("message", "Routes refreshed");

        return Mono.just(result);
    }
}