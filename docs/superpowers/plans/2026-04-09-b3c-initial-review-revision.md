# B3c — Initial Review Revision Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the initial review revision workflow — revise a submitted initial review with new answers, mark the old submission as SUPERSEDED, update the case's finalScore, and provide a revision history query.

**Architecture:** TDD bottom-up approach: domain methods on ReviewSubmission + ReviewCase → domain tests → domain event → use case (revise + history query) → use case tests → repository extension → JPA extension → REST endpoints + DTOs → integration tests. Each layer tested independently before wiring. The revise endpoint reuses ScoreCalculationService and produces a new SUBMITTED submission in one step (no DRAFT phase for revisions).

**Tech Stack:** Java 11, Spring Boot 2.7.18, JPA/Hibernate with H2 for tests, Gradle 7.6.4, JUnit 5

**Spec:** `docs/superpowers/specs/2026-04-09-b3c-initial-review-revision-design.md`

**Build/test command:** `cd services/quality-inspection-service && .\gradlew test`

**Base path for all source files:** `services/quality-inspection-service/src`

---

## File Structure

### Files to CREATE

| # | Path | Responsibility |
|---|------|---------------|
| 1 | `main/java/.../domain/reviewcase/submission/event/ReviewSubmissionRevisedEvent.java` | Domain event — old/new submissionId, caseId, stage, revisionNo, triggeredBy |
| 2 | `main/java/.../application/reviewcase/ReviseInitialReviewUseCase.java` | Command use case — orchestrate revision flow |
| 3 | `main/java/.../application/reviewcase/RevisionResult.java` | Result DTO — newSubmissionId, revisionNo, totalScore, status |
| 4 | `main/java/.../application/reviewcase/GetRevisionHistoryUseCase.java` | Query use case — return all submissions for a case |
| 5 | `main/java/.../interfaces/rest/dto/ReviseInitialReviewRequest.java` | Request DTO — answers + comment |
| 6 | `main/java/.../interfaces/rest/dto/RevisionResultResponse.java` | Response DTO — wraps RevisionResult for REST |
| 7 | `main/java/.../interfaces/rest/dto/RevisionHistoryResponse.java` | Response DTO — caseId + list of SubmissionSummary |
| 8 | `main/java/.../interfaces/rest/dto/SubmissionSummaryDto.java` | DTO — single submission summary |
| 9 | `test/java/.../application/reviewcase/ReviseInitialReviewUseCaseTest.java` | Use case tests — 10 scenarios |
| 10 | `test/java/.../application/reviewcase/GetRevisionHistoryUseCaseTest.java` | Query use case tests — 2 scenarios |
| 11 | `test/java/.../interfaces/rest/RevisionApiTest.java` | Integration tests — @SpringBootTest + MockMvc |

### Files to MODIFY

| # | Path | Change |
|---|------|--------|
| 1 | `main/java/.../domain/reviewcase/submission/ReviewSubmission.java` | Add `createRevision()` static factory + `markSuperseded()` method |
| 2 | `main/java/.../domain/reviewcase/ReviewCase.java` | Add `requireNotTerminal()` guard + `updateFinalScore()` method |
| 3 | `main/java/.../domain/reviewcase/submission/ReviewSubmissionRepository.java` | Add `findAllByCaseId(String caseId)` |
| 4 | `test/java/.../testfixture/InMemoryReviewSubmissionRepository.java` | Implement `findAllByCaseId()` |
| 5 | `main/java/.../infrastructure/persistence/submission/ReviewSubmissionJpaRepository.java` | Add `findByCaseId()` JPA query |
| 6 | `main/java/.../infrastructure/persistence/submission/ReviewSubmissionRepositoryImpl.java` | Implement `findAllByCaseId()` |
| 7 | `main/java/.../interfaces/rest/ReviewCaseController.java` | Add 2 new endpoints + inject 2 new use cases |
| 8 | `test/java/.../domain/reviewcase/submission/ReviewSubmissionTest.java` | Add revision domain tests |
| 9 | `test/java/.../domain/reviewcase/ReviewCaseTest.java` | Add requireNotTerminal + updateFinalScore tests |

> **Package root abbreviation:** `...` = `com/tzuchi/his/quality/inspection`

---

## Task 1: ReviewSubmission Domain Methods + Tests

**Files:**
- Modify: `main/java/com/tzuchi/his/quality/inspection/domain/reviewcase/submission/ReviewSubmission.java`
- Modify: `test/java/com/tzuchi/his/quality/inspection/domain/reviewcase/submission/ReviewSubmissionTest.java`

- [ ] **Step 1: Write the failing revision domain tests**

Append the following test methods to the existing `ReviewSubmissionTest.java`:

