# B3a Assignment & Case Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand the ReviewCase aggregate with assignment lifecycle (assign, reassign, cancel, return, reopen), build ReviewSubmission basic framework, and expose 5 REST API endpoints — all with full test coverage.

**Architecture:** Domain-first approach consistent with B1/B2. ReviewCase aggregate gets 5 new domain methods + ReviewerAssignment VO. 4 domain event classes. 5 application use cases follow existing @Service/@Transactional pattern with ActorContext scope guards. Infrastructure layer expands JPA entity + adds findById. Controller exposes 5 POST command endpoints.

**Tech Stack:** Java 11, Spring Boot 2.7.18, JPA/Hibernate, H2 (test), JUnit 5, Gradle 7.6.4

**Design Spec:** `docs/superpowers/specs/2026-04-08-b3a-assignment-design.md`

**Build & Test:** `cd services/quality-inspection-service && ./gradlew test`

**Package Root:** `com.tzuchi.his.quality.inspection`

---

## File Structure

### Files to Create

| File | Responsibility |
|------|---------------|
| `domain/reviewcase/ReviewerAssignment.java` | Value object: reviewerId + assignedAt |
| `domain/reviewcase/submission/ReviewSubmission.java` | Domain model shell (getters only, no business methods) |
| `domain/reviewcase/submission/ReviewStage.java` | Enum: INITIAL, REREVIEW |
| `domain/reviewcase/submission/SubmissionStatus.java` | Enum: DRAFT, SUBMITTED, SUPERSEDED |
| `domain/reviewcase/submission/ReviewSubmissionRepository.java` | Repository interface (not implemented in B3a) |
| `domain/reviewcase/event/ReviewCaseAssignedEvent.java` | Domain event for assign + reopen |
| `domain/reviewcase/event/ReviewCaseReassignedEvent.java` | Domain event for reassign |
| `domain/reviewcase/event/ReviewCaseCancelledEvent.java` | Domain event for cancel |
| `domain/reviewcase/event/ReviewCaseReturnedEvent.java` | Domain event for return |
| `application/reviewcase/AssignReviewCaseUseCase.java` | Assign reviewer use case |
| `application/reviewcase/ReassignReviewCaseUseCase.java` | Reassign reviewer use case |
| `application/reviewcase/CancelReviewCaseUseCase.java` | Cancel case use case |
| `application/reviewcase/ReturnInitialReviewUseCase.java` | Return to initial review use case |
| `application/reviewcase/ReopenInitialReviewUseCase.java` | Reopen initial review use case |
| `interfaces/rest/ReviewCaseController.java` | REST controller with 5 POST endpoints |
| `interfaces/rest/dto/AssignReviewerRequest.java` | Request DTO: reviewerId, dueAt |
| `interfaces/rest/dto/ReassignReviewerRequest.java` | Request DTO: reviewerId, dueAt |
| `interfaces/rest/dto/CancelReviewCaseRequest.java` | Request DTO: reason |
| `interfaces/rest/dto/ReturnReviewCaseRequest.java` | Request DTO: reason |
| **Test files:** | |
| `test/.../domain/reviewcase/ReviewCaseTest.java` | Domain unit tests (11+ test cases) |
| `test/.../domain/reviewcase/ReviewerAssignmentTest.java` | VO unit tests |
| `test/.../testfixture/InMemoryReviewCaseRepository.java` | Test double for ReviewCaseRepository |
| `test/.../application/reviewcase/AssignReviewCaseUseCaseTest.java` | Application layer test |
| `test/.../application/reviewcase/ReassignReviewCaseUseCaseTest.java` | Application layer test |
| `test/.../application/reviewcase/CancelReviewCaseUseCaseTest.java` | Application layer test |
| `test/.../application/reviewcase/ReturnInitialReviewUseCaseTest.java` | Application layer test |
| `test/.../application/reviewcase/ReopenInitialReviewUseCaseTest.java` | Application layer test |
| `test/.../interfaces/rest/ReviewCaseApiTest.java` | API integration tests |

### Files to Modify

| File | Change |
|------|--------|
| `domain/reviewcase/ReviewCase.java` | Add 12 fields, 5 domain methods, reconstitute factory, getters |
| `domain/reviewcase/ReviewCaseRepository.java` | Add `findById(String caseId)` |
| `infrastructure/persistence/reviewcase/ReviewCaseEntity.java` | Add 14 columns with getters/setters |
| `infrastructure/persistence/reviewcase/ReviewCaseRepositoryImpl.java` | Add `findById()` + `toDomain()` + expand `toEntity()` |

> **All source paths below are relative to:**
> `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/`
>
> **All test paths below are relative to:**
> `services/quality-inspection-service/src/test/java/com/tzuchi/his/quality/inspection/`

---

## Task 1: ReviewerAssignment Value Object + Unit Test

**Files:**
- Create: `domain/reviewcase/ReviewerAssignment.java`
- Create (test): `domain/reviewcase/ReviewerAssignmentTest.java`

- [ ] **Step 1: Write ReviewerAssignment test**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReviewerAssignmentTest {

    @Test
    void create_withValidArgs_shouldSetFields() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 15, 10, 0);
        ReviewerAssignment assignment = new ReviewerAssignment("reviewer-1", now);

        assertEquals("reviewer-1", assignment.getReviewerId());
        assertEquals(now, assignment.getAssignedAt());
    }

    @Test
    void create_nullReviewerId_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> new ReviewerAssignment(null, LocalDateTime.now()));
    }

    @Test
    void create_nullAssignedAt_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> new ReviewerAssignment("reviewer-1", null));
    }

    @Test
    void equals_sameValues_shouldBeEqual() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 15, 10, 0);
        ReviewerAssignment a1 = new ReviewerAssignment("reviewer-1", now);
        ReviewerAssignment a2 = new ReviewerAssignment("reviewer-1", now);

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    @Test
    void equals_differentValues_shouldNotBeEqual() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 15, 10, 0);
        ReviewerAssignment a1 = new ReviewerAssignment("reviewer-1", now);
        ReviewerAssignment a2 = new ReviewerAssignment("reviewer-2", now);

        assertNotEquals(a1, a2);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewerAssignmentTest" --no-daemon -q`

Expected: FAIL — `ReviewerAssignment` class does not exist yet.

- [ ] **Step 3: Implement ReviewerAssignment**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value object representing the assignment of a reviewer to a ReviewCase.
 */
public class ReviewerAssignment {

    private final String reviewerId;
    private final LocalDateTime assignedAt;

    public ReviewerAssignment(String reviewerId, LocalDateTime assignedAt) {
        Objects.requireNonNull(reviewerId, "reviewerId must not be null");
        Objects.requireNonNull(assignedAt, "assignedAt must not be null");
        this.reviewerId = reviewerId;
        this.assignedAt = assignedAt;
    }

    public String getReviewerId() { return reviewerId; }

    public LocalDateTime getAssignedAt() { return assignedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewerAssignment that = (ReviewerAssignment) o;
        return reviewerId.equals(that.reviewerId) && assignedAt.equals(that.assignedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reviewerId, assignedAt);
    }

