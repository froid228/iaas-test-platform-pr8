package ru.mirea.platform.project;


import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProjectService {
    private final ProjectRepository repository;
    public ProjectService(ProjectRepository repository) { this.repository = repository; }
    public Project create(ProjectRequest r) {
        return repository.save(new Project(r.name(), r.softwareVersion(), r.artifactReference()));
    }
    public List<Project> list() { return repository.findAll(); }
    public Project get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Проект не найден"));
    }
}
