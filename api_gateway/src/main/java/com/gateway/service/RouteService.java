package com.gateway.service;

import com.gateway.dto.RouteDTO;
import com.gateway.entity.Route;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface RouteService {
    Flux<RouteDTO> getAllRoutes();

    Flux<RouteDTO> getEnabledRoutes();

    Mono<RouteDTO> getRouteById(String id);

    Mono<RouteDTO> getRouteByRouteId(String routeId);

    Mono<RouteDTO> createRoute(Route route);

    Mono<RouteDTO> updateRoute(String id, Route route);

    Mono<Void> deleteRoute(String id);

    Mono<RouteDTO> toggleRouteStatus(String id, boolean enabled);

    Flux<RouteDTO> searchRoutes(String path, String method);

    Mono<Void> refreshRoutes();
}
