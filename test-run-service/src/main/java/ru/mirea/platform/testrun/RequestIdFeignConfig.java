package ru.mirea.platform.testrun;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RequestIdFeignConfig {
    @Bean RequestInterceptor requestIdInterceptor() {
        return template -> {
            String id = MDC.get("requestId");
            if (id != null) template.header("X-Request-Id", id);
        };
    }
}
