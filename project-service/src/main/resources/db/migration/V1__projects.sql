CREATE TABLE projects (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    software_version VARCHAR(80) NOT NULL,
    artifact_reference VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
