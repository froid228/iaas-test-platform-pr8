package ru.mirea.platform.environment;


import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EnvironmentRepository extends JpaRepository<EnvironmentTemplate, UUID> {}
