package ru.mirea.platform.testrun;

import java.util.UUID;

public record RunCreated(UUID eventId, UUID runId, UUID environmentId, String requestId) {}
