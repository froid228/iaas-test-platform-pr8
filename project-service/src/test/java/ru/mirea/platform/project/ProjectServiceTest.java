package ru.mirea.platform.project;


import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectServiceTest {
    private final ProjectRepository repository = mock(ProjectRepository.class);
    private final ProjectService service = new ProjectService(repository);
    @Test void createsAndPersists() {
        when(repository.save(any(Project.class))).thenAnswer(i -> i.getArgument(0));
        var result = service.create(new ProjectRequest("CRM", "1.0", "s3://builds/crm-1.0.zip"));
        assertNotNull(result.getName());
        verify(repository).save(result);
    }
    @Test void missingReferenceReturns404() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        var ex = assertThrows(ResponseStatusException.class, () -> service.get(id));
        assertEquals(404, ex.getStatusCode().value());
    }
}