    @Override
    public String toString() {
        return "ReviewerAssignment{reviewerId='" + reviewerId + "', assignedAt=" + assignedAt + "}";
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewerAssignmentTest" --no-daemon -q`

Expected: PASS — all 5 tests green.

- [ ] **Step 5: Commit**

```bash
cd services/quality-inspection-service
git add src/main/java/com/tzuchi/his/quality/inspection/domain/reviewcase/ReviewerAssignment.java \
        src/test/java/com/tzuchi/his/quality/inspection/domain/reviewcase/ReviewerAssignmentTest.java
git commit -m "feat(b3a): add ReviewerAssignment value object with tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 2: ReviewCase Aggregate Expansion + Domain Tests

**Files:**
- Modify: `domain/reviewcase/ReviewCase.java`
- Create (test): `domain/reviewcase/ReviewCaseTest.java`

- [ ] **Step 1: Write ReviewCase domain tests**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReviewCaseTest {

    private static final LocalDateTime DUE = LocalDateTime.of(2026, 2, 1, 23, 59);

    // --- Helper ---

    private ReviewCase createPendingAssignment() {
        return ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
    }

    private ReviewCase createPendingInitialReview() {
        ReviewCase rc = createPendingAssignment();
        rc.assignReviewer("reviewer-1", DUE);
        return rc;
    }

    private ReviewCase createInProgress() {
        ReviewCase rc = createPendingInitialReview();
        rc.forceStatus(CaseStatus.INITIAL_REVIEW_IN_PROGRESS);
        return rc;
    }

    private ReviewCase createReturned() {
        ReviewCase rc = createInProgress();
        rc.returnToInitialReview("needs correction");
        return rc;
    }

    // --- createFromSampling ---

    @Test
    void createFromSampling_shouldStartAtSelectedPendingAssignment() {
        ReviewCase rc = createPendingAssignment();

        assertEquals(CaseStatus.SELECTED_PENDING_ASSIGNMENT, rc.getStatus());
        assertNull(rc.getReviewerAssignment());
        assertNull(rc.getDueAt());
    }

    // --- assignReviewer ---

    @Test
    void assignReviewer_fromSelectedPending_shouldSucceed() {
        ReviewCase rc = createPendingAssignment();

        rc.assignReviewer("reviewer-1", DUE);

        assertEquals(CaseStatus.PENDING_INITIAL_REVIEW, rc.getStatus());
        assertNotNull(rc.getReviewerAssignment());
        assertEquals("reviewer-1", rc.getReviewerAssignment().getReviewerId());
        assertEquals(DUE, rc.getDueAt());
    }

    @Test
    void assignReviewer_fromNonSelectedPending_shouldThrow() {
        ReviewCase rc = createPendingInitialReview(); // already PENDING_INITIAL_REVIEW

        assertThrows(DomainStateConflictException.class,
                () -> rc.assignReviewer("reviewer-2", DUE));
    }

    @Test
    void assignReviewer_nullReviewerId_shouldThrow() {
        ReviewCase rc = createPendingAssignment();

        assertThrows(NullPointerException.class,
                () -> rc.assignReviewer(null, DUE));
    }

    // --- reassignReviewer ---

    @Test
    void reassignReviewer_fromPendingInitialReview_shouldSucceed() {
        ReviewCase rc = createPendingInitialReview();
        LocalDateTime newDue = DUE.plusDays(7);

        rc.reassignReviewer("reviewer-2", newDue);

        assertEquals(CaseStatus.PENDING_INITIAL_REVIEW, rc.getStatus());
        assertEquals("reviewer-2", rc.getReviewerAssignment().getReviewerId());
        assertEquals(newDue, rc.getDueAt());
    }

    @Test
    void reassignReviewer_fromNonPendingInitialReview_shouldThrow() {
        ReviewCase rc = createPendingAssignment(); // SELECTED_PENDING_ASSIGNMENT

        assertThrows(DomainStateConflictException.class,
                () -> rc.reassignReviewer("reviewer-2", DUE));
    }

    // --- cancel ---

    @Test
    void cancel_fromSelectedPending_shouldSucceed() {
        ReviewCase rc = createPendingAssignment();

        rc.cancel("project cancelled");

        assertEquals(CaseStatus.CANCELLED, rc.getStatus());
        assertEquals("project cancelled", rc.getCancelReason());
        assertNotNull(rc.getCancelledAt());
    }

    @Test
    void cancel_fromPendingInitialReview_shouldSucceed() {
        ReviewCase rc = createPendingInitialReview();

        rc.cancel("reviewer unavailable");

        assertEquals(CaseStatus.CANCELLED, rc.getStatus());
    }

    @Test
    void cancel_fromInProgress_shouldSucceed() {
        ReviewCase rc = createInProgress();

        rc.cancel("duplicate case");

        assertEquals(CaseStatus.CANCELLED, rc.getStatus());
    }

    @Test
    void cancel_fromReturned_shouldSucceed() {
        ReviewCase rc = createReturned();

        rc.cancel("no longer needed");

        assertEquals(CaseStatus.CANCELLED, rc.getStatus());
    }

    @Test
    void cancel_fromClosed_shouldThrow() {
        ReviewCase rc = createPendingAssignment();
        rc.forceStatus(CaseStatus.CLOSED);

        assertThrows(DomainStateConflictException.class,
                () -> rc.cancel("too late"));
    }

    @Test
    void cancel_fromCancelled_shouldThrow() {
        ReviewCase rc = createPendingAssignment();
        rc.cancel("first cancel");

        assertThrows(DomainStateConflictException.class,
                () -> rc.cancel("second cancel"));
    }

    // --- returnToInitialReview ---

    @Test
    void returnToInitialReview_fromInProgress_shouldSucceed() {
        ReviewCase rc = createInProgress();

        rc.returnToInitialReview("needs correction");

        assertEquals(CaseStatus.RETURNED, rc.getStatus());
        assertEquals("needs correction", rc.getReturnReason());
    }

    @Test
    void returnToInitialReview_fromNonInProgress_shouldThrow() {
        ReviewCase rc = createPendingInitialReview(); // PENDING_INITIAL_REVIEW

        assertThrows(DomainStateConflictException.class,
                () -> rc.returnToInitialReview("nope"));
    }

    // --- reopenInitialReview ---

    @Test
    void reopenInitialReview_fromReturned_shouldSucceed() {
        ReviewCase rc = createReturned();

        rc.reopenInitialReview();

        assertEquals(CaseStatus.PENDING_INITIAL_REVIEW, rc.getStatus());
        assertNull(rc.getReturnReason());
    }

    @Test
    void reopenInitialReview_fromNonReturned_shouldThrow() {
        ReviewCase rc = createPendingInitialReview(); // PENDING_INITIAL_REVIEW

        assertThrows(DomainStateConflictException.class,
                () -> rc.reopenInitialReview());
    }

    // --- reconstitute ---

    @Test
    void reconstitute_shouldRestoreAllFields() {
        LocalDateTime assignedAt = LocalDateTime.of(2026, 1, 10, 9, 0);
        ReviewerAssignment assignment = new ReviewerAssignment("reviewer-1", assignedAt);

        ReviewCase rc = ReviewCase.reconstitute(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT,
                CaseStatus.PENDING_INITIAL_REVIEW, assignment, DUE,
                null, null, null, null,
                false, null, null, null, null, null);

        assertEquals("c1", rc.getCaseId());
        assertEquals(CaseStatus.PENDING_INITIAL_REVIEW, rc.getStatus());
        assertEquals("reviewer-1", rc.getReviewerAssignment().getReviewerId());
        assertEquals(DUE, rc.getDueAt());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseTest" --no-daemon -q`

Expected: FAIL — methods `assignReviewer`, `reassignReviewer`, `cancel`, `returnToInitialReview`, `reopenInitialReview`, `forceStatus`, `reconstitute` do not exist yet.

- [ ] **Step 3: Implement ReviewCase expansion**

Replace the entire `ReviewCase.java` with:

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Aggregate root for ReviewCase.
 * <p>
 * Created from sampling selection. Starts at SELECTED_PENDING_ASSIGNMENT.
 * B3a adds assignment lifecycle: assign, reassign, cancel, return, reopen.
 */
public class ReviewCase {

    // --- Existing fields (B2) ---
    private String caseId;
    private String batchId;
    private String projectId;
    private String hospitalId;
    private String sourceCaseKey;
    private MedicalRecordType medicalRecordType;
    private CaseStatus status;

    // --- B3a: assignment ---
    private ReviewerAssignment reviewerAssignment;
    private LocalDateTime dueAt;
    private String cancelReason;
    private String returnReason;
    private LocalDateTime cancelledAt;
    private LocalDateTime closedAt;

    // --- B3b/B3c: reserved (nullable, not used in B3a) ---
    private boolean rereviewRequired;
    private BigDecimal finalScore;
    private String finalConclusion;
    private String latestInitialSubmissionId;
    private String ruleSnapshotRef;
    private String scoreSheetVersionRef;

    protected ReviewCase() {
        // for persistence
    }

    private ReviewCase(String caseId, String batchId, String projectId,
                       String hospitalId, String sourceCaseKey,
                       MedicalRecordType medicalRecordType) {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(batchId, "batchId must not be null");
        Objects.requireNonNull(projectId, "projectId must not be null");
        this.caseId = caseId;
        this.batchId = batchId;
        this.projectId = projectId;
        this.hospitalId = hospitalId;
        this.sourceCaseKey = sourceCaseKey;
        this.medicalRecordType = medicalRecordType;
        this.status = CaseStatus.SELECTED_PENDING_ASSIGNMENT;
    }

    public static ReviewCase createFromSampling(String caseId, String batchId, String projectId,
                                                 String hospitalId, String sourceCaseKey,
                                                 MedicalRecordType medicalRecordType) {
        return new ReviewCase(caseId, batchId, projectId, hospitalId, sourceCaseKey, medicalRecordType);
    }

    /**
     * Reconstitute from persistence. All fields explicitly set — no business validation.
     */
    public static ReviewCase reconstitute(
            String caseId, String batchId, String projectId, String hospitalId,
            String sourceCaseKey, MedicalRecordType medicalRecordType,
            CaseStatus status, ReviewerAssignment reviewerAssignment, LocalDateTime dueAt,
            String cancelReason, String returnReason, LocalDateTime cancelledAt, LocalDateTime closedAt,
            boolean rereviewRequired, BigDecimal finalScore, String finalConclusion,
            String latestInitialSubmissionId, String ruleSnapshotRef, String scoreSheetVersionRef) {
        ReviewCase rc = new ReviewCase();
        rc.caseId = caseId;
        rc.batchId = batchId;
        rc.projectId = projectId;
        rc.hospitalId = hospitalId;
        rc.sourceCaseKey = sourceCaseKey;
        rc.medicalRecordType = medicalRecordType;
        rc.status = status;
        rc.reviewerAssignment = reviewerAssignment;
        rc.dueAt = dueAt;
        rc.cancelReason = cancelReason;
        rc.returnReason = returnReason;
        rc.cancelledAt = cancelledAt;
        rc.closedAt = closedAt;
        rc.rereviewRequired = rereviewRequired;
        rc.finalScore = finalScore;
        rc.finalConclusion = finalConclusion;
        rc.latestInitialSubmissionId = latestInitialSubmissionId;
        rc.ruleSnapshotRef = ruleSnapshotRef;
        rc.scoreSheetVersionRef = scoreSheetVersionRef;
        return rc;
    }

    // --- B3a Domain Methods ---

    public void assignReviewer(String reviewerId, LocalDateTime dueAt) {
        if (this.status != CaseStatus.SELECTED_PENDING_ASSIGNMENT) {
            throw new DomainStateConflictException("CASE_NOT_PENDING_ASSIGNMENT",
                    "Cannot assign reviewer: case status is " + this.status);
        }
        Objects.requireNonNull(reviewerId, "reviewerId must not be null");
        this.reviewerAssignment = new ReviewerAssignment(reviewerId, LocalDateTime.now());
        this.dueAt = dueAt;
        this.status = CaseStatus.PENDING_INITIAL_REVIEW;
    }

    public void reassignReviewer(String newReviewerId, LocalDateTime dueAt) {
        if (this.status != CaseStatus.PENDING_INITIAL_REVIEW) {
            throw new DomainStateConflictException("CASE_NOT_PENDING_INITIAL_REVIEW",
                    "Cannot reassign reviewer: case status is " + this.status);
        }
        Objects.requireNonNull(newReviewerId, "newReviewerId must not be null");
        this.reviewerAssignment = new ReviewerAssignment(newReviewerId, LocalDateTime.now());
        this.dueAt = dueAt;
    }

    public void cancel(String reason) {
        if (this.status == CaseStatus.CLOSED || this.status == CaseStatus.CANCELLED) {
            throw new DomainStateConflictException("CASE_TERMINAL",
                    "Cannot cancel: case is in terminal status " + this.status);
        }
        Objects.requireNonNull(reason, "cancel reason must not be null");
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
        this.status = CaseStatus.CANCELLED;
    }

    public void returnToInitialReview(String reason) {
        if (this.status != CaseStatus.INITIAL_REVIEW_IN_PROGRESS) {
            throw new DomainStateConflictException("CASE_NOT_IN_PROGRESS",
                    "Cannot return: case status is " + this.status);
        }
        Objects.requireNonNull(reason, "return reason must not be null");
        this.returnReason = reason;
        this.status = CaseStatus.RETURNED;
    }

    public void reopenInitialReview() {
        if (this.status != CaseStatus.RETURNED) {
            throw new DomainStateConflictException("CASE_NOT_RETURNED",
                    "Cannot reopen: case status is " + this.status);
        }
        this.returnReason = null;
        this.status = CaseStatus.PENDING_INITIAL_REVIEW;
    }

    /**
     * Test-only: force status for setting up test scenarios.
     * Package-private — only accessible from tests in the same package.
     */
    void forceStatus(CaseStatus status) {
        this.status = status;
    }

    // --- Getters ---

    public String getCaseId() { return caseId; }
    public String getBatchId() { return batchId; }
    public String getProjectId() { return projectId; }
    public String getHospitalId() { return hospitalId; }
    public String getSourceCaseKey() { return sourceCaseKey; }
    public MedicalRecordType getMedicalRecordType() { return medicalRecordType; }
    public CaseStatus getStatus() { return status; }
    public ReviewerAssignment getReviewerAssignment() { return reviewerAssignment; }
    public LocalDateTime getDueAt() { return dueAt; }
    public String getCancelReason() { return cancelReason; }
    public String getReturnReason() { return returnReason; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public boolean isRereviewRequired() { return rereviewRequired; }
    public BigDecimal getFinalScore() { return finalScore; }
    public String getFinalConclusion() { return finalConclusion; }
    public String getLatestInitialSubmissionId() { return latestInitialSubmissionId; }
    public String getRuleSnapshotRef() { return ruleSnapshotRef; }
    public String getScoreSheetVersionRef() { return scoreSheetVersionRef; }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseTest" --no-daemon -q`

Expected: PASS — all tests green.

- [ ] **Step 5: Run full test suite**

Run: `cd services/quality-inspection-service && ./gradlew test --no-daemon -q`

Expected: PASS — no regression.

- [ ] **Step 6: Commit**

```bash
cd services/quality-inspection-service
git add src/main/java/com/tzuchi/his/quality/inspection/domain/reviewcase/ReviewCase.java \
        src/test/java/com/tzuchi/his/quality/inspection/domain/reviewcase/ReviewCaseTest.java
git commit -m "feat(b3a): expand ReviewCase aggregate with assignment lifecycle methods

Add 12 new fields (assignment, cancel, return, reserved B3b/B3c fields).
Add 5 domain methods: assignReviewer, reassignReviewer, cancel,
returnToInitialReview, reopenInitialReview.
Add reconstitute factory for persistence rebuild.
Add comprehensive domain unit tests.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 3: ReviewSubmission Basic Framework

**Files:**
- Create: `domain/reviewcase/submission/ReviewSubmission.java`
- Create: `domain/reviewcase/submission/ReviewStage.java`
- Create: `domain/reviewcase/submission/SubmissionStatus.java`
- Create: `domain/reviewcase/submission/ReviewSubmissionRepository.java`

> No test file for this task: these are data-only classes with no behavior to test. Enums and getters-only classes are verified through integration in later tasks.

- [ ] **Step 1: Create ReviewStage enum**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.submission;

/**
 * The review stage of a submission.
 */
public enum ReviewStage {
    INITIAL,
    REREVIEW
}
```

- [ ] **Step 2: Create SubmissionStatus enum**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.submission;

/**
 * Lifecycle status of a ReviewSubmission.
 */
public enum SubmissionStatus {
    DRAFT,
    SUBMITTED,
    SUPERSEDED
}
```

- [ ] **Step 3: Create ReviewSubmission domain class**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.submission;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model for a review submission.
 * B3a: framework only — getters, no business methods.
 * Business methods (create draft, submit, supersede) will be added in B3b/B3c.
 */
public class ReviewSubmission {

    private String submissionId;
    private String caseId;
    private ReviewStage stage;
    private String reviewerId;
    private SubmissionStatus status;
    private int revisionNo;
    private BigDecimal totalScore;
    private String comment;
    private LocalDateTime submittedAt;
    private String basedOnScoreSheetVersionId;
    private String supersededBySubmissionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected ReviewSubmission() {
        // for persistence / future factory methods
    }

    // --- Getters ---

    public String getSubmissionId() { return submissionId; }
    public String getCaseId() { return caseId; }
    public ReviewStage getStage() { return stage; }
    public String getReviewerId() { return reviewerId; }
    public SubmissionStatus getStatus() { return status; }
    public int getRevisionNo() { return revisionNo; }
    public BigDecimal getTotalScore() { return totalScore; }
    public String getComment() { return comment; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public String getBasedOnScoreSheetVersionId() { return basedOnScoreSheetVersionId; }
    public String getSupersededBySubmissionId() { return supersededBySubmissionId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

- [ ] **Step 4: Create ReviewSubmissionRepository interface**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.submission;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ReviewSubmission.
 * B3a: interface only — no infrastructure implementation.
 * JPA implementation will be added in B3b.
 */
public interface ReviewSubmissionRepository {

    void save(ReviewSubmission submission);

    Optional<ReviewSubmission> findById(String submissionId);

    List<ReviewSubmission> findByCaseIdAndStage(String caseId, ReviewStage stage);

    Optional<ReviewSubmission> findLatestByCaseIdAndStage(String caseId, ReviewStage stage);
}
```

- [ ] **Step 5: Verify compilation**

Run: `cd services/quality-inspection-service && ./gradlew compileJava --no-daemon -q`

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
cd services/quality-inspection-service
git add src/main/java/com/tzuchi/his/quality/inspection/domain/reviewcase/submission/
git commit -m "feat(b3a): add ReviewSubmission basic framework (domain model + enums + repo interface)

Framework only — no business methods, no JPA persistence.
Business methods and persistence will be added in B3b.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 4: Domain Event Classes

**Files:**
- Create: `domain/reviewcase/event/ReviewCaseAssignedEvent.java`
- Create: `domain/reviewcase/event/ReviewCaseReassignedEvent.java`
- Create: `domain/reviewcase/event/ReviewCaseCancelledEvent.java`
- Create: `domain/reviewcase/event/ReviewCaseReturnedEvent.java`

- [ ] **Step 1: Create ReviewCaseAssignedEvent**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.time.LocalDateTime;

/**
 * Published when a reviewer is assigned (or when a case is reopened to PENDING_INITIAL_REVIEW).
 */
public class ReviewCaseAssignedEvent extends DomainEvent {

    public ReviewCaseAssignedEvent(Object source, String actorUserId, String actorUserName,
                                    String hospitalId, String caseId,
                                    String reviewerId, LocalDateTime dueAt) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewCase", caseId, "ASSIGNED",
              String.format("{\"reviewerId\":\"%s\",\"dueAt\":\"%s\"}", reviewerId, dueAt));
    }
}
```

- [ ] **Step 2: Create ReviewCaseReassignedEvent**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.time.LocalDateTime;

/**
 * Published when a case reviewer is changed (reassigned).
 */
public class ReviewCaseReassignedEvent extends DomainEvent {

    public ReviewCaseReassignedEvent(Object source, String actorUserId, String actorUserName,
                                      String hospitalId, String caseId,
                                      String previousReviewerId, String newReviewerId,
                                      LocalDateTime dueAt) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewCase", caseId, "REASSIGNED",
              String.format("{\"previousReviewerId\":\"%s\",\"newReviewerId\":\"%s\",\"dueAt\":\"%s\"}",
                      previousReviewerId, newReviewerId, dueAt));
    }
}
```

- [ ] **Step 3: Create ReviewCaseCancelledEvent**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

/**
 * Published when a case is cancelled.
 */
public class ReviewCaseCancelledEvent extends DomainEvent {

    public ReviewCaseCancelledEvent(Object source, String actorUserId, String actorUserName,
                                     String hospitalId, String caseId, String reason) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewCase", caseId, "CANCELLED",
              String.format("{\"reason\":\"%s\"}", reason));
    }
}
```

- [ ] **Step 4: Create ReviewCaseReturnedEvent**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

/**
 * Published when a case is returned to initial review.
 */
public class ReviewCaseReturnedEvent extends DomainEvent {

    public ReviewCaseReturnedEvent(Object source, String actorUserId, String actorUserName,
                                    String hospitalId, String caseId, String reason) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewCase", caseId, "RETURNED",
              String.format("{\"reason\":\"%s\"}", reason));
    }
}
```

