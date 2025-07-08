package com.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class PathRewriteGatewayFilterFactory
        extends AbstractGatewayFilterFactory<PathRewriteGatewayFilterFactory.Config> {

    public PathRewriteGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("stripPrefix", "prefixSize");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getRawPath();

            String newPath;
            if (config.isStripPrefix() && config.getPrefixSize() > 0) {
                String[] parts = path.substring(1).split("/", config.getPrefixSize() + 1);
                if (parts.length > config.getPrefixSize()) {
                    newPath = "/" + parts[config.getPrefixSize()];
                    // Add the remaining path if any
                    if (parts.length > config.getPrefixSize() + 1) {
                        for (int i = config.getPrefixSize() + 1; i < parts.length; i++) {
                            newPath += "/" + parts[i];
                        }
                    }
                } else {
                    newPath = "/";
                }
            } else {
                newPath = path;
            }

            ServerHttpRequest newRequest = request.mutate()
                    .path(newPath)
                    .build();

            return chain.filter(exchange.mutate().request(newRequest).build());
        };
    }

    public static class Config {
        private boolean stripPrefix = false;
        private int prefixSize = 1;

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
    }
}