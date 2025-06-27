package com.gateway.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.gateway.dto.RouteDTO;
import com.gateway.entity.Route;
import com.gateway.repository.RouteRepository;
import com.gateway.service.RouteService;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Flux<RouteDTO> getAllRoutes() {
        return routeRepository.findAll()
                .map(this::convertToDTO);
    }

    @Override
    public Flux<RouteDTO> getEnabledRoutes() {
        return routeRepository.findByEnabled(true)
                .map(this::convertToDTO);
    }

    @Override
    public Mono<RouteDTO> getRouteById(String id) {
        return routeRepository.findById(id)
                .map(this::convertToDTO);
    }

    @Override
    public Mono<RouteDTO> getRouteByRouteId(String routeId) {
        return routeRepository.findByRouteId(routeId)
                .map(this::convertToDTO);
    }

    @Override
    public Mono<RouteDTO> createRoute(Route route) {
        if (route.getRouteId() == null) {
            route.setRouteId(UUID.randomUUID().toString());
        }

        route.setCreatedAt(LocalDateTime.now());
        route.setUpdatedAt(LocalDateTime.now());

        return routeRepository.save(route)
                .map(this::convertToDTO)
                .doOnSuccess(r -> refreshRoutes().subscribe());
    }

    @Override
    public Mono<RouteDTO> updateRoute(String id, Route route) {
        return routeRepository.findById(id)
                .flatMap(existingRoute -> {
                    existingRoute.setPath(route.getPath());
                    existingRoute.setMethod(route.getMethod());
                    existingRoute.setUri(route.getUri());
                    existingRoute.setOrder(route.getOrder());
                    existingRoute.setFilters(route.getFilters());
                    existingRoute.setMetadata(route.getMetadata());
                    existingRoute.setUpdatedAt(LocalDateTime.now());
                    existingRoute.setUpdatedBy(route.getUpdatedBy());

                    return routeRepository.save(existingRoute);
                })
                .map(this::convertToDTO)
                .doOnSuccess(r -> refreshRoutes().subscribe());
    }

    @Override
    public Mono<Void> deleteRoute(String id) {
        return routeRepository.deleteById(id)
                .then(refreshRoutes());
    }

    @Override
    public Mono<RouteDTO> toggleRouteStatus(String id, boolean enabled) {
        return routeRepository.findById(id)
                .flatMap(existingRoute -> {
                    existingRoute.setEnabled(enabled);
                    existingRoute.setUpdatedAt(LocalDateTime.now());
                    return routeRepository.save(existingRoute);
                })
                .map(this::convertToDTO)
                .doOnSuccess(r -> refreshRoutes().subscribe());
    }

    @Override
    public Flux<RouteDTO> searchRoutes(String path, String method) {
        if (path != null && method != null) {
            return routeRepository.findByPathContainingIgnoreCaseAndMethod(path, method)
                    .map(this::convertToDTO);
        } else if (path != null) {
            return routeRepository.findByPathContainingIgnoreCase(path)
                    .map(this::convertToDTO);
        } else if (method != null) {
            return routeRepository.findByMethod(method)
                    .map(this::convertToDTO);
        }
        return getAllRoutes();
    }

    @Override
    public Mono<Void> refreshRoutes() {
        log.info("Refreshing routes");
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
        return Mono.empty();
    }

    private RouteDTO convertToDTO(Route route) {
        return RouteDTO.builder()
                .id(route.getId())
                .routeId(route.getRouteId())
                .path(route.getPath())
                .method(route.getMethod())
                .uri(route.getUri())
                .order(route.getOrder())
                .metadata(route.getMetadata())
                .filters(route.getFilters())
                .enabled(route.isEnabled())
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .createdBy(route.getCreatedBy())
                .updatedBy(route.getUpdatedBy())
                .build();
    }
}