- [ ] **Step 5: Verify compilation**

Run: `cd services/quality-inspection-service && ./gradlew compileJava --no-daemon -q`

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
cd services/quality-inspection-service
git add src/main/java/com/tzuchi/his/quality/inspection/domain/reviewcase/event/
git commit -m "feat(b3a): add 4 ReviewCase domain event classes

ReviewCaseAssignedEvent (also used for reopen),
ReviewCaseReassignedEvent, ReviewCaseCancelledEvent, ReviewCaseReturnedEvent.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 5: ReviewCaseRepository findById + InMemoryReviewCaseRepository Test Fixture

**Files:**
- Modify: `domain/reviewcase/ReviewCaseRepository.java`
- Create (test): `testfixture/InMemoryReviewCaseRepository.java`

- [ ] **Step 1: Add findById to ReviewCaseRepository**

Add to `ReviewCaseRepository.java`:

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase;

import java.util.List;
import java.util.Optional;

public interface ReviewCaseRepository {

    void save(ReviewCase reviewCase);

    void saveAll(List<ReviewCase> cases);

    Optional<ReviewCase> findById(String caseId);
}
```

- [ ] **Step 2: Create InMemoryReviewCaseRepository test fixture**

```java
package com.tzuchi.his.quality.inspection.testfixture;

