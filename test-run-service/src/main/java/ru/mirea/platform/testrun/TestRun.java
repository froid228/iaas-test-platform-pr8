package ru.mirea.platform.testrun;


import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "test_runs")
public class TestRun {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Version private long revision;
    @Column(nullable = false) private UUID projectId;
    @Column(nullable = false) private UUID environmentId;
    @Column(nullable = false, length = 120) private String testSuite;
    @Column(nullable = false, length = 200) private String requestedBy;
    @Column(nullable = false) private Instant createdAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RunStatus status;
    private Instant startedAt;
    private Instant finishedAt;
    @Column(length = 2000) private String summary;
    @Column(length = 1000) private String evidenceUri;
    protected TestRun() {}
    public TestRun(RunRequest r, String requestedBy) {
        projectId = r.projectId(); environmentId = r.environmentId(); testSuite = r.testSuite();
        this.requestedBy = requestedBy; createdAt = Instant.now(); status = RunStatus.QUEUED;
    }
    public void start() {
        require(RunStatus.QUEUED); status = RunStatus.RUNNING; startedAt = Instant.now();
    }
    public void complete(RunResult r) {
        require(RunStatus.RUNNING);
        if (r.status() != RunStatus.PASSED && r.status() != RunStatus.FAILED) {
            throw new IllegalStateException("Результат должен иметь статус PASSED или FAILED");
        }
        status = r.status(); summary = r.summary(); evidenceUri = r.evidenceUri(); finishedAt = Instant.now();
    }
    public void cancel() {
        if (status != RunStatus.QUEUED && status != RunStatus.RUNNING) {
            throw new IllegalStateException("Завершённый запуск нельзя отменить");
        }
        status = RunStatus.CANCELLED; finishedAt = Instant.now();
    }
    private void require(RunStatus expected) {
        if (status != expected) throw new IllegalStateException("Ожидался статус " + expected + ", текущий: " + status);
    }
    public UUID getId() { return id; }
    public long getRevision() { return revision; }
    public UUID getProjectId() { return projectId; }
    public UUID getEnvironmentId() { return environmentId; }
    public String getTestSuite() { return testSuite; }
    public String getRequestedBy() { return requestedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public RunStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public String getSummary() { return summary; }
    public String getEvidenceUri() { return evidenceUri; }
}
