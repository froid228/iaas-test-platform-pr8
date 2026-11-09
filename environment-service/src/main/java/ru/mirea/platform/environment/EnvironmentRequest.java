package ru.mirea.platform.environment;


import jakarta.validation.constraints.*;

public record EnvironmentRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 120) String operatingSystem,
    @NotBlank @Size(max = 500) String imageReference,
    @Min(1) @Max(64) int cpu,
    @Min(256) @Max(262144) int memoryMb
) {}