import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;

import java.util.*;

public class InMemoryReviewCaseRepository implements ReviewCaseRepository {
    public final Map<String, ReviewCase> store = new HashMap<>();

    @Override
    public void save(ReviewCase reviewCase) {
        store.put(reviewCase.getCaseId(), reviewCase);
    }

    @Override
    public void saveAll(List<ReviewCase> cases) {
        cases.forEach(rc -> store.put(rc.getCaseId(), rc));
    }

    @Override
    public Optional<ReviewCase> findById(String caseId) {
        return Optional.ofNullable(store.get(caseId));
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd services/quality-inspection-service && ./gradlew compileJava compileTestJava --no-daemon -q`

Expected: FAIL — `ReviewCaseRepositoryImpl` does not implement `findById` yet. This is expected — infrastructure update comes in Task 7.

> Note: If the build fails due to `ReviewCaseRepositoryImpl` not implementing `findById`, that's expected. The compiler enforces the interface contract. We add the stub in Task 7.
> 
> **Workaround:** Temporarily add a stub `findById` to `ReviewCaseRepositoryImpl` to unblock compilation:

Add to `ReviewCaseRepositoryImpl.java`:

```java
@Override
public Optional<ReviewCase> findById(String caseId) {
    throw new UnsupportedOperationException("Not implemented yet — will be completed in Task 7");
}
```

Add import: `import java.util.Optional;`

- [ ] **Step 4: Verify compilation passes**

Run: `cd services/quality-inspection-service && ./gradlew compileJava compileTestJava --no-daemon -q`

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Run full test suite**

Run: `cd services/quality-inspection-service && ./gradlew test --no-daemon -q`

Expected: PASS — no regression.

- [ ] **Step 6: Commit**

```bash
cd services/quality-inspection-service
git add src/main/java/com/tzuchi/his/quality/inspection/domain/reviewcase/ReviewCaseRepository.java \
        src/main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/reviewcase/ReviewCaseRepositoryImpl.java \
        src/test/java/com/tzuchi/his/quality/inspection/testfixture/InMemoryReviewCaseRepository.java
git commit -m "feat(b3a): add ReviewCaseRepository.findById + InMemoryReviewCaseRepository test fixture

Infrastructure findById is a temporary stub — completed in Task 7.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 6: Application Use Cases + Tests

**Files:**
- Create: `application/reviewcase/AssignReviewCaseUseCase.java`
- Create: `application/reviewcase/ReassignReviewCaseUseCase.java`
- Create: `application/reviewcase/CancelReviewCaseUseCase.java`
- Create: `application/reviewcase/ReturnInitialReviewUseCase.java`
- Create: `application/reviewcase/ReopenInitialReviewUseCase.java`
- Create (test): `application/reviewcase/AssignReviewCaseUseCaseTest.java`
- Create (test): `application/reviewcase/ReassignReviewCaseUseCaseTest.java`
- Create (test): `application/reviewcase/CancelReviewCaseUseCaseTest.java`
- Create (test): `application/reviewcase/ReturnInitialReviewUseCaseTest.java`
- Create (test): `application/reviewcase/ReopenInitialReviewUseCaseTest.java`

### Step 6a: AssignReviewCaseUseCase

- [ ] **Step 1: Write AssignReviewCaseUseCaseTest**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseAssignedEvent;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AssignReviewCaseUseCaseTest {

    private InMemoryReviewCaseRepository repository;
    private CapturingDomainEventPublisher eventPublisher;
    private AssignReviewCaseUseCase useCase;

    private static final LocalDateTime DUE = LocalDateTime.of(2026, 2, 1, 23, 59);

    @BeforeEach
    void setUp() {
        repository = new InMemoryReviewCaseRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new AssignReviewCaseUseCase(repository, eventPublisher);
    }

    private ReviewCase createCase() {
        ReviewCase rc = ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        repository.save(rc);
        return rc;
    }

    private ActorContext hospitalManager() {
        return new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
    }

    @Test
    void execute_happyPath_shouldAssign() {
        createCase();
        useCase.execute(hospitalManager(), "c1", "reviewer-1", DUE);

        ReviewCase saved = repository.store.get("c1");
        assertEquals(CaseStatus.PENDING_INITIAL_REVIEW, saved.getStatus());
        assertEquals("reviewer-1", saved.getReviewerAssignment().getReviewerId());
    }

    @Test
    void execute_shouldPublishAssignedEvent() {
        createCase();
        useCase.execute(hospitalManager(), "c1", "reviewer-1", DUE);

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewCaseAssignedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext admin = new ActorContext("u1", "Admin", EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(admin, "c1", "reviewer-1", DUE));
    }

    @Test
    void execute_crossHospital_shouldThrow() {
        createCase();
        ActorContext other = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(other, "c1", "reviewer-1", DUE));
    }

    @Test
    void execute_notFound_shouldThrow() {
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(hospitalManager(), "no-such", "reviewer-1", DUE));
    }

    @Test
    void execute_alreadyAssigned_shouldThrow() {
        createCase();
        useCase.execute(hospitalManager(), "c1", "reviewer-1", DUE);

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(hospitalManager(), "c1", "reviewer-2", DUE));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.application.reviewcase.AssignReviewCaseUseCaseTest" --no-daemon -q`

Expected: FAIL — `AssignReviewCaseUseCase` does not exist yet.

- [ ] **Step 3: Implement AssignReviewCaseUseCase**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseAssignedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AssignReviewCaseUseCase {

    private final ReviewCaseRepository repository;
    private final DomainEventPublisher eventPublisher;

    public AssignReviewCaseUseCase(ReviewCaseRepository repository,
                                    DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(ActorContext actor, String caseId, String reviewerId, LocalDateTime dueAt) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);

        ReviewCase reviewCase = repository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND",
                        "ReviewCase not found: " + caseId));

        actor.requireHospitalScope(reviewCase.getHospitalId());

        reviewCase.assignReviewer(reviewerId, dueAt);
        repository.save(reviewCase);

        eventPublisher.publish(new ReviewCaseAssignedEvent(
                this, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId, reviewerId, dueAt));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.application.reviewcase.AssignReviewCaseUseCaseTest" --no-daemon -q`

Expected: PASS — all 6 tests green.

### Step 6b: ReassignReviewCaseUseCase

