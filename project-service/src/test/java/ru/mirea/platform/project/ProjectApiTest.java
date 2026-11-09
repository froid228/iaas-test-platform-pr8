package ru.mirea.platform.project;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProjectController.class, properties = {
    "spring.config.location=optional:classpath:/test-application.yaml",
    "spring.application.name=project-service",
    "spring.cloud.config.enabled=false", "eureka.client.enabled=false"
})
@Import(SecurityConfig.class)
class ProjectApiTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProjectService service;
    @MockitoBean JwtDecoder decoder;
    @Test void errorDispatchPreservesNotFoundStatus() throws Exception {
        mvc.perform(get("/error").with(request -> {
                request.setDispatcherType(DispatcherType.ERROR);
                return request;
            })
            .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
            .andExpect(status().isNotFound());
    }
    @Test void anonymousRequestIsRejected() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void readScopeAllowsListing() throws Exception {
        when(service.list()).thenReturn(List.of());
        mvc.perform(get("/api/projects").with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_platform.read"))))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
    }
    @Test void readScopeCannotCreateProject() throws Exception {
        mvc.perform(post("/api/projects").with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_platform.read")))
            .contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void invalidProjectIsRejectedBeforePersistence() throws Exception {
        mvc.perform(post("/api/projects").with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_platform.write")))
            .contentType("application/json").content("""
                {"name":"","softwareVersion":"1","artifactReference":"build.zip"}
                """))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
