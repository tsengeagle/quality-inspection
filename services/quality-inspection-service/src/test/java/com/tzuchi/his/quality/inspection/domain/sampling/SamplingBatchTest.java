package com.tzuchi.his.quality.inspection.domain.sampling;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SamplingBatchTest {

    @Test
    void start_shouldCreateBatchInStartedStatus() {
        SamplingBatch batch = createBatch("b1", "p1", ExecutionMode.MANUAL);

        assertEquals("b1", batch.getBatchId());
        assertEquals(BatchStatus.STARTED, batch.getStatus());
        assertNotNull(batch.getStartedAt());
    }

    @Test
    void selectCases_noDuplicates_shouldSucceed() {
        SamplingBatch batch = createBatch("b1", "p1", ExecutionMode.MANUAL);
        batch.recordCandidateCount(10, 8, 2);

        List<SelectedCaseRef> cases = Arrays.asList(
                new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT),
                new SelectedCaseRef("C2", "H1", MedicalRecordType.INPATIENT)
        );
        batch.selectCases(cases);

        assertEquals(2, batch.getSelectedCount());
        assertEquals(BatchStatus.SAMPLING, batch.getStatus());
    }

    @Test
    void selectCases_duplicateInSameBatch_shouldThrow() {
        SamplingBatch batch = createBatch("b1", "p1", ExecutionMode.MANUAL);
        batch.recordCandidateCount(10, 8, 2);

        List<SelectedCaseRef> cases = Arrays.asList(
                new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT),
                new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT)
        );

        assertThrows(IllegalArgumentException.class, () -> batch.selectCases(cases));
    }

    @Test
    void selectCases_duplicateWithExisting_shouldThrow() {
        SamplingBatch batch = createBatch("b1", "p1", ExecutionMode.MANUAL);
        batch.recordCandidateCount(10, 8, 2);

        batch.selectCases(List.of(
                new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT)
        ));

        assertThrows(IllegalArgumentException.class, () ->
                batch.selectCases(List.of(
                        new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT)
                ))
        );
    }

    @Test
    void complete_shouldTransitionToCompleted() {
        SamplingBatch batch = createBatch("b1", "p1", ExecutionMode.MANUAL);
        batch.recordCandidateCount(10, 8, 2);
        batch.selectCases(List.of(
                new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT)
        ));

        batch.complete();

        assertEquals(BatchStatus.COMPLETED, batch.getStatus());
        assertNotNull(batch.getFinishedAt());
    }

    @Test
    void complete_selectedExceedsEligible_shouldThrow() {
        SamplingBatch batch = createBatch("b1", "p1", ExecutionMode.MANUAL);
        batch.recordCandidateCount(2, 1, 1);
        batch.selectCases(List.of(
                new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT),
                new SelectedCaseRef("C2", "H1", MedicalRecordType.OUTPATIENT)
        ));

        assertThrows(IllegalStateException.class, batch::complete);
    }

    @Test
    void completedBatch_isImmutable() {
        SamplingBatch batch = createBatch("b1", "p1", ExecutionMode.MANUAL);
        batch.recordCandidateCount(10, 8, 2);
        batch.selectCases(List.of(
                new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT)
        ));
        batch.complete();

        assertThrows(IllegalStateException.class,
                () -> batch.recordCandidateCount(5, 5, 0));
        assertThrows(IllegalStateException.class,
                () -> batch.selectCases(List.of(
                        new SelectedCaseRef("C99", "H1", MedicalRecordType.OUTPATIENT)
                )));
    }

    @Test
    void fail_shouldTransitionToFailed() {
        SamplingBatch batch = createBatch("b1", "p1", ExecutionMode.MANUAL);

        batch.fail("test error");

        assertEquals(BatchStatus.FAILED, batch.getStatus());
    }

    @Test
    void fail_completedBatch_shouldThrow() {
        SamplingBatch batch = createBatch("b1", "p1", ExecutionMode.MANUAL);
        batch.recordCandidateCount(10, 8, 2);
        batch.selectCases(List.of(
                new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT)
        ));
        batch.complete();

        assertThrows(IllegalStateException.class, () -> batch.fail("cannot fail completed"));
    }

    @Test
    void markRetryOf_retryMode_shouldSucceed() {
        SamplingBatch batch = createBatch("b2", "p1", ExecutionMode.RETRY);

        batch.markRetryOf("b1");

        assertEquals("b1", batch.getRetryOfBatchId());
    }

    @Test
    void markRetryOf_nonRetryMode_shouldThrow() {
        SamplingBatch batch = createBatch("b1", "p1", ExecutionMode.MANUAL);

        assertThrows(IllegalStateException.class, () -> batch.markRetryOf("b0"));
    }

    @Test
    void negativeMetrics_shouldThrow() {
        SamplingBatch batch = createBatch("b1", "p1", ExecutionMode.MANUAL);

        assertThrows(IllegalArgumentException.class,
                () -> batch.recordCandidateCount(-1, 0, 0));
    }

    private SamplingBatch createBatch(String batchId, String projectId, ExecutionMode mode) {
        return SamplingBatch.start(batchId, projectId, mode,
                new RuleSnapshot("project-v1", "default-policy"));
    }
}