```java
    // --- createRevision ---

    @Test
    void createRevision_fromSubmittedSubmission_succeeds() {
        ReviewSubmission old = createSubmittedSubmission();

        List<AnswerItem> newAnswers = Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.00")),
                AnswerItem.ofScored("Q002", "90", new BigDecimal("90.00"))
        );

        ReviewSubmission rev = ReviewSubmission.createRevision(
                "sub-2", old, newAnswers, new BigDecimal("100.00"), "revised comment");

        assertEquals("sub-2", rev.getSubmissionId());
        assertEquals("c1", rev.getCaseId());
        assertEquals(ReviewStage.INITIAL, rev.getStage());
        assertEquals("reviewer-1", rev.getReviewerId());
        assertEquals(SubmissionStatus.SUBMITTED, rev.getStatus());
        assertEquals(2, rev.getRevisionNo());
        assertEquals(new BigDecimal("100.00"), rev.getTotalScore());
        assertEquals("revised comment", rev.getComment());
        assertNotNull(rev.getSubmittedAt());
        assertEquals("ss-v1", rev.getBasedOnScoreSheetVersionId());
    }

    @Test
    void createRevision_fromDraftSubmission_throwsConflict() {
        ReviewSubmission draft = ReviewSubmission.createInitialDraft("sub-1", "c1", "reviewer-1", "ss-v1");

        assertThrows(DomainStateConflictException.class,
                () -> ReviewSubmission.createRevision(
                        "sub-2", draft, Collections.emptyList(), BigDecimal.ZERO, null));
    }

    @Test
    void createRevision_fromSupersededSubmission_throwsConflict() {
        ReviewSubmission old = createSubmittedSubmission();
        old.markSuperseded("sub-other");

        assertThrows(DomainStateConflictException.class,
                () -> ReviewSubmission.createRevision(
                        "sub-2", old, Collections.emptyList(), BigDecimal.ZERO, null));
    }

    @Test
    void createRevision_inheritsFieldsFromOldSubmission() {
        ReviewSubmission old = createSubmittedSubmission();

        ReviewSubmission rev = ReviewSubmission.createRevision(
                "sub-2", old, Collections.emptyList(), BigDecimal.ZERO, null);

        assertEquals(old.getCaseId(), rev.getCaseId());
        assertEquals(old.getStage(), rev.getStage());
        assertEquals(old.getReviewerId(), rev.getReviewerId());
        assertEquals(old.getBasedOnScoreSheetVersionId(), rev.getBasedOnScoreSheetVersionId());
    }

    @Test
    void createRevision_incrementsRevisionNo() {
        ReviewSubmission old = createSubmittedSubmission();

        ReviewSubmission rev = ReviewSubmission.createRevision(
                "sub-2", old, Collections.emptyList(), BigDecimal.ZERO, null);

        assertEquals(old.getRevisionNo() + 1, rev.getRevisionNo());
    }

    @Test
    void createRevision_statusIsSubmitted() {
        ReviewSubmission old = createSubmittedSubmission();

        ReviewSubmission rev = ReviewSubmission.createRevision(
                "sub-2", old, Collections.emptyList(), BigDecimal.ZERO, null);

        assertEquals(SubmissionStatus.SUBMITTED, rev.getStatus());
    }

    // --- markSuperseded ---

    @Test
    void markSuperseded_fromSubmitted_succeeds() {
        ReviewSubmission sub = createSubmittedSubmission();

        sub.markSuperseded("sub-new");

        assertEquals(SubmissionStatus.SUPERSEDED, sub.getStatus());
        assertEquals("sub-new", sub.getSupersededBySubmissionId());
    }

    @Test
    void markSuperseded_fromDraft_throwsConflict() {
        ReviewSubmission draft = ReviewSubmission.createInitialDraft("sub-1", "c1", "reviewer-1", "ss-v1");

        assertThrows(DomainStateConflictException.class,
                () -> draft.markSuperseded("sub-new"));
    }

    @Test
    void markSuperseded_nullNewSubmissionId_shouldThrow() {
        ReviewSubmission sub = createSubmittedSubmission();

        assertThrows(NullPointerException.class,
                () -> sub.markSuperseded(null));
    }

    // --- Helper ---

    private ReviewSubmission createSubmittedSubmission() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft("sub-1", "c1", "reviewer-1", "ss-v1");
        sub.submit();
        return sub;
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd services/quality-inspection-service && .\gradlew test --tests "*.ReviewSubmissionTest" --no-daemon`
Expected: FAIL — `createRevision` and `markSuperseded` methods do not exist.

- [ ] **Step 3: Implement createRevision() and markSuperseded() on ReviewSubmission**

Add the following methods to `ReviewSubmission.java` (after the `submit()` method, before `requireDraft()`):

```java
    /**
     * Create a revision from an existing SUBMITTED submission.
     * The new submission is directly SUBMITTED (no DRAFT phase for revisions).
     */
    public static ReviewSubmission createRevision(
            String newSubmissionId,
            ReviewSubmission oldSubmission,
            List<AnswerItem> scoredAnswers,
            BigDecimal totalScore,
            String comment) {
        Objects.requireNonNull(newSubmissionId, "newSubmissionId must not be null");
        Objects.requireNonNull(oldSubmission, "oldSubmission must not be null");
        Objects.requireNonNull(scoredAnswers, "scoredAnswers must not be null");
        Objects.requireNonNull(totalScore, "totalScore must not be null");

        if (oldSubmission.status != SubmissionStatus.SUBMITTED) {
            throw new DomainStateConflictException("SUBMISSION_NOT_SUBMITTED",
                    "Can only revise a SUBMITTED submission. Current: " + oldSubmission.status);
        }

        ReviewSubmission rev = new ReviewSubmission();
        rev.submissionId = newSubmissionId;
        rev.caseId = oldSubmission.caseId;
        rev.stage = oldSubmission.stage;
        rev.reviewerId = oldSubmission.reviewerId;
        rev.status = SubmissionStatus.SUBMITTED;
        rev.revisionNo = oldSubmission.revisionNo + 1;
        rev.answerSet = new ArrayList<>(scoredAnswers);
        rev.totalScore = totalScore;
        rev.comment = comment;
        rev.submittedAt = LocalDateTime.now();
        rev.basedOnScoreSheetVersionId = oldSubmission.basedOnScoreSheetVersionId;
        rev.createdAt = LocalDateTime.now();
        rev.updatedAt = rev.createdAt;
        return rev;
    }

    /**
     * Mark this submission as SUPERSEDED by a newer revision.
     * Only SUBMITTED submissions can be superseded.
     */
    public void markSuperseded(String newSubmissionId) {
        if (this.status != SubmissionStatus.SUBMITTED) {
            throw new DomainStateConflictException("SUBMISSION_NOT_SUBMITTED",
                    "Can only supersede a SUBMITTED submission. Current: " + this.status);
        }
        Objects.requireNonNull(newSubmissionId, "newSubmissionId must not be null");
        this.supersededBySubmissionId = newSubmissionId;
        this.status = SubmissionStatus.SUPERSEDED;
        this.updatedAt = LocalDateTime.now();
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd services/quality-inspection-service && .\gradlew test --tests "*.ReviewSubmissionTest" --no-daemon`
Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
cd services/quality-inspection-service
git add -A
git commit -m "feat(b3c): add createRevision + markSuperseded to ReviewSubmission

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 2: ReviewCase Guard + Score Update Methods + Tests

**Files:**
- Modify: `main/java/com/tzuchi/his/quality/inspection/domain/reviewcase/ReviewCase.java`
- Modify: `test/java/com/tzuchi/his/quality/inspection/domain/reviewcase/ReviewCaseTest.java`

- [ ] **Step 1: Write the failing ReviewCase tests**

Append the following test methods to the existing `ReviewCaseTest.java`:

