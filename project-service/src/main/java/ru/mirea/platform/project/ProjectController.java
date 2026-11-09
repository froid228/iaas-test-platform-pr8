package ru.mirea.platform.project;


import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService service;
    public ProjectController(ProjectService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Project create(@Valid @RequestBody ProjectRequest r) { return service.create(r); }
    @GetMapping public List<Project> list() { return service.list(); }
    @GetMapping("/{id}") public Project get(@PathVariable UUID id) { return service.get(id); }
}