- [ ] **Step 5: Write ReassignReviewCaseUseCaseTest**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseReassignedEvent;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReassignReviewCaseUseCaseTest {

    private InMemoryReviewCaseRepository repository;
    private CapturingDomainEventPublisher eventPublisher;
    private ReassignReviewCaseUseCase useCase;

    private static final LocalDateTime DUE = LocalDateTime.of(2026, 2, 1, 23, 59);

    @BeforeEach
    void setUp() {
        repository = new InMemoryReviewCaseRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new ReassignReviewCaseUseCase(repository, eventPublisher);
    }

    private ReviewCase createAssignedCase() {
        ReviewCase rc = ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", DUE);
        repository.save(rc);
        return rc;
    }

    private ActorContext hospitalManager() {
        return new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
    }

    @Test
    void execute_happyPath_shouldReassign() {
        createAssignedCase();
        LocalDateTime newDue = DUE.plusDays(7);

        useCase.execute(hospitalManager(), "c1", "reviewer-2", newDue);

        ReviewCase saved = repository.store.get("c1");
        assertEquals("reviewer-2", saved.getReviewerAssignment().getReviewerId());
        assertEquals(newDue, saved.getDueAt());
    }

    @Test
    void execute_shouldPublishReassignedEvent() {
        createAssignedCase();

        useCase.execute(hospitalManager(), "c1", "reviewer-2", DUE);

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewCaseReassignedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext admin = new ActorContext("u1", "Admin", EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(admin, "c1", "reviewer-2", DUE));
    }

    @Test
    void execute_crossHospital_shouldThrow() {
        createAssignedCase();
        ActorContext other = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(other, "c1", "reviewer-2", DUE));
    }

    @Test
    void execute_notFound_shouldThrow() {
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(hospitalManager(), "no-such", "reviewer-2", DUE));
    }

    @Test
    void execute_notPendingReview_shouldThrow() {
        ReviewCase rc = ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        repository.save(rc); // SELECTED_PENDING_ASSIGNMENT

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(hospitalManager(), "c1", "reviewer-2", DUE));
    }
}
```

- [ ] **Step 6: Implement ReassignReviewCaseUseCase**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseReassignedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class ReassignReviewCaseUseCase {

    private final ReviewCaseRepository repository;
    private final DomainEventPublisher eventPublisher;

    public ReassignReviewCaseUseCase(ReviewCaseRepository repository,
                                      DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(ActorContext actor, String caseId, String newReviewerId, LocalDateTime dueAt) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);

        ReviewCase reviewCase = repository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND",
                        "ReviewCase not found: " + caseId));

        actor.requireHospitalScope(reviewCase.getHospitalId());

        String previousReviewerId = reviewCase.getReviewerAssignment() != null
                ? reviewCase.getReviewerAssignment().getReviewerId() : null;

        reviewCase.reassignReviewer(newReviewerId, dueAt);
        repository.save(reviewCase);

        eventPublisher.publish(new ReviewCaseReassignedEvent(
                this, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId,
                previousReviewerId, newReviewerId, dueAt));
    }
}
```

- [ ] **Step 7: Run reassign tests**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.application.reviewcase.ReassignReviewCaseUseCaseTest" --no-daemon -q`

Expected: PASS

### Step 6c: CancelReviewCaseUseCase

- [ ] **Step 8: Write CancelReviewCaseUseCaseTest**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseCancelledEvent;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CancelReviewCaseUseCaseTest {

    private InMemoryReviewCaseRepository repository;
    private CapturingDomainEventPublisher eventPublisher;
    private CancelReviewCaseUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryReviewCaseRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new CancelReviewCaseUseCase(repository, eventPublisher);
    }

    private ActorContext hospitalManager() {
        return new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
    }

    @Test
    void execute_fromSelectedPending_shouldCancel() {
        ReviewCase rc = ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        repository.save(rc);

        useCase.execute(hospitalManager(), "c1", "project cancelled");

        assertEquals(CaseStatus.CANCELLED, repository.store.get("c1").getStatus());
        assertEquals("project cancelled", repository.store.get("c1").getCancelReason());
    }

    @Test
    void execute_shouldPublishCancelledEvent() {
        ReviewCase rc = ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        repository.save(rc);

        useCase.execute(hospitalManager(), "c1", "reason");

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewCaseCancelledEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext admin = new ActorContext("u1", "Admin", EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(admin, "c1", "reason"));
    }

    @Test
    void execute_crossHospital_shouldThrow() {
        ReviewCase rc = ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        repository.save(rc);
        ActorContext other = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(other, "c1", "reason"));
    }

    @Test
    void execute_notFound_shouldThrow() {
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(hospitalManager(), "no-such", "reason"));
    }

    @Test
    void execute_fromClosed_shouldThrow() {
        ReviewCase rc = ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.forceStatus(CaseStatus.CLOSED);
        repository.save(rc);

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(hospitalManager(), "c1", "reason"));
    }
}
```

- [ ] **Step 9: Implement CancelReviewCaseUseCase**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseCancelledEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CancelReviewCaseUseCase {

    private final ReviewCaseRepository repository;
    private final DomainEventPublisher eventPublisher;

    public CancelReviewCaseUseCase(ReviewCaseRepository repository,
                                    DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(ActorContext actor, String caseId, String reason) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);

        ReviewCase reviewCase = repository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND",
                        "ReviewCase not found: " + caseId));

        actor.requireHospitalScope(reviewCase.getHospitalId());

        reviewCase.cancel(reason);
        repository.save(reviewCase);

        eventPublisher.publish(new ReviewCaseCancelledEvent(
                this, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId, reason));
    }
}
```

- [ ] **Step 10: Run cancel tests**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.application.reviewcase.CancelReviewCaseUseCaseTest" --no-daemon -q`

Expected: PASS

### Step 6d: ReturnInitialReviewUseCase

- [ ] **Step 11: Write ReturnInitialReviewUseCaseTest**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseReturnedEvent;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReturnInitialReviewUseCaseTest {

    private InMemoryReviewCaseRepository repository;
    private CapturingDomainEventPublisher eventPublisher;
    private ReturnInitialReviewUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryReviewCaseRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new ReturnInitialReviewUseCase(repository, eventPublisher);
    }

    private ActorContext hospitalManager() {
        return new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
    }

    private ReviewCase createInProgressCase() {
        ReviewCase rc = ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", LocalDateTime.of(2026, 2, 1, 23, 59));
        rc.forceStatus(CaseStatus.INITIAL_REVIEW_IN_PROGRESS);
        repository.save(rc);
        return rc;
    }

    @Test
    void execute_happyPath_shouldReturn() {
        createInProgressCase();

        useCase.execute(hospitalManager(), "c1", "needs correction");

        assertEquals(CaseStatus.RETURNED, repository.store.get("c1").getStatus());
        assertEquals("needs correction", repository.store.get("c1").getReturnReason());
    }

    @Test
    void execute_shouldPublishReturnedEvent() {
        createInProgressCase();

        useCase.execute(hospitalManager(), "c1", "reason");

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewCaseReturnedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext admin = new ActorContext("u1", "Admin", EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(admin, "c1", "reason"));
    }

    @Test
    void execute_crossHospital_shouldThrow() {
        createInProgressCase();
        ActorContext other = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(other, "c1", "reason"));
    }

    @Test
    void execute_notFound_shouldThrow() {
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(hospitalManager(), "no-such", "reason"));
    }

    @Test
    void execute_notInProgress_shouldThrow() {
        ReviewCase rc = ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        repository.save(rc); // SELECTED_PENDING_ASSIGNMENT

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(hospitalManager(), "c1", "reason"));
    }
}
```

- [ ] **Step 12: Implement ReturnInitialReviewUseCase**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseReturnedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReturnInitialReviewUseCase {

    private final ReviewCaseRepository repository;
    private final DomainEventPublisher eventPublisher;

    public ReturnInitialReviewUseCase(ReviewCaseRepository repository,
                                       DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(ActorContext actor, String caseId, String reason) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);

        ReviewCase reviewCase = repository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND",
                        "ReviewCase not found: " + caseId));

        actor.requireHospitalScope(reviewCase.getHospitalId());

        reviewCase.returnToInitialReview(reason);
        repository.save(reviewCase);

        eventPublisher.publish(new ReviewCaseReturnedEvent(
                this, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId, reason));
    }
}
```

- [ ] **Step 13: Run return tests**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.application.reviewcase.ReturnInitialReviewUseCaseTest" --no-daemon -q`

Expected: PASS

### Step 6e: ReopenInitialReviewUseCase

