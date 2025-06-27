package com.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.gateway.dto.RouteDTO;
import com.gateway.entity.Route;
import com.gateway.service.RouteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@Slf4j
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    public Flux<RouteDTO> getAllRoutes() {
        return routeService.getAllRoutes();
    }

    @GetMapping("/enabled")
    public Flux<RouteDTO> getEnabledRoutes() {
        return routeService.getEnabledRoutes();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<RouteDTO>> getRouteById(@PathVariable String id) {
        return routeService.getRouteById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/routeId/{routeId}")
    public Mono<ResponseEntity<RouteDTO>> getRouteByRouteId(@PathVariable String routeId) {
        return routeService.getRouteByRouteId(routeId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RouteDTO> createRoute(@RequestBody Route route) {
        return routeService.createRoute(route);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<RouteDTO>> updateRoute(@PathVariable String id, @RequestBody Route route) {
        return routeService.updateRoute(id, route)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteRoute(@PathVariable String id) {
        return routeService.deleteRoute(id);
    }

    @PatchMapping("/{id}/status")
    public Mono<ResponseEntity<RouteDTO>> toggleRouteStatus(
            @PathVariable String id,
            @RequestBody Map<String, Boolean> status) {

        Boolean enabled = status.get("enabled");
        if (enabled == null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return routeService.toggleRouteStatus(id, enabled)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public Flux<RouteDTO> searchRoutes(
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String method) {

        return routeService.searchRoutes(path, method);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> refreshRoutes() {
        return routeService.refreshRoutes();
    }
}
