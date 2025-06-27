package com.gateway.repository;

import com.gateway.entity.Route;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface RouteRepository extends ReactiveMongoRepository<Route, String> {
    Flux<Route> findByEnabled(boolean enabled);

    Flux<Route> findByPathContainingIgnoreCase(String path);

    Flux<Route> findByMethod(String method);

    Mono<Route> findByRouteId(String routeId);

    Flux<Route> findByPathContainingIgnoreCaseAndMethod(String path, String method);
}
