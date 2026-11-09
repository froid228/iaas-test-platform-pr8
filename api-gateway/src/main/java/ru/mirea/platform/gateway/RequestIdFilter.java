package ru.mirea.platform.gateway;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class RequestIdFilter implements WebFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);
    @Override public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }
    @Override public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String candidate = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
        String id = candidate != null && candidate.matches("[A-Za-z0-9._-]{1,100}")
                ? candidate : UUID.randomUUID().toString();
        var request = exchange.getRequest().mutate().headers(h -> h.set("X-Request-Id", id)).build();
        exchange.getResponse().getHeaders().set("X-Request-Id", id);
        log.info("requestId={} service=api-gateway method={} path={} received", id,
                request.getMethod(), request.getPath());
        return chain.filter(exchange.mutate().request(request).build()).doFinally(signal ->
            log.info("requestId={} service=api-gateway method={} path={} status={}", id,
                    request.getMethod(), request.getPath(), exchange.getResponse().getStatusCode()));
    }
}
