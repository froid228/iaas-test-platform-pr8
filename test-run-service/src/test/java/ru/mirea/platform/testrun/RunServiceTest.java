package ru.mirea.platform.testrun;


import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RunServiceTest {
    private final RunRepository repository = mock(RunRepository.class);
    private final ProjectClient projects = mock(ProjectClient.class);
    private final EnvironmentClient environments = mock(EnvironmentClient.class);
    private final org.springframework.context.ApplicationEventPublisher events = mock(org.springframework.context.ApplicationEventPublisher.class);
    private final RunService service = new RunService(repository, projects, environments, events);
    private final RunRequest input = new RunRequest(UUID.randomUUID(), UUID.randomUUID(), "functional-v1");
    @Test void verifiesBothReferencesAndForwardsToken() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        var run = service.create(input, "Bearer test", "tester");
        verify(projects).get(input.projectId(), "Bearer test");
        verify(environments).get(input.environmentId(), "Bearer test");
        assertEquals(RunStatus.QUEUED, run.getStatus());
        assertEquals("tester", run.getRequestedBy());
        var event = org.mockito.ArgumentCaptor.forClass(RunCreated.class);
        verify(events).publishEvent(event.capture());
        assertEquals(input.environmentId(), event.getValue().environmentId());
        assertNotNull(event.getValue().requestId());
    }
    @Test void missingProjectDoesNotCreateRun() {
        when(projects.get(any(), any())).thenThrow(new FeignException.NotFound("missing", request(), null, Map.of()));
        var ex = assertThrows(ResponseStatusException.class, () -> service.create(input, "Bearer test", "tester"));
        assertEquals(422, ex.getStatusCode().value());
        verifyNoInteractions(repository, environments, events);
    }
    @Test void unavailableDependencyDoesNotCreateRun() {
        when(environments.get(any(), any())).thenThrow(new FeignException.ServiceUnavailable("down", request(), null, Map.of()));
        var ex = assertThrows(ResponseStatusException.class, () -> service.create(input, "Bearer test", "tester"));
        assertEquals(503, ex.getStatusCode().value());
        verifyNoInteractions(repository, events);
    }
    private Request request() {
        return Request.create(Request.HttpMethod.GET, "http://example.test", Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
    }
}
