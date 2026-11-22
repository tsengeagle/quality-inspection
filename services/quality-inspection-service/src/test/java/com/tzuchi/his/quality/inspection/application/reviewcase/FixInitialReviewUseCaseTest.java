package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewerAssignment;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseEscalatedToRereviewEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.InitialReviewFixedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewProjectRepository;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * D-054：初審固定。檢查順序依 D-024（角色 → 載入 → 院區 → 領域規則），錯誤代碼依 D-054 第 6 點。
 */
class FixInitialReviewUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-16T10:00:00Z");

    private InMemoryReviewCaseRepository cases;
    private InMemoryReviewSubmissionRepository submissions;
    private InMemoryReviewProjectRepository projects;
    private CapturingDomainEventPublisher events;
    private FixInitialReviewUseCase useCase;

    @BeforeEach
    void setUp() {
        cases = new InMemoryReviewCaseRepository();
        submissions = new InMemoryReviewSubmissionRepository();
        projects = new InMemoryReviewProjectRepository();
        events = new CapturingDomainEventPublisher();
        useCase = new FixInitialReviewUseCase(cases, submissions, projects, events, Clock.fixed(NOW, ZoneOffset.UTC));
        project(new BigDecimal("8"));
        completedCase(CaseStatus.INITIAL_REVIEW_COMPLETED, "i1");
        submission("i1", ReviewStage.INITIAL, "doctor", SubmissionStatus.SUBMITTED, new BigDecimal("7"));
    }

    private void project(BigDecimal threshold) {
        projects.save(ReviewProject.reconstitute("p1", "H001", "Project", null, "ss:v1", null,
                ProjectStatus.ACTIVE, 1, true, threshold, new BigDecimal("0.5"), new BigDecimal("0.5")));
    }

    private void completedCase(CaseStatus status, String latestInitial) {
        cases.save(ReviewCase.reconstitute("c1", "b1", "p1", "H001", "SK-1", MedicalRecordType.OUTPATIENT,
                status, new ReviewerAssignment("doctor", LocalDateTime.of(2026, 1, 1, 0, 0)), null,
                null, null, null, null, false, null, null, latestInitial, null, "ss:v1",
                null, null, new BigDecimal("7"), null));
    }

    private void submission(String id, ReviewStage stage, String owner, SubmissionStatus status, BigDecimal score) {
        submissions.save(ReviewSubmission.reconstitute(id, "c1", stage, owner, status, 1, List.of(), score,
                null, LocalDateTime.of(2026, 1, 2, 0, 0), "ss:v1", null, null,
                LocalDateTime.of(2026, 1, 1, 0, 0), null));
    }

    private static ActorContext actor(DomainRole role, String user, String hospital) {
        return new ActorContext(user, user, EnumSet.of(role), Optional.of(hospital));
    }

    private static ActorContext doctor() {
        return actor(DomainRole.REVIEWER, "doctor", "H001");
    }

    private ReviewCase stored() {
        return cases.store.get("c1");
    }

    private String errorCode(Runnable action) {
        RuntimeException e = assertThrows(RuntimeException.class, action::run);
        if (e instanceof DomainStateConflictException) return ((DomainStateConflictException) e).getErrorCode();
        if (e instanceof AccessDeniedException) return ((AccessDeniedException) e).getErrorCode();
        if (e instanceof RoleDeniedException) return ((RoleDeniedException) e).getErrorCode();
        if (e instanceof ResourceNotFoundException) return ((ResourceNotFoundException) e).getErrorCode();
        throw new AssertionError("unexpected exception", e);
    }

    // --- 成功與流向（D-054 第 2、3、4、8 點）---

    @Test
    void scoreAtOrBelowThreshold_recordsTheFixationAndStaysCompleted() {
        FixInitialReviewResult result = useCase.execute(doctor(), "c1", "i1");

        assertEquals("c1", result.getCaseId());
        assertEquals("INITIAL_REVIEW_COMPLETED", result.getCaseStatus());
        assertEquals("i1", result.getSubmissionId());
        assertEquals(NOW, result.getFixedAt());
        ReviewCase rc = stored();
        assertTrue(rc.isInitialReviewFixed());
        assertEquals(NOW, rc.getInitialFixedAt());
        assertEquals("doctor", rc.getInitialFixedBy());
        assertEquals("i1", rc.getInitialFixedSubmissionId());
        assertEquals(CaseStatus.INITIAL_REVIEW_COMPLETED, rc.getStatus());
        assertEquals(1, events.events.size());
        InitialReviewFixedEvent fixed = (InitialReviewFixedEvent) events.events.get(0);
        assertEquals("INITIAL_REVIEW_FIXED", fixed.getAction());
        assertEquals("{\"submissionId\":\"i1\"}", fixed.getDetail(), "the audit detail must not carry the score");
    }

    @Test
    void scoreAboveThreshold_movesToPendingRereviewAndPublishesTheEscalation() {
        submission("i1", ReviewStage.INITIAL, "doctor", SubmissionStatus.SUBMITTED, new BigDecimal("9"));

        FixInitialReviewResult result = useCase.execute(doctor(), "c1", "i1");

        assertEquals("PENDING_REREVIEW", result.getCaseStatus());
        assertEquals(CaseStatus.PENDING_REREVIEW, stored().getStatus());
        assertTrue(stored().isRereviewRequired());
        assertEquals(2, events.events.size());
        assertInstanceOf(InitialReviewFixedEvent.class, events.events.get(0));
        assertInstanceOf(ReviewCaseEscalatedToRereviewEvent.class, events.events.get(1));
    }

    @Test
    void nullThreshold_staysCompletedForTheManagerToDecide() {
        project(null);
        submission("i1", ReviewStage.INITIAL, "doctor", SubmissionStatus.SUBMITTED, new BigDecimal("9"));

        assertEquals("INITIAL_REVIEW_COMPLETED", useCase.execute(doctor(), "c1", "i1").getCaseStatus());
    }

    // --- 錯誤代碼（D-054 第 6 點）---

    @Test
    void notAReviewer_isRoleDenied() {
        assertEquals("ROLE_DENIED", errorCode(() -> useCase.execute(actor(DomainRole.HOSPITAL_MANAGER, "m", "H001"), "c1", "i1")));
    }

    @Test
    void unknownCaseOrSubmission_isNotFound() {
        assertEquals("CASE_NOT_FOUND", errorCode(() -> useCase.execute(doctor(), "no-such", "i1")));
        assertEquals("SUBMISSION_NOT_FOUND", errorCode(() -> useCase.execute(doctor(), "c1", "no-such")));
    }

    @Test
    void otherHospital_isScopeMismatch() {
        assertEquals("HOSPITAL_SCOPE_MISMATCH", errorCode(() -> useCase.execute(actor(DomainRole.REVIEWER, "doctor", "H999"), "c1", "i1")));
    }

    @Test
    void terminalCase_isCaseTerminal() {
        completedCase(CaseStatus.CANCELLED, "i1");
        assertEquals("CASE_TERMINAL", errorCode(() -> useCase.execute(doctor(), "c1", "i1")));
    }

    @Test
    void repeatedFix_isInvalidStateTransitionAndChangesNothing() {
        useCase.execute(doctor(), "c1", "i1");
        Instant firstFixedAt = stored().getInitialFixedAt();

        assertEquals("INVALID_STATE_TRANSITION", errorCode(() -> useCase.execute(doctor(), "c1", "i1")));
        assertEquals(firstFixedAt, stored().getInitialFixedAt());
        assertEquals(1, events.events.size());
    }

    @Test
    void caseNotCompleted_isCaseNotInitialReviewCompleted() {
        completedCase(CaseStatus.INITIAL_REVIEW_IN_PROGRESS, "i1");
        assertEquals("CASE_NOT_INITIAL_REVIEW_COMPLETED", errorCode(() -> useCase.execute(doctor(), "c1", "i1")));
    }

    @Test
    void targetThatIsNotTheCurrentInitialResult_isConflict() {
        submission("old", ReviewStage.INITIAL, "doctor", SubmissionStatus.SUPERSEDED, new BigDecimal("7"));
        submission("rr", ReviewStage.REREVIEW, "doctor", SubmissionStatus.SUBMITTED, new BigDecimal("7"));
        assertEquals("CONFLICT", errorCode(() -> useCase.execute(doctor(), "c1", "old")));
        assertEquals("CONFLICT", errorCode(() -> useCase.execute(doctor(), "c1", "rr")));
        assertFalse(stored().isInitialReviewFixed());
    }

    @Test
    void draftTarget_isSubmissionNotSubmitted() {
        submission("i1", ReviewStage.INITIAL, "doctor", SubmissionStatus.DRAFT, new BigDecimal("7"));
        assertEquals("SUBMISSION_NOT_SUBMITTED", errorCode(() -> useCase.execute(doctor(), "c1", "i1")));
    }

    @Test
    void someoneElse_isNotSubmissionOwner() {
        assertEquals("NOT_SUBMISSION_OWNER", errorCode(() -> useCase.execute(actor(DomainRole.REVIEWER, "other", "H001"), "c1", "i1")));
        assertFalse(stored().isInitialReviewFixed());
        assertTrue(events.events.isEmpty());
    }
}
