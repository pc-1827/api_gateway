package com.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Component("PathRewrite") // <-- Add this name
@Slf4j
public class PathRewriteGatewayFilterFactory
        extends AbstractGatewayFilterFactory<PathRewriteGatewayFilterFactory.Config> {

    public PathRewriteGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("stripPrefix", "prefixPath");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getRawPath();
            String newPath = path;

            // Strip prefix if enabled
            if (config.isStripPrefix() && config.getPrefixSize() > 0) {
                String[] parts = path.split("/");
                if (parts.length > config.getPrefixSize()) {
                    StringBuilder newPathBuilder = new StringBuilder();
                    for (int i = config.getPrefixSize() + 1; i < parts.length; i++) {
                        newPathBuilder.append("/").append(parts[i]);
                    }
                    if (newPathBuilder.length() == 0) {
                        newPathBuilder.append("/");
                    }
                    newPath = newPathBuilder.toString();
                }
            }

            // Add prefix path if specified
            if (config.getPrefixPath() != null && !config.getPrefixPath().isEmpty()) {
                if (!config.getPrefixPath().startsWith("/")) {
                    newPath = "/" + config.getPrefixPath() + newPath;
                } else {
                    newPath = config.getPrefixPath() + newPath;
                }
            }

            log.debug("Path rewrite: {} -> {}", path, newPath);

            URI newUri = UriComponentsBuilder.fromUri(request.getURI())
                    .replacePath(newPath)
                    .build()
                    .toUri();

            ServerHttpRequest newRequest = request.mutate()
                    .uri(newUri)
                    .build();

            return chain.filter(exchange.mutate().request(newRequest).build());
        };
    }

    public static class Config {
        private boolean stripPrefix = false;
        private int prefixSize = 1; // Number of path segments to strip
        private String prefixPath = ""; // Path to prepend

        public boolean isStripPrefix() {
            return stripPrefix;
        }

        public void setStripPrefix(boolean stripPrefix) {
            this.stripPrefix = stripPrefix;
        }

        public int getPrefixSize() {
            return prefixSize;
        }

        public void setPrefixSize(int prefixSize) {
            this.prefixSize = prefixSize;
        }

        public String getPrefixPath() {
            return prefixPath;
        }

        public void setPrefixPath(String prefixPath) {
            this.prefixPath = prefixPath;
        }
    }
}