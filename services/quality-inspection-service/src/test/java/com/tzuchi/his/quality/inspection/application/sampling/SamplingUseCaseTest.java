package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.sampling.*;
import com.tzuchi.his.quality.inspection.domain.sampling.event.*;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SamplingUseCaseTest {

    private InMemoryReviewProjectRepository projectRepo;
    private InMemorySamplingBatchRepo batchRepo;
    private InMemoryReviewCaseRepo caseRepo;
    private StubCandidateSource candidateSource;
    private CapturingDomainEventPublisher eventPublisher;

    private PreviewSamplingPopulationUseCase previewUseCase;
    private ExecuteSamplingUseCase executeUseCase;
    private RetrySamplingBatchUseCase retryUseCase;

    private ActorContext hospitalManagerActor;

    @BeforeEach
    void setUp() {
        projectRepo = new InMemoryReviewProjectRepository();
        batchRepo = new InMemorySamplingBatchRepo();
        caseRepo = new InMemoryReviewCaseRepo();
        candidateSource = new StubCandidateSource();
        eventPublisher = new CapturingDomainEventPublisher();

        previewUseCase = new PreviewSamplingPopulationUseCase(projectRepo, candidateSource);
        executeUseCase = new ExecuteSamplingUseCase(projectRepo, batchRepo, caseRepo, candidateSource, eventPublisher);
        retryUseCase = new RetrySamplingBatchUseCase(projectRepo, batchRepo, caseRepo, candidateSource, eventPublisher);

        hospitalManagerActor = new ActorContext("u-test", "Test", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
    }

    @Test
    void preview_shouldNotCreateBatch() {
        ReviewProject project = createActiveProject("p1");
        projectRepo.save(project);
        candidateSource.setCandidates(List.of(
                new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT),
                new SelectedCaseRef("C2", "H1", MedicalRecordType.OUTPATIENT)
        ));

        SamplingPreviewResult result = previewUseCase.execute(hospitalManagerActor, "p1");

        assertEquals(2, result.getPopulationCount());
        assertEquals(2, result.getEligibleCount());
        assertTrue(batchRepo.store.isEmpty(), "Preview must NOT create a batch");
        assertTrue(caseRepo.store.isEmpty(), "Preview must NOT create review cases");
    }

    @Test
    void execute_shouldCreateBatchAndReviewCases() {
        ReviewProject project = createActiveProject("p1");
        projectRepo.save(project);
        candidateSource.setCandidates(List.of(
                new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT),
                new SelectedCaseRef("C2", "H1", MedicalRecordType.INPATIENT)
        ));

        String batchId = executeUseCase.execute(hospitalManagerActor, "p1");

        assertNotNull(batchId);
        assertEquals(1, batchRepo.store.size());
        SamplingBatch batch = batchRepo.store.values().iterator().next();
        assertEquals(BatchStatus.COMPLETED, batch.getStatus());
        assertEquals(2, batch.getSelectedCount());
        assertEquals(2, caseRepo.store.size(), "Should create 2 review cases");
    }

    @Test
    void execute_createsReviewCasesWithCorrectBatchId() {
        ReviewProject project = createActiveProject("p1");
        projectRepo.save(project);
        candidateSource.setCandidates(List.of(
                new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT)
        ));

        String batchId = executeUseCase.execute(hospitalManagerActor, "p1");

        ReviewCase rc = caseRepo.store.values().iterator().next();
        assertEquals(batchId, rc.getBatchId());
        assertEquals("p1", rc.getProjectId());
    }

    @Test
    void retry_shouldCreateNewBatch_notOverwriteOldBatch() {
        ReviewProject project = createActiveProject("p1");
        projectRepo.save(project);

        SamplingBatch failedBatch = SamplingBatch.start("failed-b", "p1",
                ExecutionMode.MANUAL, new RuleSnapshot("PV-p1-v1", 1, "SS-001-v1", "MANUAL", null));
        failedBatch.fail("test failure");
        batchRepo.save(failedBatch);

        candidateSource.setCandidates(List.of(
                new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT)
        ));

        String newBatchId = retryUseCase.execute(hospitalManagerActor, "failed-b");

        assertNotEquals("failed-b", newBatchId, "Retry creates a NEW batch");
        assertEquals(2, batchRepo.store.size(), "Old batch + new batch both exist");

        SamplingBatch oldBatch = batchRepo.store.get("failed-b");
        assertEquals(BatchStatus.FAILED, oldBatch.getStatus(), "Old batch remains FAILED");

        SamplingBatch newBatch = batchRepo.store.get(newBatchId);
        assertEquals(BatchStatus.COMPLETED, newBatch.getStatus());
        assertEquals("failed-b", newBatch.getRetryOfBatchId());
        assertEquals(ExecutionMode.RETRY, newBatch.getExecutionMode());
    }

    @Test
    void retry_nonFailedBatch_shouldThrow() {
        ReviewProject project = createActiveProject("p1");
        projectRepo.save(project);

        SamplingBatch completedBatch = SamplingBatch.start("done-b", "p1",
                ExecutionMode.MANUAL, new RuleSnapshot("PV-p1-v1", 1, "SS-001-v1", "MANUAL", null));
        completedBatch.recordCandidateCount(1, 1, 0);
        completedBatch.selectCases(List.of(
                new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT)
        ));
        completedBatch.complete();
        batchRepo.save(completedBatch);

        assertThrows(DomainRuleViolationException.class, () -> retryUseCase.execute(hospitalManagerActor, "done-b"));
    }

    @Test
    void execute_inactiveProject_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "H1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        projectRepo.save(project);

        assertThrows(DomainRuleViolationException.class, () -> executeUseCase.execute(hospitalManagerActor, "p1"));
    }

    @Test
    void retry_shouldUseSnapshotFromPreviousBatch() {
        ReviewProject project = createActiveProject("p1");
        projectRepo.save(project);

        RuleSnapshot originalSnapshot = new RuleSnapshot("PV-p1-v1", 1, "SS-001-v1", "original-policy", null);
        SamplingBatch failedBatch = SamplingBatch.start("failed-b2", "p1",
                ExecutionMode.MANUAL, originalSnapshot);
        failedBatch.fail("test failure");
        batchRepo.save(failedBatch);

        candidateSource.setCandidates(List.of());

        String newBatchId = retryUseCase.execute(hospitalManagerActor, "failed-b2");

        SamplingBatch newBatch = batchRepo.store.get(newBatchId);
        assertEquals("PV-p1-v1",
                newBatch.getRuleSnapshot().getProjectVersionId(),
                "Retry batch must use original batch's snapshot, not current project version");
        assertEquals("original-policy",
                newBatch.getRuleSnapshot().getSamplingPolicy());
    }

    @Test
    void preview_inactiveProject_shouldThrow() {
        ReviewProject project = ReviewProject.create("p-draft", "H1", "Draft Project",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        projectRepo.save(project);

        assertThrows(DomainRuleViolationException.class,
                () -> previewUseCase.execute(hospitalManagerActor, "p-draft"));
    }

    @Test
    void execute_shouldEmit4Events() {
        ReviewProject project = createActiveProject("p1");
        projectRepo.save(project);
        candidateSource.setCandidates(List.of(
                new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT)
        ));

        String batchId = executeUseCase.execute(hospitalManagerActor, "p1");

        assertEquals(4, eventPublisher.events.size());

        SamplingBatchStartedEvent started = assertInstanceOf(SamplingBatchStartedEvent.class, eventPublisher.events.get(0));
        assertEquals("u-test", started.getActorUserId());
        assertEquals("Test", started.getActorUserName());
        assertEquals("H001", started.getHospitalId());
        assertEquals("SamplingBatch", started.getEntityType());
        assertEquals(batchId, started.getEntityId());
        assertEquals("STARTED", started.getAction());

        SamplingCandidatesPreparedEvent prepared = assertInstanceOf(SamplingCandidatesPreparedEvent.class, eventPublisher.events.get(1));
        assertEquals(batchId, prepared.getEntityId());
        assertEquals("CANDIDATES_PREPARED", prepared.getAction());
        assertTrue(prepared.getDetail().contains("\"populationCount\":1"));

        ReviewCasesSelectedEvent selected = assertInstanceOf(ReviewCasesSelectedEvent.class, eventPublisher.events.get(2));
        assertEquals(batchId, selected.getEntityId());
        assertEquals("CASES_SELECTED", selected.getAction());
        assertTrue(selected.getDetail().contains("\"selectedCount\":1"));

        SamplingBatchCompletedEvent completed = assertInstanceOf(SamplingBatchCompletedEvent.class, eventPublisher.events.get(3));
        assertEquals(batchId, completed.getEntityId());
        assertEquals("COMPLETED", completed.getAction());
        assertTrue(completed.getDetail().contains("\"selectedCount\":1"));
    }

    @Test
    void retry_shouldEmit4Events() {
        ReviewProject project = createActiveProject("p1");
        projectRepo.save(project);

        SamplingBatch failedBatch = SamplingBatch.start("failed-ev", "p1",
                ExecutionMode.MANUAL, new RuleSnapshot("PV-p1-v1", 1, "SS-001-v1", "MANUAL", null));
        failedBatch.fail("test failure");
        batchRepo.save(failedBatch);

        candidateSource.setCandidates(List.of(
                new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT)
        ));

        String newBatchId = retryUseCase.execute(hospitalManagerActor, "failed-ev");

        assertEquals(4, eventPublisher.events.size());

        SamplingBatchStartedEvent started = assertInstanceOf(SamplingBatchStartedEvent.class, eventPublisher.events.get(0));
        assertEquals("u-test", started.getActorUserId());
        assertEquals("H001", started.getHospitalId());
        assertEquals(newBatchId, started.getEntityId());
        assertEquals("STARTED", started.getAction());

        assertInstanceOf(SamplingCandidatesPreparedEvent.class, eventPublisher.events.get(1));
        assertInstanceOf(ReviewCasesSelectedEvent.class, eventPublisher.events.get(2));

        SamplingBatchCompletedEvent completed = assertInstanceOf(SamplingBatchCompletedEvent.class, eventPublisher.events.get(3));
        assertEquals(newBatchId, completed.getEntityId());
        assertEquals("COMPLETED", completed.getAction());
    }

    // --- Helpers ---

    private ReviewProject createActiveProject(String projectId) {
        ReviewProject project = ReviewProject.create(projectId, "H1", "Test Project",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT, MedicalRecordType.INPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-test");
        project.activate();
        return project;
    }

    // --- In-memory repos for unit testing ---

    static class InMemorySamplingBatchRepo implements SamplingBatchRepository {
        final Map<String, SamplingBatch> store = new HashMap<>();

        @Override
        public void save(SamplingBatch batch) {
            store.put(batch.getBatchId(), batch);
        }

        @Override
        public Optional<SamplingBatch> findById(String batchId) {
            return Optional.ofNullable(store.get(batchId));
        }
    }

    static class InMemoryReviewCaseRepo implements com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository {
        final Map<String, ReviewCase> store = new HashMap<>();

        @Override
        public void save(ReviewCase reviewCase) {
            store.put(reviewCase.getCaseId(), reviewCase);
        }

        @Override
        public void saveAll(List<ReviewCase> cases) {
            cases.forEach(this::save);
        }
    }

    static class StubCandidateSource implements CandidateSourcePort {
        private List<SelectedCaseRef> candidates = new ArrayList<>();

        void setCandidates(List<SelectedCaseRef> candidates) {
            this.candidates = candidates;
        }

        @Override
        public List<SelectedCaseRef> queryCandidates(String projectId, String hospitalId,
                                                       List<MedicalRecordType> types) {
            return new ArrayList<>(candidates);
        }
    }
}