```java
    // --- requireNotTerminal ---

    @Test
    void requireNotTerminal_whenClosed_throwsConflict() {
        ReviewCase rc = createPendingAssignment();
        rc.forceStatus(CaseStatus.CLOSED);

        assertThrows(DomainStateConflictException.class, rc::requireNotTerminal);
    }

    @Test
    void requireNotTerminal_whenCancelled_throwsConflict() {
        ReviewCase rc = createPendingAssignment();
        rc.forceStatus(CaseStatus.CANCELLED);

        assertThrows(DomainStateConflictException.class, rc::requireNotTerminal);
    }

    @Test
    void requireNotTerminal_whenInitialReviewCompleted_doesNotThrow() {
        ReviewCase rc = createPendingAssignment();
        rc.forceStatus(CaseStatus.INITIAL_REVIEW_COMPLETED);

        assertDoesNotThrow(rc::requireNotTerminal);
    }

    @Test
    void requireNotTerminal_whenInProgress_doesNotThrow() {
        ReviewCase rc = createInProgress();

        assertDoesNotThrow(rc::requireNotTerminal);
    }

    // --- updateFinalScore ---

    @Test
    void updateFinalScore_updatesScore() {
        ReviewCase rc = createPendingAssignment();
        rc.forceStatus(CaseStatus.INITIAL_REVIEW_COMPLETED);

        rc.updateFinalScore(new BigDecimal("85.00"));

        assertEquals(new BigDecimal("85.00"), rc.getFinalScore());
    }

    @Test
    void updateFinalScore_whenClosed_throwsConflict() {
        ReviewCase rc = createPendingAssignment();
        rc.forceStatus(CaseStatus.CLOSED);

        assertThrows(DomainStateConflictException.class,
                () -> rc.updateFinalScore(new BigDecimal("85.00")));
    }

    @Test
    void updateFinalScore_whenCancelled_throwsConflict() {
        ReviewCase rc = createPendingAssignment();
        rc.forceStatus(CaseStatus.CANCELLED);

        assertThrows(DomainStateConflictException.class,
                () -> rc.updateFinalScore(new BigDecimal("85.00")));
    }

    @Test
    void updateFinalScore_nullScore_shouldThrow() {
        ReviewCase rc = createPendingAssignment();
        rc.forceStatus(CaseStatus.INITIAL_REVIEW_COMPLETED);

        assertThrows(NullPointerException.class,
                () -> rc.updateFinalScore(null));
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd services/quality-inspection-service && .\gradlew test --tests "*.ReviewCaseTest" --no-daemon`
Expected: FAIL — `requireNotTerminal` and `updateFinalScore` methods do not exist.

- [ ] **Step 3: Implement requireNotTerminal() and updateFinalScore() on ReviewCase**

Add the following methods to `ReviewCase.java` (after `completeInitialReview()`, before `forceStatus()`):

```java
    /**
     * Guard: reject operations on terminal cases (CLOSED or CANCELLED).
     */
    public void requireNotTerminal() {
        if (this.status == CaseStatus.CLOSED || this.status == CaseStatus.CANCELLED) {
            throw new DomainStateConflictException("CASE_TERMINAL",
                    "Case is in terminal status: " + this.status + ". Revision not allowed.");
        }
    }

    /**
     * Update finalScore after a revision. Does not change case status.
     */
    public void updateFinalScore(BigDecimal newScore) {
        requireNotTerminal();
        Objects.requireNonNull(newScore, "newScore must not be null");
        this.finalScore = newScore;
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd services/quality-inspection-service && .\gradlew test --tests "*.ReviewCaseTest" --no-daemon`
Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
cd services/quality-inspection-service
git add -A
git commit -m "feat(b3c): add requireNotTerminal + updateFinalScore to ReviewCase

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 3: ReviewSubmissionRevisedEvent + Repository Extension

**Files:**
- Create: `main/java/com/tzuchi/his/quality/inspection/domain/reviewcase/submission/event/ReviewSubmissionRevisedEvent.java`
- Modify: `main/java/com/tzuchi/his/quality/inspection/domain/reviewcase/submission/ReviewSubmissionRepository.java`
- Modify: `test/java/com/tzuchi/his/quality/inspection/testfixture/InMemoryReviewSubmissionRepository.java`
- Modify: `main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/submission/ReviewSubmissionJpaRepository.java`
- Modify: `main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/submission/ReviewSubmissionRepositoryImpl.java`

- [ ] **Step 1: Create ReviewSubmissionRevisedEvent**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

/**
 * Published when an initial review submission is revised.
 */
public class ReviewSubmissionRevisedEvent extends DomainEvent {

    public ReviewSubmissionRevisedEvent(Object source, String actorUserId, String actorUserName,
                                         String hospitalId, String caseId,
                                         String oldSubmissionId, String newSubmissionId,
                                         String stage, int revisionNo) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewSubmission", newSubmissionId, "REVISED",
              String.format("{\"caseId\":\"%s\",\"oldSubmissionId\":\"%s\",\"stage\":\"%s\",\"revisionNo\":%d}",
                      caseId, oldSubmissionId, stage, revisionNo));
    }
}
```

- [ ] **Step 2: Add findAllByCaseId to ReviewSubmissionRepository interface**

Add below `findLatestByCaseIdAndStage`:

```java
    List<ReviewSubmission> findAllByCaseId(String caseId);
```

- [ ] **Step 3: Implement findAllByCaseId in InMemoryReviewSubmissionRepository**

Add to `InMemoryReviewSubmissionRepository.java`:

```java
    @Override
    public List<ReviewSubmission> findAllByCaseId(String caseId) {
        return store.values().stream()
                .filter(s -> caseId.equals(s.getCaseId()))
                .collect(Collectors.toList());
    }
```

- [ ] **Step 4: Add JPA query method to ReviewSubmissionJpaRepository**

Add below the existing `findByCaseIdAndStageOrderByRevisionNoDesc` method:

```java
    List<ReviewSubmissionEntity> findByCaseId(String caseId);
