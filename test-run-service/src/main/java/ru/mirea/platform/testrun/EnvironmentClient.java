package ru.mirea.platform.testrun;


import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "environment-service")
public interface EnvironmentClient {
    @GetMapping("/api/environments/{id}")
    Reference get(@PathVariable("id") UUID id, @RequestHeader("Authorization") String authorization);
    record Reference(UUID id) {}
}