- [ ] **Step 14: Write ReopenInitialReviewUseCaseTest**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseAssignedEvent;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReopenInitialReviewUseCaseTest {

    private InMemoryReviewCaseRepository repository;
    private CapturingDomainEventPublisher eventPublisher;
    private ReopenInitialReviewUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryReviewCaseRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new ReopenInitialReviewUseCase(repository, eventPublisher);
    }

    private ActorContext hospitalManager() {
        return new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
    }

    private ReviewCase createReturnedCase() {
        ReviewCase rc = ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", LocalDateTime.of(2026, 2, 1, 23, 59));
        rc.forceStatus(CaseStatus.INITIAL_REVIEW_IN_PROGRESS);
        rc.returnToInitialReview("needs correction");
        repository.save(rc);
        return rc;
    }

    @Test
    void execute_happyPath_shouldReopen() {
        createReturnedCase();

        useCase.execute(hospitalManager(), "c1");

        ReviewCase saved = repository.store.get("c1");
        assertEquals(CaseStatus.PENDING_INITIAL_REVIEW, saved.getStatus());
        assertNull(saved.getReturnReason());
    }

    @Test
    void execute_shouldPublishAssignedEvent() {
        createReturnedCase();

        useCase.execute(hospitalManager(), "c1");

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewCaseAssignedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext admin = new ActorContext("u1", "Admin", EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(admin, "c1"));
    }

    @Test
    void execute_crossHospital_shouldThrow() {
        createReturnedCase();
        ActorContext other = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(other, "c1"));
    }

    @Test
    void execute_notFound_shouldThrow() {
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(hospitalManager(), "no-such"));
    }

    @Test
    void execute_notReturned_shouldThrow() {
        ReviewCase rc = ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", LocalDateTime.of(2026, 2, 1, 23, 59));
        repository.save(rc); // PENDING_INITIAL_REVIEW

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(hospitalManager(), "c1"));
    }
}
```

- [ ] **Step 15: Implement ReopenInitialReviewUseCase**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseAssignedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReopenInitialReviewUseCase {

    private final ReviewCaseRepository repository;
    private final DomainEventPublisher eventPublisher;

    public ReopenInitialReviewUseCase(ReviewCaseRepository repository,
                                       DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(ActorContext actor, String caseId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);

        ReviewCase reviewCase = repository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND",
                        "ReviewCase not found: " + caseId));

        actor.requireHospitalScope(reviewCase.getHospitalId());

        reviewCase.reopenInitialReview();
        repository.save(reviewCase);

        eventPublisher.publish(new ReviewCaseAssignedEvent(
                this, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId,
                reviewCase.getReviewerAssignment().getReviewerId(),
                reviewCase.getDueAt()));
    }
}
```

- [ ] **Step 16: Run all application tests**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.application.reviewcase.*" --no-daemon -q`

Expected: PASS — all 30 tests (6 per use case × 5 use cases) green.

- [ ] **Step 17: Run full test suite**

Run: `cd services/quality-inspection-service && ./gradlew test --no-daemon -q`

Expected: PASS — no regression.

- [ ] **Step 18: Commit**

```bash
cd services/quality-inspection-service
git add src/main/java/com/tzuchi/his/quality/inspection/application/reviewcase/ \
        src/test/java/com/tzuchi/his/quality/inspection/application/reviewcase/
git commit -m "feat(b3a): add 5 ReviewCase application use cases with tests

AssignReviewCaseUseCase, ReassignReviewCaseUseCase, CancelReviewCaseUseCase,
ReturnInitialReviewUseCase, ReopenInitialReviewUseCase.
All with role/scope guards, event publishing, and comprehensive tests.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 7: Infrastructure — JPA Entity Expansion + Repository findById + Round-Trip Tests

**Files:**
- Modify: `infrastructure/persistence/reviewcase/ReviewCaseEntity.java`
- Modify: `infrastructure/persistence/reviewcase/ReviewCaseRepositoryImpl.java`
- Create: `src/test/java/.../infrastructure/persistence/reviewcase/ReviewCaseEntityRoundTripTest.java`

- [ ] **Step 1: Expand ReviewCaseEntity with new columns**

Replace the entire `ReviewCaseEntity.java` with:

```java
package com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase;

import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_case")
public class ReviewCaseEntity {

    @Id
    @Column(name = "case_id")
    private String caseId;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "hospital_id")
    private String hospitalId;

    @Column(name = "source_case_key")
    private String sourceCaseKey;

    @Column(name = "medical_record_type")
    private String medicalRecordType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status;

    // --- B3a: assignment ---

    @Column(name = "reviewer_id")
    private String reviewerId;

    @Column(name = "reviewer_assigned_at")
    private LocalDateTime reviewerAssignedAt;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "return_reason")
    private String returnReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // --- B3b/B3c reserved ---

    @Column(name = "rereview_required")
    private boolean rereviewRequired;

    @Column(name = "final_score", precision = 10, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "final_conclusion")
    private String finalConclusion;

    @Column(name = "latest_initial_submission_id")
    private String latestInitialSubmissionId;

    @Column(name = "rule_snapshot_ref")
    private String ruleSnapshotRef;

    @Column(name = "score_sheet_version_ref")
    private String scoreSheetVersionRef;

    protected ReviewCaseEntity() {}

    // --- Existing getters/setters ---

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getSourceCaseKey() { return sourceCaseKey; }
    public void setSourceCaseKey(String sourceCaseKey) { this.sourceCaseKey = sourceCaseKey; }

    public String getMedicalRecordType() { return medicalRecordType; }
    public void setMedicalRecordType(String medicalRecordType) { this.medicalRecordType = medicalRecordType; }

    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }

    // --- B3a getters/setters ---

    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }

    public LocalDateTime getReviewerAssignedAt() { return reviewerAssignedAt; }
    public void setReviewerAssignedAt(LocalDateTime reviewerAssignedAt) { this.reviewerAssignedAt = reviewerAssignedAt; }

    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public boolean isRereviewRequired() { return rereviewRequired; }
    public void setRereviewRequired(boolean rereviewRequired) { this.rereviewRequired = rereviewRequired; }

    public BigDecimal getFinalScore() { return finalScore; }
    public void setFinalScore(BigDecimal finalScore) { this.finalScore = finalScore; }

    public String getFinalConclusion() { return finalConclusion; }
    public void setFinalConclusion(String finalConclusion) { this.finalConclusion = finalConclusion; }

    public String getLatestInitialSubmissionId() { return latestInitialSubmissionId; }
    public void setLatestInitialSubmissionId(String id) { this.latestInitialSubmissionId = id; }

    public String getRuleSnapshotRef() { return ruleSnapshotRef; }
    public void setRuleSnapshotRef(String ruleSnapshotRef) { this.ruleSnapshotRef = ruleSnapshotRef; }

    public String getScoreSheetVersionRef() { return scoreSheetVersionRef; }
    public void setScoreSheetVersionRef(String ref) { this.scoreSheetVersionRef = ref; }
}
```

- [ ] **Step 2: Expand ReviewCaseRepositoryImpl with findById + toDomain + expanded toEntity**

Replace the entire `ReviewCaseRepositoryImpl.java` with:

```java
package com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewerAssignment;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ReviewCaseRepositoryImpl implements ReviewCaseRepository {

    private final ReviewCaseJpaRepository jpa;

    public ReviewCaseRepositoryImpl(ReviewCaseJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(ReviewCase reviewCase) {
        jpa.save(toEntity(reviewCase));
    }

    @Override
    public void saveAll(List<ReviewCase> cases) {
        jpa.saveAll(cases.stream().map(this::toEntity).collect(Collectors.toList()));
    }

    @Override
    public Optional<ReviewCase> findById(String caseId) {
        return jpa.findById(caseId).map(this::toDomain);
    }

    private ReviewCase toDomain(ReviewCaseEntity e) {
        ReviewerAssignment assignment = null;
        if (e.getReviewerId() != null && e.getReviewerAssignedAt() != null) {
            assignment = new ReviewerAssignment(e.getReviewerId(), e.getReviewerAssignedAt());
        }

        MedicalRecordType mrt = null;
        if (e.getMedicalRecordType() != null) {
            mrt = MedicalRecordType.valueOf(e.getMedicalRecordType());
        }

        return ReviewCase.reconstitute(
                e.getCaseId(), e.getBatchId(), e.getProjectId(), e.getHospitalId(),
                e.getSourceCaseKey(), mrt,
                e.getStatus(), assignment, e.getDueAt(),
                e.getCancelReason(), e.getReturnReason(), e.getCancelledAt(), e.getClosedAt(),
                e.isRereviewRequired(), e.getFinalScore(), e.getFinalConclusion(),
                e.getLatestInitialSubmissionId(), e.getRuleSnapshotRef(), e.getScoreSheetVersionRef());
    }

    private ReviewCaseEntity toEntity(ReviewCase rc) {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(rc.getCaseId());
        e.setBatchId(rc.getBatchId());
        e.setProjectId(rc.getProjectId());
        e.setHospitalId(rc.getHospitalId());
        e.setSourceCaseKey(rc.getSourceCaseKey());
        e.setMedicalRecordType(rc.getMedicalRecordType() != null ? rc.getMedicalRecordType().name() : null);
        e.setStatus(rc.getStatus());

        if (rc.getReviewerAssignment() != null) {
            e.setReviewerId(rc.getReviewerAssignment().getReviewerId());
            e.setReviewerAssignedAt(rc.getReviewerAssignment().getAssignedAt());
        }
        e.setDueAt(rc.getDueAt());
        e.setCancelReason(rc.getCancelReason());
        e.setReturnReason(rc.getReturnReason());
        e.setCancelledAt(rc.getCancelledAt());
        e.setClosedAt(rc.getClosedAt());
        e.setRereviewRequired(rc.isRereviewRequired());
        e.setFinalScore(rc.getFinalScore());
        e.setFinalConclusion(rc.getFinalConclusion());
        e.setLatestInitialSubmissionId(rc.getLatestInitialSubmissionId());
        e.setRuleSnapshotRef(rc.getRuleSnapshotRef());
        e.setScoreSheetVersionRef(rc.getScoreSheetVersionRef());
        return e;
    }
}
```

- [ ] **Step 3: Write JPA round-trip test**

Create: `src/test/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/reviewcase/ReviewCaseEntityRoundTripTest.java`

```java
package com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase;

import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ReviewCaseRepositoryImpl.class)
class ReviewCaseEntityRoundTripTest {

