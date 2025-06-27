package com.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.gateway.entity.Route;
import com.gateway.repository.RouteRepository;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RouteConfig {

    private final RouteRepository routeRepository;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        RouteLocatorBuilder.Builder routesBuilder = builder.routes();

        routeRepository.findByEnabled(true)
                .doOnNext(route -> {
                    log.info("Loading route: {}", route);

                    // Create route with path and optional method predicate
                    if (route.getMethod() != null && !route.getMethod().isEmpty()) {
                        routesBuilder.route(route.getRouteId(), r -> r
                                .path(route.getPath())
                                .and()
                                .method(route.getMethod())
                                .uri(route.getUri()));
                    } else {
                        routesBuilder.route(route.getRouteId(), r -> r
                                .path(route.getPath())
                                .uri(route.getUri()));
                    }
                })
                .subscribe();

        return routesBuilder.build();
    }
}
