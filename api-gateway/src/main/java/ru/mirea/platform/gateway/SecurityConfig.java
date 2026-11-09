package ru.mirea.platform.gateway;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityWebFilterChain security(ServerHttpSecurity http) {
        return http.csrf(c -> c.disable())
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange(a -> a
                .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/**").hasAuthority("SCOPE_platform.read")
                .pathMatchers("/api/**").hasAuthority("SCOPE_platform.write")
                .anyExchange().denyAll())
            .oauth2ResourceServer(o -> o.jwt(j -> {})).build();
    }
}
