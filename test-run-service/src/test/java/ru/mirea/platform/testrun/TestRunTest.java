package ru.mirea.platform.testrun;


import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TestRunTest {
    private TestRun run() { return new TestRun(new RunRequest(UUID.randomUUID(), UUID.randomUUID(), "functional-v1"), "tester"); }
    @Test void recordsSuccessfulResultWithEvidence() {
        var run = run(); run.start();
        run.complete(new RunResult(RunStatus.PASSED, "10/10", "s3://reports/result.json"));
        assertEquals(RunStatus.PASSED, run.getStatus());
        assertNotNull(run.getStartedAt()); assertNotNull(run.getFinishedAt());
        assertEquals("s3://reports/result.json", run.getEvidenceUri());
    }
    @Test void cannotCompleteQueuedRun() {
        assertThrows(IllegalStateException.class, () -> run().complete(new RunResult(RunStatus.PASSED, "ok", "uri")));
    }
    @Test void cannotUseRunningAsResult() {
        var run = run(); run.start();
        assertThrows(IllegalStateException.class, () -> run.complete(new RunResult(RunStatus.RUNNING, "ok", "uri")));
        assertEquals(RunStatus.RUNNING, run.getStatus());
    }
    @Test void cannotRestartOrCancelFinishedRun() {
        var run = run(); run.start(); run.complete(new RunResult(RunStatus.FAILED, "1 failure", "uri"));
        assertThrows(IllegalStateException.class, run::start);
        assertThrows(IllegalStateException.class, run::cancel);
    }
    @Test void canCancelQueuedRun() {
        var run = run(); run.cancel();
        assertEquals(RunStatus.CANCELLED, run.getStatus()); assertNotNull(run.getFinishedAt());
    }
}
