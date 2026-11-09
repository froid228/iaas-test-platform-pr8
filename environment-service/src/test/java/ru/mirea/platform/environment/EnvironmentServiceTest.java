package ru.mirea.platform.environment;


import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnvironmentServiceTest {
    private final EnvironmentRepository repository = mock(EnvironmentRepository.class);
    private final EnvironmentService service = new EnvironmentService(repository);
    @Test void createsAndPersists() {
        when(repository.save(any(EnvironmentTemplate.class))).thenAnswer(i -> i.getArgument(0));
        var result = service.create(new EnvironmentRequest("Linux", "Ubuntu 24.04", "ubuntu-24.04", 2, 2048));
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
