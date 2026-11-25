package com.tzuchi.his.quality.inspection.application.sampling;

import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.ParameterizedTest;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
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
import java.time.Instant;
import java.util.stream.Collectors;
import com.tzuchi.his.quality.inspection.application.common.Page;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;

class SamplingUseCaseTest {

    private InMemoryReviewProjectRepository projectRepo;
    private InMemorySamplingBatchRepo batchRepo;
    private InMemoryReviewCaseRepo caseRepo;
    private StubCandidateSource candidateSource;
    private CapturingDomainEventPublisher eventPublisher;

    private PreviewSamplingPopulationUseCase previewUseCase;
    private ExecuteSamplingUseCase executeUseCase;
    private RetrySamplingBatchUseCase retryUseCase;
    private GetSamplingExecuteContextUseCase executeContextUseCase;

    private ActorContext hospitalManagerActor;

    @BeforeEach
    void setUp() {
        projectRepo = new InMemoryReviewProjectRepository();
        batchRepo = new InMemorySamplingBatchRepo();
        caseRepo = new InMemoryReviewCaseRepo();
        candidateSource = new StubCandidateSource();
        eventPublisher = new CapturingDomainEventPublisher();

        previewUseCase = new PreviewSamplingPopulationUseCase(projectRepo, candidateSource);
        executeUseCase = new ExecuteSamplingUseCase(projectRepo, batchRepo, caseRepo, candidateSource, eventPublisher, Clock.systemDefaultZone(), () -> java.util.UUID.randomUUID().toString());
        retryUseCase = new RetrySamplingBatchUseCase(projectRepo, batchRepo, caseRepo, candidateSource, eventPublisher, Clock.systemDefaultZone(), () -> java.util.UUID.randomUUID().toString());
        executeContextUseCase = new GetSamplingExecuteContextUseCase(projectRepo, new InMemoryBatchQueryPort());

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
                ExecutionMode.MANUAL, new RuleSnapshot("PV-p1-v1", 1, "SS-001-v1", "MANUAL", null), Instant.now());
        failedBatch.fail("test failure", Instant.now());
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
                ExecutionMode.MANUAL, new RuleSnapshot("PV-p1-v1", 1, "SS-001-v1", "MANUAL", null), Instant.now());
        completedBatch.recordCandidateCount(1, 1, 0);
        completedBatch.selectCases(List.of(
                new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT)
        ));
        completedBatch.complete(Instant.now());
        batchRepo.save(completedBatch);

        assertThrows(DomainRuleViolationException.class, () -> retryUseCase.execute(hospitalManagerActor, "done-b"));
    }

    @Test
    void execute_inactiveProject_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        projectRepo.save(project);

        assertThrows(DomainRuleViolationException.class, () -> executeUseCase.execute(hospitalManagerActor, "p1"));
    }

    @Test
    void execute_unknownProject_shouldThrow404() {
        com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException e = assertThrows(
                com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException.class,
                () -> executeUseCase.execute(hospitalManagerActor, "missing"));
        assertEquals("PROJECT_NOT_FOUND", e.getErrorCode());
    }

    @Test
    void retry_shouldUseSnapshotFromPreviousBatch() {
        ReviewProject project = createActiveProject("p1");
        projectRepo.save(project);

        RuleSnapshot originalSnapshot = new RuleSnapshot("PV-p1-v1", 1, "SS-001-v1", "original-policy", null);
        SamplingBatch failedBatch = SamplingBatch.start("failed-b2", "p1",
                ExecutionMode.MANUAL, originalSnapshot, Instant.now());
        failedBatch.fail("test failure", Instant.now());
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
        ReviewProject project = ReviewProject.create("p-draft", "H001", "Draft Project",
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
                ExecutionMode.MANUAL, new RuleSnapshot("PV-p1-v1", 1, "SS-001-v1", "MANUAL", null), Instant.now());
        failedBatch.fail("test failure", Instant.now());
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

    @Test
    void listBatches_shouldDelegateToQueryPort() {
        InMemoryBatchQueryPort queryPort = new InMemoryBatchQueryPort();
        queryPort.summaries.add(new SamplingBatchSummary(
            "b1", "p1", "Project 1", ExecutionMode.MANUAL, BatchStatus.COMPLETED,
            10, 8, 2, 5, "PV-1", 1, null, Instant.now(), Instant.now()));
        ListSamplingBatchesUseCase listUseCase = new ListSamplingBatchesUseCase(queryPort);

        Page<SamplingBatchSummary> result =
                listUseCase.execute(hospitalManagerActor, null, null, null, null, 0, 20);

        assertEquals(1, result.getContent().size());
        assertEquals("b1", result.getContent().get(0).getBatchId());
    }

    @Test
    void listBatches_unauthorizedRole_shouldThrow() {
        InMemoryBatchQueryPort queryPort = new InMemoryBatchQueryPort();
        ListSamplingBatchesUseCase listUseCase = new ListSamplingBatchesUseCase(queryPort);
        ActorContext reviewerActor = new ActorContext("u-rev", "Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));

        assertThrows(RoleDeniedException.class,
                () -> listUseCase.execute(reviewerActor, null, null, null, null, 0, 20));
    }

    @ParameterizedTest
    @EnumSource(value = DomainRole.class, names = {"REVIEWER", "REREVIEWER"})
    void listBatches_reviewerRoles_areDenied(DomainRole role) {
        // api-spec.md §9.5 列出的角色是 HOSPITAL_MANAGER、AUDITOR、SYSTEM_ADMIN
        ListSamplingBatchesUseCase listUseCase = new ListSamplingBatchesUseCase(new InMemoryBatchQueryPort());
        ActorContext actor = new ActorContext("u1", role.name(), EnumSet.of(role), Optional.of("H001"));

        RoleDeniedException e = assertThrows(RoleDeniedException.class,
                () -> listUseCase.execute(actor, null, null, null, null, 0, 20));
        assertEquals("ROLE_DENIED", e.getErrorCode());
    }

    @ParameterizedTest
    @EnumSource(value = DomainRole.class, names = {"REVIEWER", "REREVIEWER"})
    void getDetail_reviewerRoles_areDeniedBeforeLoading(DomainRole role) {
        // api-spec.md §9.6 列出的角色是 SYSTEM_ADMIN、HOSPITAL_MANAGER、AUDITOR。
        // 存在與不存在的批次都回 ROLE_DENIED（D-024：先檢查角色再載入）
        InMemoryBatchQueryPort queryPort = new InMemoryBatchQueryPort();
        queryPort.details.put("b1", new SamplingBatchDetailView(
                "b1", "p1", ExecutionMode.MANUAL, BatchStatus.COMPLETED,
                10, 8, 2, 5, null, null, Instant.now(), Instant.now(),
                new RuleSnapshotSummary("PV-1", 1, "SS-1", "MANUAL"),
                List.of()));
        projectRepo.save(createActiveProject("p1"));
        GetSamplingBatchDetailUseCase detailUseCase = new GetSamplingBatchDetailUseCase(queryPort, projectRepo);
        ActorContext actor = new ActorContext("u1", role.name(), EnumSet.of(role), Optional.of("H001"));

        assertEquals("ROLE_DENIED",
                assertThrows(RoleDeniedException.class, () -> detailUseCase.execute(actor, "b1")).getErrorCode());
        assertEquals("ROLE_DENIED",
                assertThrows(RoleDeniedException.class, () -> detailUseCase.execute(actor, "nonexistent")).getErrorCode());
    }

    @Test
    void getDetail_notFound_shouldThrow404() {
        InMemoryBatchQueryPort queryPort = new InMemoryBatchQueryPort();
        GetSamplingBatchDetailUseCase detailUseCase = new GetSamplingBatchDetailUseCase(queryPort, projectRepo);

        assertThrows(com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException.class,
                () -> detailUseCase.execute(hospitalManagerActor, "nonexistent"));
    }

    @Test
    void export_shouldWrapDetailView() {
        InMemoryBatchQueryPort queryPort = new InMemoryBatchQueryPort();
        SamplingBatchDetailView detail = new SamplingBatchDetailView(
                "b1", "p1", ExecutionMode.MANUAL, BatchStatus.COMPLETED,
                10, 8, 2, 5, null, null, Instant.now(), Instant.now(),
                new RuleSnapshotSummary("PV-1", 1, "SS-1", "MANUAL"),
                List.of());
        queryPort.details.put("b1", detail);
        projectRepo.save(createActiveProject("p1"));
        ExportSamplingBatchResultUseCase exportUseCase = new ExportSamplingBatchResultUseCase(queryPort, projectRepo);

        SamplingBatchExportResult result = exportUseCase.execute(hospitalManagerActor, "b1");

        assertEquals("b1", result.getDetail().getBatchId());
    }

    @Test
    void export_otherHospital_shouldThrowAccessDenied() {
        InMemoryBatchQueryPort queryPort = new InMemoryBatchQueryPort();
        SamplingBatchDetailView detail = new SamplingBatchDetailView(
                "b1", "p1", ExecutionMode.MANUAL, BatchStatus.COMPLETED,
                10, 8, 2, 5, null, null, Instant.now(), Instant.now(),
                new RuleSnapshotSummary("PV-1", 1, "SS-1", "MANUAL"),
                List.of());
        queryPort.details.put("b1", detail);
        projectRepo.save(createActiveProject("p1"));
        ExportSamplingBatchResultUseCase exportUseCase = new ExportSamplingBatchResultUseCase(queryPort, projectRepo);
        ActorContext otherHospital = new ActorContext("u-other", "Other",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("OTHER_HOSPITAL"));

        assertThrows(AccessDeniedException.class, () -> exportUseCase.execute(otherHospital, "b1"));
    }

    @Test
    void getDetail_otherHospital_deniedExceptSystemAdmin() {
        InMemoryBatchQueryPort queryPort = new InMemoryBatchQueryPort();
        SamplingBatchDetailView detail = new SamplingBatchDetailView(
                "b1", "p1", ExecutionMode.MANUAL, BatchStatus.COMPLETED,
                10, 8, 2, 5, null, null, Instant.now(), Instant.now(),
                new RuleSnapshotSummary("PV-1", 1, "SS-1", "MANUAL"),
                List.of());
        queryPort.details.put("b1", detail);
        projectRepo.save(createActiveProject("p1"));
        GetSamplingBatchDetailUseCase detailUseCase = new GetSamplingBatchDetailUseCase(queryPort, projectRepo);
        ActorContext otherAuditor = new ActorContext("u-aud", "Auditor",
                EnumSet.of(DomainRole.AUDITOR), Optional.of("OTHER_HOSPITAL"));
        ActorContext admin = new ActorContext("u-adm", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        assertThrows(AccessDeniedException.class, () -> detailUseCase.execute(otherAuditor, "b1"));
        assertEquals("b1", detailUseCase.execute(hospitalManagerActor, "b1").getBatchId());
        assertEquals("b1", detailUseCase.execute(admin, "b1").getBatchId());
    }

    @Test
    void preview_otherHospital_shouldThrowAccessDenied() {
        projectRepo.save(createActiveProject("p1"));
        ActorContext otherHospital = new ActorContext("u-other", "Other",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("OTHER_HOSPITAL"));

        assertThrows(AccessDeniedException.class, () -> previewUseCase.execute(otherHospital, "p1"));
    }

    @Test
    void listBatches_nonAdmin_queriesOwnHospitalOnly() {
        InMemoryBatchQueryPort queryPort = new InMemoryBatchQueryPort();
        ListSamplingBatchesUseCase listUseCase = new ListSamplingBatchesUseCase(queryPort);
        ActorContext admin = new ActorContext("u-adm", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        listUseCase.execute(hospitalManagerActor, null, null, null, null, 0, 20);
        assertEquals("H001", queryPort.lastCriteria.getHospitalId());

        listUseCase.execute(admin, null, null, null, null, 0, 20);
        assertNull(queryPort.lastCriteria.getHospitalId());
    }

    @Test
    void listBatches_admin_requestedHospitalNarrowsScope_nonAdminIgnoresIt() {
        // D-047 第 1、2 點：SYSTEM_ADMIN 可以指定院區；其他角色的指定不採用
        InMemoryBatchQueryPort queryPort = new InMemoryBatchQueryPort();
        ListSamplingBatchesUseCase listUseCase = new ListSamplingBatchesUseCase(queryPort);
        ActorContext admin = new ActorContext("u-adm", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        listUseCase.execute(admin, null, null, null, "H002", 0, 20);
        assertEquals("H002", queryPort.lastCriteria.getHospitalId());

        listUseCase.execute(hospitalManagerActor, null, null, null, "H002", 0, 20);
        assertEquals("H001", queryPort.lastCriteria.getHospitalId());
    }

    // ---- D-086：重跑與預覽的檢查、SYSTEM_ADMIN 的詳情、列表的分頁 ----

    @Test
    void retry_unknownBatch_shouldThrowSamplingBatchNotFound() {
        // SB-G-04（D-086 第 3 點）：原本是 IllegalArgumentException（500）
        com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException e = assertThrows(
                com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException.class,
                () -> retryUseCase.execute(hospitalManagerActor, "nonexistent"));
        assertEquals("SAMPLING_BATCH_NOT_FOUND", e.getErrorCode());
    }

    @Test
    void retry_otherHospital_isDeniedBeforeTheStatusCheck() {
        // SB-G-05（D-086 第 3 點）：院區的檢查在「不是 FAILED」之前，FAILED 的批次也一樣擋下
        projectRepo.save(createActiveProject("p1"));
        SamplingBatch failed = SamplingBatch.start("failed-x", "p1",
                ExecutionMode.MANUAL, new RuleSnapshot("PV-p1-v1", 1, "SS-001-v1", "MANUAL", null), Instant.now());
        failed.fail("test failure", Instant.now());
        batchRepo.save(failed);
        SamplingBatch completed = SamplingBatch.start("done-x", "p1",
                ExecutionMode.MANUAL, new RuleSnapshot("PV-p1-v1", 1, "SS-001-v1", "MANUAL", null), Instant.now());
        completed.recordCandidateCount(0, 0, 0);
        completed.selectCases(List.of());
        completed.complete(Instant.now());
        batchRepo.save(completed);
        ActorContext otherHospital = new ActorContext("u-other", "Other",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("OTHER_HOSPITAL"));

        assertEquals("HOSPITAL_SCOPE_MISMATCH", assertThrows(AccessDeniedException.class,
                () -> retryUseCase.execute(otherHospital, "failed-x")).getErrorCode());
        assertEquals("HOSPITAL_SCOPE_MISMATCH", assertThrows(AccessDeniedException.class,
                () -> retryUseCase.execute(otherHospital, "done-x")).getErrorCode());
        assertEquals(2, batchRepo.store.size());
        assertTrue(eventPublisher.events.isEmpty());
    }

    @Test
    void preview_unknownProject_shouldThrowProjectNotFound() {
        // SB-G-02（D-086 第 9 點）：原本是 IllegalArgumentException（500）
        com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException e = assertThrows(
                com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException.class,
                () -> previewUseCase.execute(hospitalManagerActor, "nonexistent"));
        assertEquals("PROJECT_NOT_FOUND", e.getErrorCode());
    }

    @Test
    void getDetail_systemAdmin_hasNoSelectedCases_othersHaveThem() {
        // SB-DET-01、SB-DET-02（D-086 第 12 點）
        InMemoryBatchQueryPort queryPort = new InMemoryBatchQueryPort();
        queryPort.details.put("b1", new SamplingBatchDetailView(
                "b1", "p1", ExecutionMode.MANUAL, BatchStatus.COMPLETED,
                1, 1, 0, 1, null, null, Instant.now(), Instant.now(),
                new RuleSnapshotSummary("PV-1", 1, "SS-1", "MANUAL"),
                List.of(new SelectedCaseRefView("C1", "H001", "OUTPATIENT"))));
        projectRepo.save(createActiveProject("p1"));
        GetSamplingBatchDetailUseCase detailUseCase = new GetSamplingBatchDetailUseCase(queryPort, projectRepo);
        ActorContext admin = new ActorContext("u-adm", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());
        ActorContext auditor = new ActorContext("u-aud", "Auditor",
                EnumSet.of(DomainRole.AUDITOR), Optional.of("H001"));

        SamplingBatchDetailView forAdmin = detailUseCase.execute(admin, "b1");
        assertNull(forAdmin.getSelectedCases());
        assertEquals(1, forAdmin.getSelectedCount());
        assertEquals("SS-1", forAdmin.getRuleSnapshot().getScoreSheetVersionId());

        assertEquals(1, detailUseCase.execute(hospitalManagerActor, "b1").getSelectedCases().size());
        assertEquals(1, detailUseCase.execute(auditor, "b1").getSelectedCases().size());
    }

    @Test
    void listBatches_pagingDefaultsAndFilters() {
        // SB-LST-03、SB-LST-04（D-086 第 11 點）：size 預設 20、上限 100；page 負數當成 0
        InMemoryBatchQueryPort queryPort = new InMemoryBatchQueryPort();
        ListSamplingBatchesUseCase listUseCase = new ListSamplingBatchesUseCase(queryPort);

        listUseCase.execute(hospitalManagerActor, null, null, null, null, null, null);
        assertEquals(0, queryPort.lastCriteria.getPage());
        assertEquals(20, queryPort.lastCriteria.getSize());

        listUseCase.execute(hospitalManagerActor, null, null, null, null, -3, 500);
        assertEquals(0, queryPort.lastCriteria.getPage());
        assertEquals(100, queryPort.lastCriteria.getSize());

        listUseCase.execute(hospitalManagerActor, null, null, null, null, 2, 0);
        assertEquals(2, queryPort.lastCriteria.getPage());
        assertEquals(20, queryPort.lastCriteria.getSize());

        listUseCase.execute(hospitalManagerActor, "p1", "COMPLETED", "MANUAL", null, 0, 20);
        assertEquals("p1", queryPort.lastCriteria.getProjectId());
        assertEquals(BatchStatus.COMPLETED, queryPort.lastCriteria.getStatus());
        assertEquals(ExecutionMode.MANUAL, queryPort.lastCriteria.getExecutionMode());
    }
    @Test
    void execute_wrongHospital_shouldThrowAccessDenied() {
        ReviewProject project = createActiveProject("p1");
        projectRepo.save(project);
        ActorContext wrongHospital = new ActorContext("u-test", "Test",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("OTHER_HOSPITAL"));
        assertThrows(AccessDeniedException.class, () -> executeUseCase.execute(wrongHospital, "p1"));
    }

    @Test
    void getExecuteContext_activeProject_shouldReturnBootstrapContext() {
        ReviewProject project = createActiveProject("p1");
        projectRepo.save(project);

        SamplingExecuteContextView context = executeContextUseCase.execute(hospitalManagerActor, "p1");

        assertEquals("p1", context.getProjectId());
        assertEquals("Test Project", context.getProjectName());
        assertEquals("H001", context.getHospitalId());
        assertEquals("ACTIVE", context.getProjectStatus());
        assertEquals(1, context.getVersionNo());
        // 專案綁定的是評分表版本 id（D-041、D-050 第 1 點）
        assertEquals(com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of("ss-test", 1), context.getScoreSheetId());
        assertFalse(context.isPreviewReady());
        assertFalse(context.isExecuting());
        assertFalse(context.isExecuteFailed());
        assertNull(context.getExecutedBatchId());
        assertNull(context.getBlockedReason());
        assertNull(context.getPreviewStats());
        assertNull(context.getPostExecuteSummary());
    }

    @Test
    void getExecuteContext_failedLatestBatch_shouldExposeFailureSummary() {
        ReviewProject project = createActiveProject("p1");
        projectRepo.save(project);

        InMemoryBatchQueryPort queryPort = new InMemoryBatchQueryPort();
        queryPort.latestDetail = new SamplingBatchDetailView(
                "failed-batch", "p1", ExecutionMode.MANUAL, BatchStatus.FAILED,
                10, 8, 2, 0, null, "simulated failure",
                Instant.now(), Instant.now(),
                new RuleSnapshotSummary("PV-1", 1, "SS-1", "MANUAL"),
                List.of());
        GetSamplingExecuteContextUseCase useCase = new GetSamplingExecuteContextUseCase(projectRepo, queryPort);

        SamplingExecuteContextView context = useCase.execute(hospitalManagerActor, "p1");

        assertTrue(context.isExecuteFailed());
        assertNotNull(context.getPostExecuteSummary());
        assertEquals("failed", context.getPostExecuteSummary().getTone());
        assertEquals("failed-batch", context.getPostExecuteSummary().getBatchId());
        assertNotNull(context.getPostExecuteSummary().getStartedAt());
    }

    @Test
    void getExecuteContext_completedLatestBatch_shouldExposeExecutedBatchId() {
        ReviewProject project = createActiveProject("p1");
        projectRepo.save(project);

        InMemoryBatchQueryPort queryPort = new InMemoryBatchQueryPort();
        queryPort.latestDetail = new SamplingBatchDetailView(
                "completed-batch", "p1", ExecutionMode.MANUAL, BatchStatus.COMPLETED,
                10, 8, 2, 3, null, null,
                Instant.now(), Instant.now(),
                new RuleSnapshotSummary("PV-1", 1, "SS-1", "MANUAL"),
                List.of());
        GetSamplingExecuteContextUseCase useCase = new GetSamplingExecuteContextUseCase(projectRepo, queryPort);

        SamplingExecuteContextView context = useCase.execute(hospitalManagerActor, "p1");

        assertFalse(context.isExecuteFailed());
        assertEquals("completed-batch", context.getExecutedBatchId());
        assertNotNull(context.getPostExecuteSummary());
        assertEquals("success", context.getPostExecuteSummary().getTone());
        assertNotNull(context.getPostExecuteSummary().getFinishedAt());
    }

    @Test
    void getExecuteContext_draftProject_shouldExposePublishedVersionBlockedReason() {
        ReviewProject draftProject = ReviewProject.create("p-draft", "H001", "Draft Project",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        projectRepo.save(draftProject);

        SamplingExecuteContextView context = executeContextUseCase.execute(hospitalManagerActor, "p-draft");

        assertEquals("PROJECT_VERSION_NOT_PUBLISHED", context.getBlockedReason());
        assertNull(context.getExecutedBatchId());
    }

    // --- Helpers ---

    private ReviewProject createActiveProject(String projectId) {
        ReviewProject project = ReviewProject.create(projectId, "H001", "Test Project",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT, MedicalRecordType.INPATIENT));
        com.tzuchi.his.quality.inspection.testfixture.PublishableProjects.prepare(project);
        project.publishNewVersion();
        project.activate();
        return project;
    }

    // --- In-memory repos for unit testing ---

    static class InMemoryBatchQueryPort implements SamplingBatchQueryPort {
        final List<SamplingBatchSummary> summaries = new ArrayList<>();
        final Map<String, SamplingBatchDetailView> details = new HashMap<>();
        SamplingBatchDetailView latestDetail;
        SamplingBatchQueryCriteria lastCriteria;

        @Override
        public Page<SamplingBatchSummary> findBatches(SamplingBatchQueryCriteria criteria) {
            lastCriteria = criteria;
            List<SamplingBatchSummary> filtered = summaries.stream()
                    .filter(s -> criteria.getProjectId() == null || s.getProjectId().equals(criteria.getProjectId()))
                    .filter(s -> criteria.getStatus() == null || s.getStatus().equals(criteria.getStatus()))
                    .collect(Collectors.toList());
            return new Page<>(filtered, criteria.getPage(), criteria.getSize(), filtered.size());
        }

        @Override
        public Optional<SamplingBatchDetailView> findDetailById(String batchId) {
            return Optional.ofNullable(details.get(batchId));
        }

        @Override
        public Optional<SamplingBatchDetailView> findLatestDetailByProjectId(String projectId) {
            return Optional.ofNullable(latestDetail);
        }
    }

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

        @Override
        public Optional<ReviewCase> findById(String caseId) {
            return Optional.ofNullable(store.get(caseId));
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
