package ru.mirea.platform.testrun;


import jakarta.validation.constraints.*;
import java.util.UUID;
public record RunRequest(@NotNull UUID projectId, @NotNull UUID environmentId,
                         @NotBlank @Size(max = 120) String testSuite) {}
