package ru.mirea.platform.testrun;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InstanceHeaderFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(InstanceHeaderFilter.class);
    private final String instance;
    public InstanceHeaderFilter(@Value("${spring.application.name}") String application,
                                @Value("${HOSTNAME:localhost}") String hostname,
                                @Value("${server.port:8080}") String port) {
        instance = application + ":" + hostname + ":" + port;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Service-Instance", instance);
        String candidate = request.getHeader("X-Request-Id");
        String requestId = candidate != null && candidate.matches("[A-Za-z0-9._-]{1,100}")
                ? candidate : UUID.randomUUID().toString();
        response.setHeader("X-Request-Id", requestId);
        String previous = MDC.get("requestId");
        MDC.put("requestId", requestId);
        log.info("requestId={} service={} method={} path={} received", requestId, instance,
                request.getMethod(), request.getRequestURI());
        try {
            chain.doFilter(request, response);
        } finally {
            log.info("requestId={} service={} method={} path={} status={}", requestId, instance,
                    request.getMethod(), request.getRequestURI(), response.getStatus());
            if (previous == null) MDC.remove("requestId"); else MDC.put("requestId", previous);
        }
    }
}
