package ru.mirea.platform.testrun;


import jakarta.validation.constraints.*;
public record RunResult(@NotNull RunStatus status, @NotBlank @Size(max = 2000) String summary,
                        @NotBlank @Size(max = 1000) String evidenceUri) {}
