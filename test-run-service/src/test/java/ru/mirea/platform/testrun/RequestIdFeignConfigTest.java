package ru.mirea.platform.testrun;

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import static org.junit.jupiter.api.Assertions.*;

class RequestIdFeignConfigTest {
    @Test void forwardsRequestIdToDownstreamService() {
        try {
            MDC.put("requestId", "pr8-test-trace");
            var template = new RequestTemplate();
            new RequestIdFeignConfig().requestIdInterceptor().apply(template);
            assertEquals(java.util.List.of("pr8-test-trace"),
                    java.util.List.copyOf(template.headers().get("X-Request-Id")));
        } finally { MDC.remove("requestId"); }
    }
    @Test void omitsHeaderOutsideHttpRequest() {
        MDC.remove("requestId");
        var template = new RequestTemplate();
        new RequestIdFeignConfig().requestIdInterceptor().apply(template);
        assertFalse(template.headers().containsKey("X-Request-Id"));
    }
}
