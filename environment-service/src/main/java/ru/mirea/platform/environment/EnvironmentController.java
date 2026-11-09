package ru.mirea.platform.environment;


import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/environments")
public class EnvironmentController {
    private final EnvironmentService service;
    public EnvironmentController(EnvironmentService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public EnvironmentTemplate create(@Valid @RequestBody EnvironmentRequest r) { return service.create(r); }
    @GetMapping public List<EnvironmentTemplate> list() { return service.list(); }
    @GetMapping("/{id}") public EnvironmentTemplate get(@PathVariable UUID id) { return service.get(id); }
}
