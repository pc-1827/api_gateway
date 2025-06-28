package com.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Component("Timeout")
@Slf4j
public class TimeoutGatewayFilterFactory extends AbstractGatewayFilterFactory<TimeoutGatewayFilterFactory.Config> {

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("timeout", "cancelRunningFuture");
    }

    public TimeoutGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return chain.filter(exchange)
                    .timeout(Duration.ofSeconds(config.getTimeout()))
                    .onErrorResume(TimeoutException.class, e -> handleTimeoutError(exchange, e));
        };
    }

    private Mono<Void> handleTimeoutError(ServerWebExchange exchange, TimeoutException e) {
        log.error("Request timed out: {}", e.getMessage());
        exchange.getResponse().setStatusCode(HttpStatus.GATEWAY_TIMEOUT);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        private Integer timeout = 30;
        private Boolean cancelRunningFuture = true;

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }

        public Boolean getCancelRunningFuture() {
            return cancelRunningFuture;
        }

        public void setCancelRunningFuture(Boolean cancelRunningFuture) {
            this.cancelRunningFuture = cancelRunningFuture;
        }
    }
}