```

- [ ] **Step 5: Implement findAllByCaseId in ReviewSubmissionRepositoryImpl**

Add below the existing `findLatestByCaseIdAndStage` method:

```java
    @Override
    public List<ReviewSubmission> findAllByCaseId(String caseId) {
        return jpa.findByCaseId(caseId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
```

- [ ] **Step 6: Run full test suite to verify nothing is broken**

Run: `cd services/quality-inspection-service && .\gradlew test --no-daemon`
Expected: ALL PASS (no consumers of new methods yet, just interface additions)

- [ ] **Step 7: Commit**

```bash
cd services/quality-inspection-service
git add -A
git commit -m "feat(b3c): add ReviewSubmissionRevisedEvent + findAllByCaseId repository

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 4: ReviseInitialReviewUseCase + RevisionResult DTO

**Files:**
- Create: `main/java/com/tzuchi/his/quality/inspection/application/reviewcase/RevisionResult.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/application/reviewcase/ReviseInitialReviewUseCase.java`

- [ ] **Step 1: Create RevisionResult DTO**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import java.math.BigDecimal;

/**
 * Result of a revision operation — returned to the caller with new submission details.
 */
public class RevisionResult {

    private final String newSubmissionId;
    private final int revisionNo;
    private final BigDecimal totalScore;
    private final String status;

    public RevisionResult(String newSubmissionId, int revisionNo,
                          BigDecimal totalScore, String status) {
        this.newSubmissionId = newSubmissionId;
        this.revisionNo = revisionNo;
        this.totalScore = totalScore;
        this.status = status;
    }

    public String getNewSubmissionId() { return newSubmissionId; }
    public int getRevisionNo() { return revisionNo; }
    public BigDecimal getTotalScore() { return totalScore; }
    public String getStatus() { return status; }
}
```

- [ ] **Step 2: Create ReviseInitialReviewUseCase**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.ReviewSubmissionRevisedEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import com.tzuchi.his.quality.inspection.interfaces.rest.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviseInitialReviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewSubmissionRepository submissionRepository;
    private final ScoreSheetRepository scoreSheetRepository;
    private final DomainEventPublisher eventPublisher;
    private final ScoreCalculationService scoreCalculationService;

    public ReviseInitialReviewUseCase(ReviewCaseRepository caseRepository,
                                       ReviewSubmissionRepository submissionRepository,
                                       ScoreSheetRepository scoreSheetRepository,
                                       DomainEventPublisher eventPublisher,
                                       ScoreCalculationService scoreCalculationService) {
        this.caseRepository = caseRepository;
        this.submissionRepository = submissionRepository;
        this.scoreSheetRepository = scoreSheetRepository;
        this.eventPublisher = eventPublisher;
        this.scoreCalculationService = scoreCalculationService;
    }

    public RevisionResult execute(ActorContext actor, String caseId, String submissionId,
                                   List<AnswerInput> answers, String comment) {
        actor.requireRole(DomainRole.REVIEWER);

        // Load and validate ReviewCase
        ReviewCase reviewCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND",
                        "ReviewCase not found: " + caseId));
        reviewCase.requireNotTerminal();

        // Load and validate old submission
        ReviewSubmission oldSubmission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("SUBMISSION_NOT_FOUND",
                        "Submission not found: " + submissionId));

        if (!oldSubmission.getCaseId().equals(caseId)) {
            throw new BadRequestException(
                    "Submission " + submissionId + " does not belong to case " + caseId);
        }

        if (oldSubmission.getStage() != ReviewStage.INITIAL) {
            throw new DomainRuleViolationException("SUBMISSION_NOT_INITIAL",
                    "Can only revise INITIAL stage submissions. Current: " + oldSubmission.getStage());
        }

        if (!actor.getUserId().equals(oldSubmission.getReviewerId())) {
            throw new AccessDeniedException("NOT_SUBMISSION_OWNER",
                    "Only the original reviewer can revise this submission");
        }

        // Load score sheet for scoring
        ScoreSheetDefinition scoreSheet = scoreSheetRepository
                .findById(oldSubmission.getBasedOnScoreSheetVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                        "ScoreSheetDefinition not found: " + oldSubmission.getBasedOnScoreSheetVersionId()));

        // Calculate scores
        List<AnswerItem> rawItems = answers.stream()
                .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                .collect(Collectors.toList());
        ScoreResult result = scoreCalculationService.calculate(rawItems, scoreSheet);

        // Create new revision
        String newSubmissionId = UUID.randomUUID().toString();
        ReviewSubmission newSubmission = ReviewSubmission.createRevision(
                newSubmissionId, oldSubmission, result.getScoredAnswers(),
                result.getTotalScore(), comment);

        // Mark old as superseded
        oldSubmission.markSuperseded(newSubmissionId);

        // Update case's final score (no status change)
        reviewCase.updateFinalScore(result.getTotalScore());

        // Persist
        submissionRepository.save(oldSubmission);
        submissionRepository.save(newSubmission);
        caseRepository.save(reviewCase);

        // Publish event
        eventPublisher.publish(new ReviewSubmissionRevisedEvent(
                this, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId,
                submissionId, newSubmissionId,
                oldSubmission.getStage().name(), newSubmission.getRevisionNo()));

        return new RevisionResult(
                newSubmissionId, newSubmission.getRevisionNo(),
                result.getTotalScore(), newSubmission.getStatus().name());
    }
}
```

- [ ] **Step 3: Run compilation check**

Run: `cd services/quality-inspection-service && .\gradlew compileJava --no-daemon`
Expected: PASS (compiles successfully)

- [ ] **Step 4: Commit**

```bash
cd services/quality-inspection-service
git add -A
git commit -m "feat(b3c): add ReviseInitialReviewUseCase + RevisionResult

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 5: ReviseInitialReviewUseCase Tests

**Files:**
- Create: `test/java/com/tzuchi/his/quality/inspection/application/reviewcase/ReviseInitialReviewUseCaseTest.java`

- [ ] **Step 1: Write all use case tests**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.ReviewSubmissionRevisedEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.*;
import com.tzuchi.his.quality.inspection.interfaces.rest.BadRequestException;
import com.tzuchi.his.quality.inspection.testfixture.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ReviseInitialReviewUseCaseTest {

    private InMemoryReviewCaseRepository caseRepo;
    private InMemoryReviewSubmissionRepository submissionRepo;
    private InMemoryScoreSheetRepository scoreSheetRepo;
    private CapturingDomainEventPublisher eventPublisher;
    private ReviseInitialReviewUseCase useCase;

    private static final String CASE_ID = "c1";
    private static final String SUBMISSION_ID = "sub-1";
    private static final String PROJECT_ID = "p1";
    private static final String HOSPITAL_ID = "H001";
    private static final String REVIEWER_ID = "reviewer-1";
    private static final String SCORE_SHEET_ID = "ss-1";

    @BeforeEach
    void setUp() {
        caseRepo = new InMemoryReviewCaseRepository();
        submissionRepo = new InMemoryReviewSubmissionRepository();
        scoreSheetRepo = new InMemoryScoreSheetRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        ScoreCalculationService scoreCalcService = new ScoreCalculationService();
        useCase = new ReviseInitialReviewUseCase(
                caseRepo, submissionRepo, scoreSheetRepo, eventPublisher, scoreCalcService);
    }

    private ActorContext reviewer() {
        return new ActorContext(REVIEWER_ID, "Test Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));
    }

    private ScoreSheetDefinition seedScoreSheet() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft(SCORE_SHEET_ID, PROJECT_ID);
        ScoreSheetSection section = new ScoreSheetSection("sec-1", "Section 1");
        section.addItem(new ScoreItem("Q001", "Q001", "Bool Question", ScoreType.BOOLEAN,
                new BigDecimal("10"), new BigDecimal("1.0"),
                RequiredRule.ALWAYS_REQUIRED, null));
        section.addItem(new ScoreItem("Q002", "Q002", "Range Question", ScoreType.RANGE,
                new BigDecimal("20"), new BigDecimal("0.5"),
                RequiredRule.OPTIONAL, null));
        ss.publishVersion();
        scoreSheetRepo.save(ss);
        return ss;
    }

    private ReviewCase seedCompletedCase() {
        ReviewCase rc = ReviewCase.createFromSampling(
                CASE_ID, "b1", PROJECT_ID, HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer(REVIEWER_ID, LocalDateTime.of(2026, 2, 1, 23, 59));
        rc.startInitialReview(SUBMISSION_ID, SCORE_SHEET_ID);
        rc.completeInitialReview(new BigDecimal("17.5"));
        caseRepo.save(rc);
        return rc;
    }

    private ReviewSubmission seedSubmittedSubmission() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        List<AnswerItem> answers = Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.0")),
                AnswerItem.ofScored("Q002", "15", new BigDecimal("7.5"))
        );
        sub.updateDraft(answers, new BigDecimal("17.5"), "initial comment");
        sub.submit();
        submissionRepo.save(sub);
        return sub;
    }

    private List<AnswerInput> revisedAnswers() {
        return Arrays.asList(
                new AnswerInput("Q001", "true"),
                new AnswerInput("Q002", "18")
        );
    }

    @Test
    void revise_happyPath_createsNewSubmission() {
        seedScoreSheet();
        seedCompletedCase();
        seedSubmittedSubmission();

        RevisionResult result = useCase.execute(
                reviewer(), CASE_ID, SUBMISSION_ID, revisedAnswers(), "revised comment");

        assertNotNull(result.getNewSubmissionId());
        assertEquals(2, result.getRevisionNo());
        assertEquals("SUBMITTED", result.getStatus());

        // Old submission is SUPERSEDED
        ReviewSubmission oldSub = submissionRepo.store.get(SUBMISSION_ID);
        assertEquals(SubmissionStatus.SUPERSEDED, oldSub.getStatus());
        assertEquals(result.getNewSubmissionId(), oldSub.getSupersededBySubmissionId());

        // New submission exists and is SUBMITTED
        ReviewSubmission newSub = submissionRepo.store.get(result.getNewSubmissionId());
        assertNotNull(newSub);
        assertEquals(SubmissionStatus.SUBMITTED, newSub.getStatus());
        assertEquals(2, newSub.getRevisionNo());
        assertEquals("revised comment", newSub.getComment());

        // Case finalScore updated
        ReviewCase savedCase = caseRepo.store.get(CASE_ID);
        assertEquals(result.getTotalScore(), savedCase.getFinalScore());

        // Event published
        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewSubmissionRevisedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void revise_actorNotReviewer_throwsUnauthorized() {
        ActorContext manager = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of(HOSPITAL_ID));

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(manager, CASE_ID, SUBMISSION_ID, revisedAnswers(), null));
    }

    @Test
    void revise_caseNotFound_throwsNotFound() {
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(reviewer(), "nonexistent", SUBMISSION_ID, revisedAnswers(), null));
    }

    @Test
    void revise_caseClosed_throwsConflict() {
        seedScoreSheet();
        ReviewCase rc = ReviewCase.createFromSampling(
                CASE_ID, "b1", PROJECT_ID, HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
        rc.forceStatus(CaseStatus.CLOSED);
        caseRepo.save(rc);

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(reviewer(), CASE_ID, SUBMISSION_ID, revisedAnswers(), null));
    }

    @Test
    void revise_caseCancelled_throwsConflict() {
        seedScoreSheet();
        ReviewCase rc = ReviewCase.createFromSampling(
                CASE_ID, "b1", PROJECT_ID, HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
        rc.forceStatus(CaseStatus.CANCELLED);
        caseRepo.save(rc);

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(reviewer(), CASE_ID, SUBMISSION_ID, revisedAnswers(), null));
    }

    @Test
    void revise_submissionNotFound_throwsNotFound() {
        seedScoreSheet();
        seedCompletedCase();

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(reviewer(), CASE_ID, "nonexistent", revisedAnswers(), null));
    }

    @Test
    void revise_submissionCaseIdMismatch_throwsBadRequest() {
        seedScoreSheet();
        seedCompletedCase();

        // Submission belongs to different case
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, "different-case", REVIEWER_ID, SCORE_SHEET_ID);
        sub.submit();
        submissionRepo.save(sub);

        assertThrows(BadRequestException.class,
                () -> useCase.execute(reviewer(), CASE_ID, SUBMISSION_ID, revisedAnswers(), null));
    }

    @Test
    void revise_submissionNotInitialStage_throwsRuleViolation() {
        seedScoreSheet();
        seedCompletedCase();

        // Create a REREVIEW stage submission via reconstitute
        ReviewSubmission sub = ReviewSubmission.reconstitute(
                SUBMISSION_ID, CASE_ID, com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage.REREVIEW,
                REVIEWER_ID, SubmissionStatus.SUBMITTED, 1,
                Collections.emptyList(), BigDecimal.ZERO, null, LocalDateTime.now(),
                SCORE_SHEET_ID, null, null, LocalDateTime.now(), LocalDateTime.now());
        submissionRepo.save(sub);

        assertThrows(DomainRuleViolationException.class,
                () -> useCase.execute(reviewer(), CASE_ID, SUBMISSION_ID, revisedAnswers(), null));
    }

    @Test
    void revise_actorNotSubmissionOwner_throwsAccessDenied() {
        seedScoreSheet();
        seedCompletedCase();
        seedSubmittedSubmission();

        ActorContext other = new ActorContext("other-reviewer", "Other",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(other, CASE_ID, SUBMISSION_ID, revisedAnswers(), null));
    }

    @Test
    void revise_submissionIsDraft_throwsConflict() {
        seedScoreSheet();
        seedCompletedCase();

        // Submission in DRAFT status (not submitted)
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        submissionRepo.save(sub);

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(reviewer(), CASE_ID, SUBMISSION_ID, revisedAnswers(), null));
    }
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `cd services/quality-inspection-service && .\gradlew test --tests "*.ReviseInitialReviewUseCaseTest" --no-daemon`
Expected: ALL 10 PASS

- [ ] **Step 3: Commit**

```bash
cd services/quality-inspection-service
git add -A
git commit -m "test(b3c): add ReviseInitialReviewUseCase tests (10 scenarios)

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 6: GetRevisionHistoryUseCase + Tests

**Files:**
- Create: `main/java/com/tzuchi/his/quality/inspection/application/reviewcase/GetRevisionHistoryUseCase.java`
- Create: `test/java/com/tzuchi/his/quality/inspection/application/reviewcase/GetRevisionHistoryUseCaseTest.java`

- [ ] **Step 1: Write the failing GetRevisionHistoryUseCase tests**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetRevisionHistoryUseCaseTest {

    private InMemoryReviewSubmissionRepository submissionRepo;
    private GetRevisionHistoryUseCase useCase;

    private static final String CASE_ID = "c1";
    private static final String REVIEWER_ID = "reviewer-1";
    private static final String HOSPITAL_ID = "H001";
    private static final String SCORE_SHEET_ID = "ss-1";

    @BeforeEach
    void setUp() {
        submissionRepo = new InMemoryReviewSubmissionRepository();
        useCase = new GetRevisionHistoryUseCase(submissionRepo);
    }

    private ActorContext reviewer() {
        return new ActorContext(REVIEWER_ID, "Test Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));
    }

    @Test
    void getHistory_returnsAllSubmissions_sortedByRevisionNo() {
        // Seed rev-2 first, rev-1 second (to test sorting)
        ReviewSubmission sub1 = ReviewSubmission.createInitialDraft(
                "sub-1", CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        sub1.submit();
        ReviewSubmission sub2 = ReviewSubmission.createRevision(
                "sub-2", sub1, Collections.emptyList(), BigDecimal.ZERO, null);
        sub1.markSuperseded("sub-2");

        // Store in reverse order
        submissionRepo.save(sub2);
        submissionRepo.save(sub1);

        List<ReviewSubmission> result = useCase.execute(reviewer(), CASE_ID);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getRevisionNo());
        assertEquals(2, result.get(1).getRevisionNo());
    }

    @Test
    void getHistory_emptyCase_returnsEmptyList() {
        List<ReviewSubmission> result = useCase.execute(reviewer(), "nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void getHistory_wrongRole_throwsUnauthorized() {
        ActorContext noRole = new ActorContext("u1", "NoRole",
                EnumSet.noneOf(DomainRole.class), Optional.of(HOSPITAL_ID));

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(noRole, CASE_ID));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd services/quality-inspection-service && .\gradlew test --tests "*.GetRevisionHistoryUseCaseTest" --no-daemon`
Expected: FAIL — `GetRevisionHistoryUseCase` does not exist.

- [ ] **Step 3: Create GetRevisionHistoryUseCase**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetRevisionHistoryUseCase {

    private final ReviewSubmissionRepository submissionRepository;

    public GetRevisionHistoryUseCase(ReviewSubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    public List<ReviewSubmission> execute(ActorContext actor, String caseId) {
        actor.requireAnyRole(DomainRole.REVIEWER, DomainRole.HOSPITAL_MANAGER,
                DomainRole.REREVIEWER, DomainRole.AUDITOR);

        List<ReviewSubmission> submissions = submissionRepository.findAllByCaseId(caseId);

        return submissions.stream()
                .sorted(Comparator.comparing(ReviewSubmission::getStage)
                        .thenComparing(ReviewSubmission::getRevisionNo))
                .collect(Collectors.toList());
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd services/quality-inspection-service && .\gradlew test --tests "*.GetRevisionHistoryUseCaseTest" --no-daemon`
Expected: ALL 3 PASS

- [ ] **Step 5: Commit**

```bash
cd services/quality-inspection-service
git add -A
git commit -m "feat(b3c): add GetRevisionHistoryUseCase + tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 7: REST DTOs + Controller Endpoints

**Files:**
- Create: `main/java/com/tzuchi/his/quality/inspection/interfaces/rest/dto/ReviseInitialReviewRequest.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/interfaces/rest/dto/RevisionResultResponse.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/interfaces/rest/dto/RevisionHistoryResponse.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/interfaces/rest/dto/SubmissionSummaryDto.java`
- Modify: `main/java/com/tzuchi/his/quality/inspection/interfaces/rest/ReviewCaseController.java`

- [ ] **Step 1: Create ReviseInitialReviewRequest DTO**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

public class ReviseInitialReviewRequest {

    @NotNull(message = "answers must not be null")
    @Valid
    private List<SaveInitialReviewDraftRequest.AnswerInputDto> answers;

    private String comment;

    public List<SaveInitialReviewDraftRequest.AnswerInputDto> getAnswers() {
        return answers;
    }

    public void setAnswers(List<SaveInitialReviewDraftRequest.AnswerInputDto> answers) {
        this.answers = answers;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
```

- [ ] **Step 2: Create RevisionResultResponse DTO**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.math.BigDecimal;

public class RevisionResultResponse {

    private String newSubmissionId;
    private int revisionNo;
    private BigDecimal totalScore;
    private String status;

    public RevisionResultResponse() {}

    public RevisionResultResponse(String newSubmissionId, int revisionNo,
                                   BigDecimal totalScore, String status) {
        this.newSubmissionId = newSubmissionId;
        this.revisionNo = revisionNo;
        this.totalScore = totalScore;
        this.status = status;
    }

    public String getNewSubmissionId() { return newSubmissionId; }
    public void setNewSubmissionId(String newSubmissionId) { this.newSubmissionId = newSubmissionId; }
    public int getRevisionNo() { return revisionNo; }
    public void setRevisionNo(int revisionNo) { this.revisionNo = revisionNo; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
```

- [ ] **Step 3: Create SubmissionSummaryDto**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SubmissionSummaryDto {

    private String submissionId;
    private String stage;
    private String reviewerId;
    private String status;
    private int revisionNo;
    private BigDecimal totalScore;
    private String comment;
    private LocalDateTime submittedAt;
    private String supersededBySubmissionId;

    public SubmissionSummaryDto() {}

    public SubmissionSummaryDto(String submissionId, String stage, String reviewerId,
                                 String status, int revisionNo, BigDecimal totalScore,
                                 String comment, LocalDateTime submittedAt,
                                 String supersededBySubmissionId) {
        this.submissionId = submissionId;
        this.stage = stage;
        this.reviewerId = reviewerId;
        this.status = status;
        this.revisionNo = revisionNo;
        this.totalScore = totalScore;
        this.comment = comment;
        this.submittedAt = submittedAt;
        this.supersededBySubmissionId = supersededBySubmissionId;
    }

    public String getSubmissionId() { return submissionId; }
    public void setSubmissionId(String submissionId) { this.submissionId = submissionId; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getRevisionNo() { return revisionNo; }
    public void setRevisionNo(int revisionNo) { this.revisionNo = revisionNo; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public String getSupersededBySubmissionId() { return supersededBySubmissionId; }
    public void setSupersededBySubmissionId(String v) { this.supersededBySubmissionId = v; }
}
```

- [ ] **Step 4: Create RevisionHistoryResponse DTO**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.util.List;

public class RevisionHistoryResponse {

    private String caseId;
    private List<SubmissionSummaryDto> submissions;

    public RevisionHistoryResponse() {}

    public RevisionHistoryResponse(String caseId, List<SubmissionSummaryDto> submissions) {
        this.caseId = caseId;
        this.submissions = submissions;
    }

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public List<SubmissionSummaryDto> getSubmissions() { return submissions; }
    public void setSubmissions(List<SubmissionSummaryDto> submissions) { this.submissions = submissions; }
}
```

- [ ] **Step 5: Add 2 new endpoints to ReviewCaseController**

Add the two new use case fields to the constructor and inject them. Then add the two endpoint methods.

Add to the field declarations (after `submitInitialReviewUseCase`):

```java
    private final ReviseInitialReviewUseCase reviseInitialReviewUseCase;
    private final GetRevisionHistoryUseCase getRevisionHistoryUseCase;
```

Update the constructor to accept and inject them (add after `SubmitInitialReviewUseCase submitInitialReviewUseCase`):

```java
    public ReviewCaseController(AssignReviewCaseUseCase assignUseCase,
                                 ReassignReviewCaseUseCase reassignUseCase,
                                 CancelReviewCaseUseCase cancelUseCase,
                                 ReturnInitialReviewUseCase returnUseCase,
                                 ReopenInitialReviewUseCase reopenUseCase,
                                 StartInitialReviewUseCase startInitialReviewUseCase,
                                 SaveInitialReviewDraftUseCase saveInitialReviewDraftUseCase,
                                 SubmitInitialReviewUseCase submitInitialReviewUseCase,
                                 ReviseInitialReviewUseCase reviseInitialReviewUseCase,
                                 GetRevisionHistoryUseCase getRevisionHistoryUseCase) {
        this.assignUseCase = assignUseCase;
        this.reassignUseCase = reassignUseCase;
        this.cancelUseCase = cancelUseCase;
        this.returnUseCase = returnUseCase;
        this.reopenUseCase = reopenUseCase;
        this.startInitialReviewUseCase = startInitialReviewUseCase;
        this.saveInitialReviewDraftUseCase = saveInitialReviewDraftUseCase;
        this.submitInitialReviewUseCase = submitInitialReviewUseCase;
        this.reviseInitialReviewUseCase = reviseInitialReviewUseCase;
        this.getRevisionHistoryUseCase = getRevisionHistoryUseCase;
    }
```

Add the two new endpoint methods (after `submitInitialReview`):

```java
    // --- B3c endpoints ---

    @PostMapping("/{caseId}/submissions/{submissionId}/revise")
    public ResponseEntity<ApiResponse<RevisionResultResponse>> reviseInitialReview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @PathVariable String submissionId,
            @Valid @RequestBody ReviseInitialReviewRequest request) {
        List<com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput> answers =
                request.getAnswers().stream()
                        .filter(Objects::nonNull)
                        .map(dto -> new com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput(
                                dto.getItemId(), dto.getAnswerValue()))
                        .collect(Collectors.toList());
        RevisionResult result = reviseInitialReviewUseCase.execute(
                actor, caseId, submissionId, answers, request.getComment());
        return ResponseEntity.ok(ApiResponse.ok(new RevisionResultResponse(
                result.getNewSubmissionId(), result.getRevisionNo(),
                result.getTotalScore(), result.getStatus())));
    }

    @GetMapping("/{caseId}/revision-history")
    public ResponseEntity<ApiResponse<RevisionHistoryResponse>> getRevisionHistory(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        java.util.List<com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission> submissions =
                getRevisionHistoryUseCase.execute(actor, caseId);
        java.util.List<SubmissionSummaryDto> dtos = submissions.stream()
                .map(s -> new SubmissionSummaryDto(
                        s.getSubmissionId(), s.getStage().name(), s.getReviewerId(),
                        s.getStatus().name(), s.getRevisionNo(), s.getTotalScore(),
                        s.getComment(), s.getSubmittedAt(), s.getSupersededBySubmissionId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(new RevisionHistoryResponse(caseId, dtos)));
    }
```

Also add the necessary import at the top of the controller:

```java
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
```

- [ ] **Step 6: Run compilation check**

Run: `cd services/quality-inspection-service && .\gradlew compileJava --no-daemon`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
cd services/quality-inspection-service
git add -A
git commit -m "feat(b3c): add REST DTOs + revise/revision-history endpoints

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 8: Integration Tests (RevisionApiTest)

**Files:**
- Create: `test/java/com/tzuchi/his/quality/inspection/interfaces/rest/RevisionApiTest.java`

- [ ] **Step 1: Write integration tests**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetStatus;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.project.ReviewProjectEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.project.ReviewProjectJpaRepository;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseJpaRepository;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.scoresheet.ScoreItemEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.scoresheet.ScoreSheetEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.scoresheet.ScoreSheetJpaRepository;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.scoresheet.ScoreSheetSectionEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.submission.ReviewSubmissionEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.submission.ReviewSubmissionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RevisionApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ReviewProjectJpaRepository projectJpa;
    @Autowired private ScoreSheetJpaRepository scoreSheetJpa;
    @Autowired private ReviewCaseJpaRepository caseJpa;
    @Autowired private ReviewSubmissionJpaRepository submissionJpa;

    private static final String BASE = "/review-cases";
    private static final String CASE_ID = "c1";
    private static final String SUBMISSION_ID = "sub-1";
    private static final String PROJECT_ID = "p1";
    private static final String SCORE_SHEET_ID = "ss-1";
    private static final String REVIEWER_ID = "reviewer-1";
    private static final String HOSPITAL_ID = "H001";

    private final String reviewerToken = TestTokenFactory.reviewerToken(REVIEWER_ID, HOSPITAL_ID);
    private final String hmToken = TestTokenFactory.hospitalManagerToken();

    @BeforeEach
    void setUp() {
        seedProjectAndScoreSheet();
    }

    private void seedProjectAndScoreSheet() {
        ScoreItemEntity q001 = new ScoreItemEntity();
        q001.setItemId("Q001");
        q001.setCode("Q001");
        q001.setTitle("Bool Question");
        q001.setScoreType("BOOLEAN");
        q001.setMaxScore(new BigDecimal("10"));
        q001.setWeight(new BigDecimal("1.0"));
        q001.setRequiredRule("ALWAYS_REQUIRED");

        ScoreItemEntity q002 = new ScoreItemEntity();
        q002.setItemId("Q002");
        q002.setCode("Q002");
        q002.setTitle("Range Question");
        q002.setScoreType("RANGE");
        q002.setMaxScore(new BigDecimal("20"));
        q002.setWeight(new BigDecimal("0.5"));
        q002.setRequiredRule("OPTIONAL");

        ScoreSheetSectionEntity section = new ScoreSheetSectionEntity();
        section.setSectionId("sec-1");
        section.setTitle("Section 1");
        List<ScoreItemEntity> items = new ArrayList<>();
        items.add(q001);
        items.add(q002);
        section.setItems(items);

        ScoreSheetEntity ssEntity = new ScoreSheetEntity();
        ssEntity.setScoreSheetId(SCORE_SHEET_ID);
        ssEntity.setProjectId(PROJECT_ID);
        ssEntity.setVersion(1);
        ssEntity.setStatus(ScoreSheetStatus.PUBLISHED);
        List<ScoreSheetSectionEntity> sections = new ArrayList<>();
        sections.add(section);
        ssEntity.setSections(sections);
        scoreSheetJpa.save(ssEntity);

        ReviewProjectEntity proj = new ReviewProjectEntity();
        proj.setProjectId(PROJECT_ID);
        proj.setName("Test Project");
        proj.setHospitalId(HOSPITAL_ID);
        proj.setStatus(ProjectStatus.ACTIVE);
        proj.setVersionNumber(1);
        proj.setVersionPublished(true);
        proj.setBoundScoreSheetId(SCORE_SHEET_ID);
        projectJpa.save(proj);
    }

    private void seedCompletedCaseWithSubmittedSubmission() {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(CASE_ID);
        e.setBatchId("b1");
        e.setProjectId(PROJECT_ID);
        e.setHospitalId(HOSPITAL_ID);
        e.setSourceCaseKey("SK-001");
        e.setMedicalRecordType("OUTPATIENT");
        e.setStatus(CaseStatus.INITIAL_REVIEW_COMPLETED);
        e.setReviewerId(REVIEWER_ID);
        e.setReviewerAssignedAt(LocalDateTime.now());
        e.setDueAt(LocalDateTime.of(2026, 2, 1, 23, 59));
        e.setLatestInitialSubmissionId(SUBMISSION_ID);
        e.setScoreSheetVersionRef(SCORE_SHEET_ID);
        e.setFinalScore(new BigDecimal("17.5"));
        caseJpa.save(e);

        ReviewSubmissionEntity sub = new ReviewSubmissionEntity();
        sub.setSubmissionId(SUBMISSION_ID);
        sub.setCaseId(CASE_ID);
        sub.setStage(ReviewStage.INITIAL);
        sub.setReviewerId(REVIEWER_ID);
        sub.setStatus(SubmissionStatus.SUBMITTED);
        sub.setRevisionNo(1);
        sub.setAnswerSet(Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.0")),
                AnswerItem.ofScored("Q002", "15", new BigDecimal("7.5"))
        ));
        sub.setTotalScore(new BigDecimal("17.5"));
        sub.setComment("initial review");
        sub.setSubmittedAt(LocalDateTime.now());
        sub.setBasedOnScoreSheetVersionId(SCORE_SHEET_ID);
        sub.setCreatedAt(LocalDateTime.now());
        sub.setUpdatedAt(LocalDateTime.now());
        submissionJpa.save(sub);
    }

    // --- REVISE INITIAL REVIEW ---

    @Test
    void revise_returns200_withRevisionResult() throws Exception {
        seedCompletedCaseWithSubmittedSubmission();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("answers", Arrays.asList(
                Map.of("itemId", "Q001", "answerValue", "true"),
                Map.of("itemId", "Q002", "answerValue", "18")
        ));
        body.put("comment", "revised answer");

        mockMvc.perform(post(BASE + "/c1/submissions/sub-1/revise")
                        .header("api-key", reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.newSubmissionId").isString())
                .andExpect(jsonPath("$.data.revisionNo").value(2))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.totalScore").isNumber());

        // Verify old submission is SUPERSEDED in DB
        ReviewSubmissionEntity oldSub = submissionJpa.findById(SUBMISSION_ID).orElseThrow();
        assertEquals(SubmissionStatus.SUPERSEDED, oldSub.getStatus());
    }

    @Test
    void revise_wrongRole_returns403() throws Exception {
        seedCompletedCaseWithSubmittedSubmission();

        Map<String, Object> body = Map.of(
                "answers", Collections.emptyList(),
                "comment", "test"
        );

        mockMvc.perform(post(BASE + "/c1/submissions/sub-1/revise")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    void revise_terminalCase_returns409() throws Exception {
        seedCompletedCaseWithSubmittedSubmission();

        // Force case to CLOSED
        ReviewCaseEntity caseEntity = caseJpa.findById(CASE_ID).orElseThrow();
        caseEntity.setStatus(CaseStatus.CLOSED);
        caseJpa.save(caseEntity);

        Map<String, Object> body = Map.of(
                "answers", Arrays.asList(
                        Map.of("itemId", "Q001", "answerValue", "true")
                ),
                "comment", "should fail"
        );

        mockMvc.perform(post(BASE + "/c1/submissions/sub-1/revise")
                        .header("api-key", reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    // --- GET REVISION HISTORY ---

    @Test
    void getRevisionHistory_returns200_withSubmissions() throws Exception {
        seedCompletedCaseWithSubmittedSubmission();

        mockMvc.perform(get(BASE + "/c1/revision-history")
                        .header("api-key", reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.caseId").value(CASE_ID))
                .andExpect(jsonPath("$.data.submissions", hasSize(1)))
                .andExpect(jsonPath("$.data.submissions[0].submissionId").value(SUBMISSION_ID))
                .andExpect(jsonPath("$.data.submissions[0].stage").value("INITIAL"))
                .andExpect(jsonPath("$.data.submissions[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.submissions[0].revisionNo").value(1));
    }

    @Test
    void getRevisionHistory_hmRole_returns200() throws Exception {
        seedCompletedCaseWithSubmittedSubmission();

        mockMvc.perform(get(BASE + "/c1/revision-history")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getRevisionHistory_emptyCase_returnsEmptyList() throws Exception {
        mockMvc.perform(get(BASE + "/nonexistent/revision-history")
                        .header("api-key", reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submissions", hasSize(0)));
    }

    // --- FULL ROUND-TRIP ---

    @Test
    void fullRoundTrip_revise_thenQueryHistory() throws Exception {
        seedCompletedCaseWithSubmittedSubmission();

        // Step 1: Revise
        Map<String, Object> reviseBody = new LinkedHashMap<>();
        reviseBody.put("answers", Arrays.asList(
                Map.of("itemId", "Q001", "answerValue", "true"),
                Map.of("itemId", "Q002", "answerValue", "18")
        ));
        reviseBody.put("comment", "revised");

        mockMvc.perform(post(BASE + "/c1/submissions/sub-1/revise")
                        .header("api-key", reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviseBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revisionNo").value(2));

        // Step 2: Query history — should have 2 submissions
        mockMvc.perform(get(BASE + "/c1/revision-history")
                        .header("api-key", reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submissions", hasSize(2)))
                .andExpect(jsonPath("$.data.submissions[0].revisionNo").value(1))
                .andExpect(jsonPath("$.data.submissions[0].status").value("SUPERSEDED"))
                .andExpect(jsonPath("$.data.submissions[1].revisionNo").value(2))
                .andExpect(jsonPath("$.data.submissions[1].status").value("SUBMITTED"));
    }
}
```

- [ ] **Step 2: Run integration tests**

Run: `cd services/quality-inspection-service && .\gradlew test --tests "*.RevisionApiTest" --no-daemon`
Expected: ALL 6 PASS

- [ ] **Step 3: Commit**

```bash
cd services/quality-inspection-service
git add -A
git commit -m "test(b3c): add RevisionApiTest integration tests (6 scenarios)

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 9: Full Test Suite Verification + Final Commit

**Files:** None (verification only)

- [ ] **Step 1: Run the complete test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test --no-daemon`
Expected: ALL PASS — no regressions from B3c changes.

- [ ] **Step 2: If any failures, debug and fix**

If any existing tests fail, investigate the root cause. The most likely issue would be `ReviewCaseController` constructor change requiring adjustment in existing integration tests that mock or instantiate the controller.

- [ ] **Step 3: Final commit (if any fixes were needed)**

```bash
cd services/quality-inspection-service
git add -A
git commit -m "fix(b3c): resolve test regressions from B3c changes

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

- [ ] **Step 4: Verify clean build**

Run: `cd services/quality-inspection-service && .\gradlew clean build --no-daemon`
Expected: BUILD SUCCESSFUL
