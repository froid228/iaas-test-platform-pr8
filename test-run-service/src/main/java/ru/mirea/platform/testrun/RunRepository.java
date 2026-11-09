package ru.mirea.platform.testrun;


import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RunRepository extends JpaRepository<TestRun, UUID> {}
