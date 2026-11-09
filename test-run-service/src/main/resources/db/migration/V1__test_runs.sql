CREATE TABLE test_runs (
    id UUID PRIMARY KEY,
    revision BIGINT NOT NULL DEFAULT 0,
    project_id UUID NOT NULL,
    environment_id UUID NOT NULL,
    test_suite VARCHAR(120) NOT NULL,
    requested_by VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('QUEUED', 'RUNNING', 'PASSED', 'FAILED', 'CANCELLED')),
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    summary VARCHAR(2000),
    evidence_uri VARCHAR(1000)
);
CREATE INDEX idx_test_runs_project ON test_runs(project_id);
