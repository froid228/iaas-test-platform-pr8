package ru.mirea.platform.environment;


import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EnvironmentService {
    private final EnvironmentRepository repository;
    public EnvironmentService(EnvironmentRepository repository) { this.repository = repository; }
    public EnvironmentTemplate create(EnvironmentRequest r) { return repository.save(new EnvironmentTemplate(r)); }
    public List<EnvironmentTemplate> list() { return repository.findAll(); }
    public EnvironmentTemplate get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Шаблон стенда не найден"));
    }
}