    @Autowired
    private ReviewCaseRepository repo;

    @Test
    void roundTrip_allFields_persistAndLoad() {
        // Given — a fully populated ReviewCase
        ReviewCase original = ReviewCase.reconstitute(
                "case-rt-1", "batch-1", "proj-1", "hosp-1",
                "SK-001", com.tzuchi.his.quality.inspection.domain.reviewcase.MedicalRecordType.INPATIENT,
                CaseStatus.ASSIGNED,
                new com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewerAssignment("reviewer-1", LocalDateTime.of(2025, 6, 1, 10, 0)),
                LocalDateTime.of(2025, 6, 15, 23, 59),
                null, null, null, null,
                false, null, null,
                null, null, null
        );

        // When — persist and reload
        repo.save(original);
        Optional<ReviewCase> loaded = repo.findById("case-rt-1");

        // Then
        assertThat(loaded).isPresent();
        ReviewCase rc = loaded.get();
        assertThat(rc.getCaseId()).isEqualTo("case-rt-1");
        assertThat(rc.getBatchId()).isEqualTo("batch-1");
        assertThat(rc.getProjectId()).isEqualTo("proj-1");
        assertThat(rc.getHospitalId()).isEqualTo("hosp-1");
        assertThat(rc.getSourceCaseKey()).isEqualTo("SK-001");
        assertThat(rc.getMedicalRecordType()).isEqualTo(
                com.tzuchi.his.quality.inspection.domain.reviewcase.MedicalRecordType.INPATIENT);
        assertThat(rc.getStatus()).isEqualTo(CaseStatus.ASSIGNED);
        assertThat(rc.getReviewerAssignment()).isNotNull();
        assertThat(rc.getReviewerAssignment().getReviewerId()).isEqualTo("reviewer-1");
        assertThat(rc.getDueAt()).isEqualTo(LocalDateTime.of(2025, 6, 15, 23, 59));
    }

    @Test
    void roundTrip_minimalFields_backwardCompat() {
        // Given — a case with only the original B2 fields (new columns all null)
        ReviewCase minimal = ReviewCase.reconstitute(
                "case-rt-2", "batch-2", "proj-2", "hosp-2",
                "SK-002", null,
                CaseStatus.PENDING_ASSIGNMENT,
                null, null, null, null, null, null,
                false, null, null,
                null, null, null
        );

        // When
        repo.save(minimal);
        Optional<ReviewCase> loaded = repo.findById("case-rt-2");

        // Then — all new fields are null/default, no error
        assertThat(loaded).isPresent();
        ReviewCase rc = loaded.get();
        assertThat(rc.getStatus()).isEqualTo(CaseStatus.PENDING_ASSIGNMENT);
        assertThat(rc.getReviewerAssignment()).isNull();
        assertThat(rc.getDueAt()).isNull();
        assertThat(rc.getCancelReason()).isNull();
        assertThat(rc.getReturnReason()).isNull();
        assertThat(rc.getCancelledAt()).isNull();
        assertThat(rc.getClosedAt()).isNull();
        assertThat(rc.isRereviewRequired()).isFalse();
        assertThat(rc.getFinalScore()).isNull();
        assertThat(rc.getFinalConclusion()).isNull();
    }

    @Test
    void roundTrip_cancelledCase_withReason() {
        ReviewCase cancelled = ReviewCase.reconstitute(
                "case-rt-3", "batch-3", "proj-3", "hosp-3",
                "SK-003", null,
                CaseStatus.CANCELLED,
                null, null,
                "Duplicate case", null,
                LocalDateTime.of(2025, 7, 1, 12, 0), null,
                false, null, null,
                null, null, null
        );

        repo.save(cancelled);
        Optional<ReviewCase> loaded = repo.findById("case-rt-3");

        assertThat(loaded).isPresent();
        ReviewCase rc = loaded.get();
        assertThat(rc.getStatus()).isEqualTo(CaseStatus.CANCELLED);
        assertThat(rc.getCancelReason()).isEqualTo("Duplicate case");
        assertThat(rc.getCancelledAt()).isEqualTo(LocalDateTime.of(2025, 7, 1, 12, 0));
    }
}
```

- [ ] **Step 4: Run the JPA round-trip tests**

Run: `cd services/quality-inspection-service && ./gradlew test --tests '*ReviewCaseEntityRoundTripTest' -q`

Expected: PASS — all 3 tests pass.

- [ ] **Step 5: Run full test suite**

Run: `cd services/quality-inspection-service && ./gradlew test -q`

Expected: PASS — no regression. All existing sampling/project tests still pass.

- [ ] **Step 6: Commit**

```bash
cd services/quality-inspection-service
git add src/main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/reviewcase/
git add src/test/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/reviewcase/
git commit -m "feat(b3a): expand ReviewCaseEntity JPA + implement findById with toDomain/toEntity

Add 14 new columns for assignment, cancel, return, reserved fields.
Implement findById with ReviewerAssignment VO flat-column mapping.
Expand toEntity for all new fields.
Add JPA round-trip tests: full fields, backward compat, cancelled case.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 8: ReviewCaseController + Request DTOs + API Integration Tests

**Files:**
- Create: `interfaces/rest/ReviewCaseController.java`
- Create: `interfaces/rest/dto/AssignReviewerRequest.java`
- Create: `interfaces/rest/dto/ReassignReviewerRequest.java`
- Create: `interfaces/rest/dto/CancelReviewCaseRequest.java`
- Create: `interfaces/rest/dto/ReturnReviewCaseRequest.java`
- Create (test): `interfaces/rest/ReviewCaseApiTest.java`

- [ ] **Step 1: Create request DTOs**

`AssignReviewerRequest.java`:
```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class AssignReviewerRequest {

    @NotBlank(message = "reviewerId must not be blank")
    private String reviewerId;

    @NotNull(message = "dueAt must not be null")
    private LocalDateTime dueAt;

    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }

    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
}
```

`ReassignReviewerRequest.java`:
```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class ReassignReviewerRequest {

    @NotBlank(message = "reviewerId must not be blank")
    private String reviewerId;

    @NotNull(message = "dueAt must not be null")
    private LocalDateTime dueAt;

    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }

    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
}
```

`CancelReviewCaseRequest.java`:
```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.constraints.NotBlank;

public class CancelReviewCaseRequest {

    @NotBlank(message = "reason must not be blank")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
```

`ReturnReviewCaseRequest.java`:
```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.constraints.NotBlank;

public class ReturnReviewCaseRequest {

    @NotBlank(message = "reason must not be blank")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
```

- [ ] **Step 2: Create ReviewCaseController**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.*;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/review-cases")
public class ReviewCaseController {

    private final AssignReviewCaseUseCase assignUseCase;
    private final ReassignReviewCaseUseCase reassignUseCase;
    private final CancelReviewCaseUseCase cancelUseCase;
    private final ReturnInitialReviewUseCase returnUseCase;
    private final ReopenInitialReviewUseCase reopenUseCase;

    public ReviewCaseController(AssignReviewCaseUseCase assignUseCase,
                                 ReassignReviewCaseUseCase reassignUseCase,
                                 CancelReviewCaseUseCase cancelUseCase,
                                 ReturnInitialReviewUseCase returnUseCase,
                                 ReopenInitialReviewUseCase reopenUseCase) {
        this.assignUseCase = assignUseCase;
        this.reassignUseCase = reassignUseCase;
        this.cancelUseCase = cancelUseCase;
        this.returnUseCase = returnUseCase;
        this.reopenUseCase = reopenUseCase;
    }

