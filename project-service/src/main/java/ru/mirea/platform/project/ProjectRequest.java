package ru.mirea.platform.project;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 80) String softwareVersion,
    @NotBlank @Size(max = 500) String artifactReference
) {}
