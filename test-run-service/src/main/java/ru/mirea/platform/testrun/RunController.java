package ru.mirea.platform.testrun;


import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test-runs")
public class RunController {
    private final RunService service;
    public RunController(RunService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public TestRun create(@Valid @RequestBody RunRequest r, @RequestHeader("Authorization") String token,
                          @AuthenticationPrincipal Jwt jwt) { return service.create(r, token, jwt.getSubject()); }
    @GetMapping public List<TestRun> list() { return service.list(); }
    @GetMapping("/{id}") public TestRun get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping("/{id}/start") public TestRun start(@PathVariable UUID id) { return service.start(id); }
    @PostMapping("/{id}/result") public TestRun complete(@PathVariable UUID id, @Valid @RequestBody RunResult r) { return service.complete(id, r); }
    @PostMapping("/{id}/cancel") public TestRun cancel(@PathVariable UUID id) { return service.cancel(id); }
}