    @PostMapping("/{caseId}/assign-reviewer")
    public ResponseEntity<ApiResponse<Void>> assignReviewer(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @Valid @RequestBody AssignReviewerRequest request) {
        assignUseCase.execute(actor, caseId, request.getReviewerId(), request.getDueAt());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/reassign-reviewer")
    public ResponseEntity<ApiResponse<Void>> reassignReviewer(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @Valid @RequestBody ReassignReviewerRequest request) {
        reassignUseCase.execute(actor, caseId, request.getReviewerId(), request.getDueAt());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @Valid @RequestBody CancelReviewCaseRequest request) {
        cancelUseCase.execute(actor, caseId, request.getReason());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/return")
    public ResponseEntity<ApiResponse<Void>> returnCase(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @Valid @RequestBody ReturnReviewCaseRequest request) {
        returnUseCase.execute(actor, caseId, request.getReason());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/reopen")
    public ResponseEntity<ApiResponse<Void>> reopen(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        reopenUseCase.execute(actor, caseId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
```

- [ ] **Step 3: Write ReviewCaseApiTest**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseJpaRepository;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReviewCaseApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ReviewCaseJpaRepository jpaRepository;

    private static final String BASE = "/review-cases";
    private final String hmToken = TestTokenFactory.hospitalManagerToken();
    private final String saToken = TestTokenFactory.systemAdminToken();

    private void seedCase(String caseId, CaseStatus status) {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(caseId);
        e.setBatchId("b1");
        e.setProjectId("p1");
        e.setHospitalId("H001");
        e.setSourceCaseKey("SK-001");
        e.setMedicalRecordType("OUTPATIENT");
        e.setStatus(status);
        jpaRepository.save(e);
    }

    private void seedAssignedCase(String caseId) {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(caseId);
        e.setBatchId("b1");
        e.setProjectId("p1");
        e.setHospitalId("H001");
        e.setSourceCaseKey("SK-001");
        e.setMedicalRecordType("OUTPATIENT");
        e.setStatus(CaseStatus.PENDING_INITIAL_REVIEW);
        e.setReviewerId("reviewer-1");
        e.setReviewerAssignedAt(LocalDateTime.now());
        e.setDueAt(LocalDateTime.of(2026, 2, 1, 23, 59));
        jpaRepository.save(e);
    }

    // --- ASSIGN ---

    @Test
    void assignReviewer_shouldReturn200() throws Exception {
        seedCase("c1", CaseStatus.SELECTED_PENDING_ASSIGNMENT);

        AssignReviewerRequest req = new AssignReviewerRequest();
        req.setReviewerId("reviewer-1");
        req.setDueAt(LocalDateTime.of(2026, 2, 1, 23, 59));

        mockMvc.perform(post(BASE + "/c1/assign-reviewer")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void assignReviewer_missingReviewerId_shouldReturn400() throws Exception {
        seedCase("c1", CaseStatus.SELECTED_PENDING_ASSIGNMENT);

        String body = "{\"dueAt\":\"2026-02-01T23:59:00\"}";

        mockMvc.perform(post(BASE + "/c1/assign-reviewer")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignReviewer_systemAdmin_shouldReturn403() throws Exception {
        seedCase("c1", CaseStatus.SELECTED_PENDING_ASSIGNMENT);

        AssignReviewerRequest req = new AssignReviewerRequest();
        req.setReviewerId("reviewer-1");
        req.setDueAt(LocalDateTime.of(2026, 2, 1, 23, 59));

        mockMvc.perform(post(BASE + "/c1/assign-reviewer")
                        .header("api-key", saToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignReviewer_notFound_shouldReturn404() throws Exception {
        AssignReviewerRequest req = new AssignReviewerRequest();
        req.setReviewerId("reviewer-1");
        req.setDueAt(LocalDateTime.of(2026, 2, 1, 23, 59));

        mockMvc.perform(post(BASE + "/no-such/assign-reviewer")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignReviewer_alreadyAssigned_shouldReturn409() throws Exception {
        seedAssignedCase("c1"); // already PENDING_INITIAL_REVIEW

        AssignReviewerRequest req = new AssignReviewerRequest();
        req.setReviewerId("reviewer-2");
        req.setDueAt(LocalDateTime.of(2026, 2, 1, 23, 59));

        mockMvc.perform(post(BASE + "/c1/assign-reviewer")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // --- REASSIGN ---

    @Test
    void reassignReviewer_shouldReturn200() throws Exception {
        seedAssignedCase("c1");

        ReassignReviewerRequest req = new ReassignReviewerRequest();
        req.setReviewerId("reviewer-2");
        req.setDueAt(LocalDateTime.of(2026, 3, 1, 23, 59));

        mockMvc.perform(post(BASE + "/c1/reassign-reviewer")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void reassignReviewer_notAssigned_shouldReturn409() throws Exception {
        seedCase("c1", CaseStatus.SELECTED_PENDING_ASSIGNMENT);

        ReassignReviewerRequest req = new ReassignReviewerRequest();
        req.setReviewerId("reviewer-2");
        req.setDueAt(LocalDateTime.of(2026, 3, 1, 23, 59));

        mockMvc.perform(post(BASE + "/c1/reassign-reviewer")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // --- CANCEL ---

    @Test
    void cancel_shouldReturn200() throws Exception {
        seedCase("c1", CaseStatus.SELECTED_PENDING_ASSIGNMENT);

        CancelReviewCaseRequest req = new CancelReviewCaseRequest();
        req.setReason("project cancelled");

        mockMvc.perform(post(BASE + "/c1/cancel")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void cancel_alreadyCancelled_shouldReturn409() throws Exception {
        seedCase("c1", CaseStatus.CANCELLED);

        CancelReviewCaseRequest req = new CancelReviewCaseRequest();
        req.setReason("again");

        mockMvc.perform(post(BASE + "/c1/cancel")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void cancel_missingReason_shouldReturn400() throws Exception {
        seedCase("c1", CaseStatus.SELECTED_PENDING_ASSIGNMENT);

        mockMvc.perform(post(BASE + "/c1/cancel")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- RETURN ---

    @Test
    void returnCase_shouldReturn200() throws Exception {
        seedCase("c1", CaseStatus.INITIAL_REVIEW_IN_PROGRESS);

        ReturnReviewCaseRequest req = new ReturnReviewCaseRequest();
        req.setReason("needs correction");

        mockMvc.perform(post(BASE + "/c1/return")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void returnCase_notInProgress_shouldReturn409() throws Exception {
        seedCase("c1", CaseStatus.SELECTED_PENDING_ASSIGNMENT);

        ReturnReviewCaseRequest req = new ReturnReviewCaseRequest();
        req.setReason("nope");

        mockMvc.perform(post(BASE + "/c1/return")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // --- REOPEN ---

    @Test
    void reopen_shouldReturn200() throws Exception {
        seedCase("c1", CaseStatus.RETURNED);
        // Set reviewer data for reopen event
        ReviewCaseEntity e = jpaRepository.findById("c1").orElseThrow();
        e.setReviewerId("reviewer-1");
        e.setReviewerAssignedAt(LocalDateTime.now());
        e.setDueAt(LocalDateTime.of(2026, 2, 1, 23, 59));
        jpaRepository.save(e);

        mockMvc.perform(post(BASE + "/c1/reopen")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());
    }

    @Test
    void reopen_notReturned_shouldReturn409() throws Exception {
        seedAssignedCase("c1"); // PENDING_INITIAL_REVIEW

        mockMvc.perform(post(BASE + "/c1/reopen")
                        .header("api-key", hmToken))
                .andExpect(status().isConflict());
    }

    @Test
    void reopen_noApiKey_shouldReturn401() throws Exception {
        mockMvc.perform(post(BASE + "/c1/reopen"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 4: Run API tests**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.interfaces.rest.ReviewCaseApiTest" --no-daemon -q`

Expected: PASS — all API tests green.

- [ ] **Step 5: Run full test suite**

Run: `cd services/quality-inspection-service && ./gradlew test --no-daemon -q`

Expected: PASS — all tests (existing + new) pass.

- [ ] **Step 6: Commit**

```bash
cd services/quality-inspection-service
git add src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/ReviewCaseController.java \
        src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/dto/AssignReviewerRequest.java \
        src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/dto/ReassignReviewerRequest.java \
        src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/dto/CancelReviewCaseRequest.java \
        src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/dto/ReturnReviewCaseRequest.java \
        src/test/java/com/tzuchi/his/quality/inspection/interfaces/rest/ReviewCaseApiTest.java
git commit -m "feat(b3a): add ReviewCaseController with 5 endpoints + API integration tests

POST /{caseId}/assign-reviewer
POST /{caseId}/reassign-reviewer
POST /{caseId}/cancel
POST /{caseId}/return
POST /{caseId}/reopen

All endpoints validated with success, error, auth, and state conflict tests.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Summary

| Task | Content | Files Changed | Tests |
|------|---------|---------------|-------|
| 1 | ReviewerAssignment VO | 2 created | 5 |
| 2 | ReviewCase aggregate expansion | 1 modified, 1 created | 15+ |
| 3 | ReviewSubmission framework | 4 created | 0 (data-only) |
| 4 | 4 Domain events | 4 created | 0 (compile check) |
| 5 | Repository findById + test fixture | 1 modified, 1 created, 1 stub | 0 (compile check) |
| 6 | 5 Use cases + tests | 5 created, 5 tests created | 30 |
| 7 | JPA entity + repo impl + round-trip tests | 2 modified, 1 test created | 3 |
| 8 | Controller + DTOs + API tests | 5 created, 1 test created | 16 |
| **Total** | | **~29 files** | **~69 tests** |

**Dependency chain:** Task 1 ∥ Task 3 → Task 2 → Task 4 → Task 5 → Task 6 → Task 7 → Task 8

(Tasks 1 and 3 can be parallel. Task 2 depends on Task 1. Tasks 4-8 are sequential.)
