CREATE TABLE environment_templates (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    operating_system VARCHAR(120) NOT NULL,
    image_reference VARCHAR(500) NOT NULL,
    cpu INTEGER NOT NULL CHECK (cpu BETWEEN 1 AND 64),
    memory_mb INTEGER NOT NULL CHECK (memory_mb BETWEEN 256 AND 262144)
);
