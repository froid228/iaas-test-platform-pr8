package ru.mirea.platform.testrun;


import feign.FeignException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RunService {
    private final RunRepository repository;
    private final ProjectClient projects;
    private final EnvironmentClient environments;
    private final ApplicationEventPublisher events;
    public RunService(RunRepository repository, ProjectClient projects, EnvironmentClient environments, ApplicationEventPublisher events) {
        this.repository = repository; this.projects = projects; this.environments = environments; this.events = events;
    }
    @Transactional
    public TestRun create(RunRequest r, String authorization, String subject) {
        try {
            projects.get(r.projectId(), authorization);
            environments.get(r.environmentId(), authorization);
        } catch (FeignException ex) {
            if (ex.status() == 404) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Проект или шаблон стенда не существует");
            if (ex.status() == 401 || ex.status() == 403) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Недостаточно прав для проверки проекта или стенда");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Не удалось проверить проект и стенд");
        }
        var run = repository.save(new TestRun(r, subject));
        String requestId = MDC.get("requestId");
        events.publishEvent(new RunCreated(UUID.randomUUID(), run.getId(), run.getEnvironmentId(),
                requestId == null ? UUID.randomUUID().toString() : requestId));
        return run;
    }
    public List<TestRun> list() { return repository.findAll(); }
    public TestRun get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запуск не найден"));
    }
    @Transactional public TestRun start(UUID id) { var run = get(id); run.start(); return run; }
    @Transactional public TestRun complete(UUID id, RunResult result) { var run = get(id); run.complete(result); return run; }
    @Transactional public TestRun cancel(UUID id) { var run = get(id); run.cancel(); return run; }
}
