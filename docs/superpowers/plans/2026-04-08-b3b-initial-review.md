# B3b — Initial Review Operations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the initial review workflow — start review, save draft with server-side scoring, and submit — adding business methods to ReviewSubmission/ReviewCase, a scoring engine, 3 use cases, JPA persistence, and REST endpoints.

**Architecture:** TDD bottom-up approach: domain value objects → domain aggregates → application services (scoring + use cases) → infrastructure (JPA + JSON converter) → interface layer (REST + DTOs). Each layer tested independently before wiring. All 3 endpoints operate under REVIEWER role with identity guard (assigned reviewer only).

**Tech Stack:** Java 11, Spring Boot 2.7.18, JPA/Hibernate with H2 for tests, Gradle 7.6.4, JUnit 5

**Spec:** `docs/superpowers/specs/2026-04-08-b3b-initial-review-design.md`

**Build/test command:** `cd services/quality-inspection-service && ./gradlew test`

**Base path for all source files:** `services/quality-inspection-service/src`

---

## File Structure

### Files to CREATE

| # | Path | Responsibility |
|---|------|---------------|
| 1 | `main/java/.../domain/reviewcase/submission/AnswerItem.java` | Immutable VO — itemId, answerValue, derivedScore |
| 2 | `test/java/.../domain/reviewcase/submission/AnswerItemTest.java` | VO factory + immutability tests |
| 3 | `test/java/.../domain/reviewcase/submission/ReviewSubmissionTest.java` | createDraft, updateDraft, submit, guards |
| 4 | `main/java/.../application/reviewcase/ScoreCalculationService.java` | Server-side scoring engine (BOOLEAN/FIXED/RANGE) |
| 5 | `main/java/.../application/reviewcase/ScoreResult.java` | Result VO — scoredAnswers + totalScore |
| 6 | `test/java/.../application/reviewcase/ScoreCalculationServiceTest.java` | Scoring rules unit tests |
| 7 | `main/java/.../domain/reviewcase/submission/event/InitialReviewStartedEvent.java` | Domain event |
| 8 | `main/java/.../domain/reviewcase/submission/event/ReviewDraftSavedEvent.java` | Domain event |
| 9 | `main/java/.../domain/reviewcase/submission/event/ReviewSubmissionSubmittedEvent.java` | Domain event |
| 10 | `test/java/.../testfixture/InMemoryReviewSubmissionRepository.java` | Test double for use case tests |
| 11 | `main/java/.../application/reviewcase/StartInitialReviewUseCase.java` | Use case — start review + create submission draft |
| 12 | `test/java/.../application/reviewcase/StartInitialReviewUseCaseTest.java` | Use case tests |
| 13 | `main/java/.../application/reviewcase/SaveInitialReviewDraftUseCase.java` | Use case — save draft with scoring |
| 14 | `test/java/.../application/reviewcase/SaveInitialReviewDraftUseCaseTest.java` | Use case tests |
| 15 | `main/java/.../application/reviewcase/SubmitInitialReviewUseCase.java` | Use case — submit + complete review |
| 16 | `test/java/.../application/reviewcase/SubmitInitialReviewUseCaseTest.java` | Use case tests |
| 17 | `main/java/.../infrastructure/persistence/submission/ReviewSubmissionEntity.java` | JPA entity |
| 18 | `main/java/.../infrastructure/persistence/submission/AnswerSetJsonConverter.java` | JPA converter List\<AnswerItem\> ↔ JSON |
| 19 | `main/java/.../infrastructure/persistence/submission/ReviewSubmissionJpaRepository.java` | Spring Data JPA interface |
| 20 | `main/java/.../infrastructure/persistence/submission/ReviewSubmissionRepositoryImpl.java` | Repository impl — domain ↔ entity mapping |
| 21 | `test/java/.../infrastructure/persistence/submission/ReviewSubmissionPersistenceTest.java` | JPA round-trip + converter tests |
| 22 | `main/java/.../interfaces/rest/dto/StartInitialReviewResponse.java` | Response DTO |
| 23 | `main/java/.../interfaces/rest/dto/SaveInitialReviewDraftRequest.java` | Request DTO with validation |
| 24 | `main/java/.../application/reviewcase/dto/AnswerInput.java` | Application-level answer input DTO (itemId + answerValue) |
| 25 | `main/java/.../interfaces/rest/dto/SaveDraftResponse.java` | Response DTO |
| 26 | `test/java/.../interfaces/rest/InitialReviewApiTest.java` | API integration tests for 3 endpoints |

### Files to MODIFY

| # | Path | Change |
|---|------|--------|
| 1 | `main/java/.../domain/reviewcase/submission/ReviewSubmission.java` | Add answerSet + attachmentRefsJson fields, createInitialDraft(), reconstitute(), updateDraft(), submit() |
| 2 | `main/java/.../domain/reviewcase/ReviewCase.java` | Add startInitialReview() + completeInitialReview() |
| 3 | `test/java/.../domain/reviewcase/ReviewCaseTest.java` | Add tests for new methods |
| 4 | `test/java/.../TestTokenFactory.java` | Add reviewerToken() method |
| 5 | `main/java/.../interfaces/rest/ReviewCaseController.java` | Add 3 new endpoints, inject 3 new use cases |

> **Package root abbreviation:** `...` = `com/tzuchi/his/quality/inspection`

---

## Task 1: AnswerItem Value Object + Tests

**Files:**
- Create: `main/java/com/tzuchi/his/quality/inspection/domain/reviewcase/submission/AnswerItem.java`
- Create: `test/java/com/tzuchi/his/quality/inspection/domain/reviewcase/submission/AnswerItemTest.java`

- [ ] **Step 1: Write the AnswerItem tests**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.submission;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AnswerItemTest {

    @Test
    void ofRaw_shouldCreateWithNullDerivedScore() {
        AnswerItem item = AnswerItem.ofRaw("Q001", "true");

        assertEquals("Q001", item.getItemId());
        assertEquals("true", item.getAnswerValue());
        assertNull(item.getDerivedScore());
    }

    @Test
    void ofScored_shouldCreateWithAllFields() {
        AnswerItem item = AnswerItem.ofScored("Q001", "true", new BigDecimal("10.00"));

        assertEquals("Q001", item.getItemId());
        assertEquals("true", item.getAnswerValue());
        assertEquals(new BigDecimal("10.00"), item.getDerivedScore());
    }

    @Test
    void reconstitute_shouldRestoreAllFields() {
        AnswerItem item = AnswerItem.reconstitute("Q002", "85", new BigDecimal("85.00"));

        assertEquals("Q002", item.getItemId());
        assertEquals("85", item.getAnswerValue());
        assertEquals(new BigDecimal("85.00"), item.getDerivedScore());
    }

    @Test
    void ofRaw_nullItemId_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> AnswerItem.ofRaw(null, "true"));
    }

    @Test
    void ofRaw_nullAnswerValue_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> AnswerItem.ofRaw("Q001", null));
    }

    @Test
    void ofScored_nullDerivedScore_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> AnswerItem.ofScored("Q001", "true", null));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItemTest" 2>&1 | tail -5`
Expected: Compilation error — `AnswerItem` class does not exist yet.

- [ ] **Step 3: Implement AnswerItem**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.submission;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable value object representing a single answer within a review submission.
 * answerValue is always String (unified serialization).
 * derivedScore is computed by ScoreCalculationService.
 */
public class AnswerItem {

    private final String itemId;
    private final String answerValue;
    private final BigDecimal derivedScore;

    private AnswerItem(String itemId, String answerValue, BigDecimal derivedScore) {
        this.itemId = itemId;
        this.answerValue = answerValue;
        this.derivedScore = derivedScore;
    }

    /** Frontend-submitted raw answer — no derived score yet. */
    public static AnswerItem ofRaw(String itemId, String answerValue) {
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(answerValue, "answerValue must not be null");
        return new AnswerItem(itemId, answerValue, null);
    }

    /** Server-scored answer with derived score. */
    public static AnswerItem ofScored(String itemId, String answerValue, BigDecimal derivedScore) {
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(answerValue, "answerValue must not be null");
        Objects.requireNonNull(derivedScore, "derivedScore must not be null");
        return new AnswerItem(itemId, answerValue, derivedScore);
    }

    /** Reconstitute from persistence — no validation. */
    public static AnswerItem reconstitute(String itemId, String answerValue, BigDecimal derivedScore) {
        return new AnswerItem(itemId, answerValue, derivedScore);
    }

    public String getItemId() { return itemId; }
    public String getAnswerValue() { return answerValue; }
    public BigDecimal getDerivedScore() { return derivedScore; }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItemTest" 2>&1 | tail -5`
Expected: All 6 tests PASS.

- [ ] **Step 5: Commit**

```bash
cd services/quality-inspection-service
git add src/main/java/com/tzuchi/his/quality/inspection/domain/reviewcase/submission/AnswerItem.java \
        src/test/java/com/tzuchi/his/quality/inspection/domain/reviewcase/submission/AnswerItemTest.java
git commit -m "feat(b3b): add AnswerItem value object with factory methods and tests

- ofRaw: raw answer from frontend (no derivedScore)
- ofScored: scored answer from ScoreCalculationService
- reconstitute: persistence restoration
- Immutable VO with null guards

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 2: ReviewSubmission Domain Expansion + Tests

**Files:**
- Modify: `main/java/com/tzuchi/his/quality/inspection/domain/reviewcase/submission/ReviewSubmission.java`
- Create: `test/java/com/tzuchi/his/quality/inspection/domain/reviewcase/submission/ReviewSubmissionTest.java`

- [ ] **Step 1: Write the ReviewSubmission tests**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.submission;

import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReviewSubmissionTest {

    // --- createInitialDraft ---

    @Test
    void createInitialDraft_shouldCreateWithCorrectDefaults() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                "sub-1", "c1", "reviewer-1", "ss-v1");

        assertEquals("sub-1", sub.getSubmissionId());
        assertEquals("c1", sub.getCaseId());
        assertEquals(ReviewStage.INITIAL, sub.getStage());
        assertEquals("reviewer-1", sub.getReviewerId());
        assertEquals(SubmissionStatus.DRAFT, sub.getStatus());
        assertEquals(1, sub.getRevisionNo());
        assertEquals(BigDecimal.ZERO, sub.getTotalScore());
        assertTrue(sub.getAnswerSet().isEmpty());
        assertNull(sub.getComment());
        assertNull(sub.getSubmittedAt());
        assertEquals("ss-v1", sub.getBasedOnScoreSheetVersionId());
        assertNotNull(sub.getCreatedAt());
    }

    @Test
    void createInitialDraft_nullSubmissionId_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> ReviewSubmission.createInitialDraft(null, "c1", "r1", "ss-v1"));
    }

    @Test
    void createInitialDraft_nullCaseId_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> ReviewSubmission.createInitialDraft("sub-1", null, "r1", "ss-v1"));
    }

    // --- updateDraft ---

    @Test
    void updateDraft_shouldUpdateAnswersAndScore() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                "sub-1", "c1", "reviewer-1", "ss-v1");

        List<AnswerItem> scored = Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.00")),
                AnswerItem.ofScored("Q002", "85", new BigDecimal("85.00"))
        );
        sub.updateDraft(scored, new BigDecimal("95.00"), "looks good");

        assertEquals(2, sub.getAnswerSet().size());
        assertEquals(new BigDecimal("95.00"), sub.getTotalScore());
        assertEquals("looks good", sub.getComment());
        assertNotNull(sub.getUpdatedAt());
    }

    @Test
    void updateDraft_calledMultipleTimes_shouldOverwrite() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                "sub-1", "c1", "reviewer-1", "ss-v1");

        List<AnswerItem> first = Collections.singletonList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.00")));
        sub.updateDraft(first, new BigDecimal("10.00"), "first draft");

        List<AnswerItem> second = Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.00")),
                AnswerItem.ofScored("Q002", "85", new BigDecimal("85.00"))
        );
        sub.updateDraft(second, new BigDecimal("95.00"), "second draft");

        assertEquals(2, sub.getAnswerSet().size());
        assertEquals(new BigDecimal("95.00"), sub.getTotalScore());
        assertEquals("second draft", sub.getComment());
    }

    @Test
    void updateDraft_afterSubmit_shouldThrow() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                "sub-1", "c1", "reviewer-1", "ss-v1");
        sub.submit();

        List<AnswerItem> answers = Collections.singletonList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.00")));

        assertThrows(DomainStateConflictException.class,
                () -> sub.updateDraft(answers, new BigDecimal("10.00"), "too late"));
    }

    // --- submit ---

    @Test
    void submit_fromDraft_shouldTransitionToSubmitted() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                "sub-1", "c1", "reviewer-1", "ss-v1");

        sub.submit();

        assertEquals(SubmissionStatus.SUBMITTED, sub.getStatus());
        assertNotNull(sub.getSubmittedAt());
    }

    @Test
    void submit_alreadySubmitted_shouldThrow() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                "sub-1", "c1", "reviewer-1", "ss-v1");
        sub.submit();

        assertThrows(DomainStateConflictException.class, sub::submit);
    }

    // --- reconstitute ---

    @Test
    void reconstitute_shouldRestoreAllFields() {
        List<AnswerItem> answers = Collections.singletonList(
                AnswerItem.reconstitute("Q001", "true", new BigDecimal("10.00")));
        LocalDateTime now = LocalDateTime.now();

        ReviewSubmission sub = ReviewSubmission.reconstitute(
                "sub-1", "c1", ReviewStage.INITIAL, "reviewer-1",
                SubmissionStatus.SUBMITTED, 1,
                answers, new BigDecimal("10.00"), "comment", now,
                "ss-v1", null, null, now, now);

        assertEquals("sub-1", sub.getSubmissionId());
        assertEquals(SubmissionStatus.SUBMITTED, sub.getStatus());
        assertEquals(1, sub.getAnswerSet().size());
        assertEquals(new BigDecimal("10.00"), sub.getTotalScore());
        assertEquals("comment", sub.getComment());
        assertEquals(now, sub.getSubmittedAt());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionTest" 2>&1 | tail -5`
Expected: Compilation error — `createInitialDraft`, `updateDraft`, `submit`, `reconstitute`, `getAnswerSet` do not exist.

- [ ] **Step 3: Expand ReviewSubmission with business methods**

Replace the entire `ReviewSubmission.java` with:

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.submission;

import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root for a review submission.
 * B3a: shell with getters only.
 * B3b: full business methods — createDraft, updateDraft, submit.
 */
public class ReviewSubmission {

    private String submissionId;
    private String caseId;
    private ReviewStage stage;
    private String reviewerId;
    private SubmissionStatus status;
    private int revisionNo;
    private List<AnswerItem> answerSet;
    private BigDecimal totalScore;
    private String comment;
    private LocalDateTime submittedAt;
    private String basedOnScoreSheetVersionId;
    private String supersededBySubmissionId;
    private String attachmentRefsJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected ReviewSubmission() {
        // for persistence / reconstitution
    }

    /**
     * Create an initial review draft submission.
     * Called by StartInitialReviewUseCase.
     */
    public static ReviewSubmission createInitialDraft(
            String submissionId, String caseId, String reviewerId,
            String scoreSheetVersionId) {
        Objects.requireNonNull(submissionId, "submissionId must not be null");
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(reviewerId, "reviewerId must not be null");
        Objects.requireNonNull(scoreSheetVersionId, "scoreSheetVersionId must not be null");

        ReviewSubmission sub = new ReviewSubmission();
        sub.submissionId = submissionId;
        sub.caseId = caseId;
        sub.stage = ReviewStage.INITIAL;
        sub.reviewerId = reviewerId;
        sub.status = SubmissionStatus.DRAFT;
        sub.revisionNo = 1;
        sub.answerSet = new ArrayList<>();
        sub.totalScore = BigDecimal.ZERO;
        sub.basedOnScoreSheetVersionId = scoreSheetVersionId;
        sub.createdAt = LocalDateTime.now();
        return sub;
    }

    /**
     * Reconstitute from persistence. No business validation.
     */
    public static ReviewSubmission reconstitute(
            String submissionId, String caseId, ReviewStage stage,
            String reviewerId, SubmissionStatus status, int revisionNo,
            List<AnswerItem> answerSet, BigDecimal totalScore,
            String comment, LocalDateTime submittedAt,
            String basedOnScoreSheetVersionId,
            String supersededBySubmissionId,
            String attachmentRefsJson,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        ReviewSubmission sub = new ReviewSubmission();
        sub.submissionId = submissionId;
        sub.caseId = caseId;
        sub.stage = stage;
        sub.reviewerId = reviewerId;
        sub.status = status;
        sub.revisionNo = revisionNo;
        sub.answerSet = answerSet != null ? new ArrayList<>(answerSet) : new ArrayList<>();
        sub.totalScore = totalScore;
        sub.comment = comment;
        sub.submittedAt = submittedAt;
        sub.basedOnScoreSheetVersionId = basedOnScoreSheetVersionId;
        sub.supersededBySubmissionId = supersededBySubmissionId;
        sub.attachmentRefsJson = attachmentRefsJson;
        sub.createdAt = createdAt;
        sub.updatedAt = updatedAt;
        return sub;
    }

    /**
     * Update draft answers and score. Can be called multiple times while in DRAFT.
     */
    public void updateDraft(List<AnswerItem> scoredAnswers, BigDecimal totalScore, String comment) {
        requireDraft();
        Objects.requireNonNull(scoredAnswers, "scoredAnswers must not be null");
        Objects.requireNonNull(totalScore, "totalScore must not be null");
        this.answerSet = new ArrayList<>(scoredAnswers);
        this.totalScore = totalScore;
        this.comment = comment;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Submit this draft. Transitions DRAFT → SUBMITTED. Irreversible.
     */
    public void submit() {
        requireDraft();
        this.status = SubmissionStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    private void requireDraft() {
        if (this.status != SubmissionStatus.DRAFT) {
            throw new DomainStateConflictException("SUBMISSION_NOT_DRAFT",
                    "Submission must be in DRAFT status. Current: " + this.status);
        }
    }

    // --- Getters ---

    public String getSubmissionId() { return submissionId; }
    public String getCaseId() { return caseId; }
    public ReviewStage getStage() { return stage; }
    public String getReviewerId() { return reviewerId; }
    public SubmissionStatus getStatus() { return status; }
    public int getRevisionNo() { return revisionNo; }
    public List<AnswerItem> getAnswerSet() { return Collections.unmodifiableList(answerSet != null ? answerSet : Collections.emptyList()); }
    public BigDecimal getTotalScore() { return totalScore; }
    public String getComment() { return comment; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public String getBasedOnScoreSheetVersionId() { return basedOnScoreSheetVersionId; }
    public String getSupersededBySubmissionId() { return supersededBySubmissionId; }
    public String getAttachmentRefsJson() { return attachmentRefsJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionTest" 2>&1 | tail -5`
Expected: All 10 tests PASS.

- [ ] **Step 5: Run full test suite**

Run: `cd services/quality-inspection-service && ./gradlew test 2>&1 | tail -5`
Expected: All existing tests still pass (no regressions from removing old B3a shell getters).

- [ ] **Step 6: Commit**

```bash
cd services/quality-inspection-service
git add src/main/java/com/tzuchi/his/quality/inspection/domain/reviewcase/submission/ReviewSubmission.java \
        src/test/java/com/tzuchi/his/quality/inspection/domain/reviewcase/submission/ReviewSubmissionTest.java
git commit -m "feat(b3b): expand ReviewSubmission with createInitialDraft, updateDraft, submit

- createInitialDraft factory: DRAFT status, revisionNo=1, empty answerSet
- updateDraft: update scored answers, totalScore, comment (DRAFT guard)
- submit: DRAFT → SUBMITTED transition, sets submittedAt
- reconstitute: persistence restoration with all fields
- Added answerSet (List<AnswerItem>) and attachmentRefsJson fields

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 3: ReviewCase Domain Methods (startInitialReview + completeInitialReview) + Tests

**Files:**
- Modify: `main/java/com/tzuchi/his/quality/inspection/domain/reviewcase/ReviewCase.java`
- Modify: `test/java/com/tzuchi/his/quality/inspection/domain/reviewcase/ReviewCaseTest.java`

- [ ] **Step 1: Add tests for startInitialReview and completeInitialReview to ReviewCaseTest**

Append the following tests to `ReviewCaseTest.java`, after the existing `reconstitute` test block:

```java
    // --- startInitialReview (B3b) ---

    @Test
    void startInitialReview_fromPendingInitialReview_shouldSucceed() {
        ReviewCase rc = createPendingInitialReview();

        rc.startInitialReview("sub-1", "ss-v1");

        assertEquals(CaseStatus.INITIAL_REVIEW_IN_PROGRESS, rc.getStatus());
        assertEquals("sub-1", rc.getLatestInitialSubmissionId());
        assertEquals("ss-v1", rc.getScoreSheetVersionRef());
    }

    @Test
    void startInitialReview_fromNonPending_shouldThrow() {
        ReviewCase rc = createPendingAssignment(); // SELECTED_PENDING_ASSIGNMENT

        assertThrows(DomainStateConflictException.class,
                () -> rc.startInitialReview("sub-1", "ss-v1"));
    }

    @Test
    void startInitialReview_nullSubmissionId_shouldThrow() {
        ReviewCase rc = createPendingInitialReview();

        assertThrows(NullPointerException.class,
                () -> rc.startInitialReview(null, "ss-v1"));
    }

    @Test
    void startInitialReview_nullScoreSheetVersionRef_shouldThrow() {
        ReviewCase rc = createPendingInitialReview();

        assertThrows(NullPointerException.class,
                () -> rc.startInitialReview("sub-1", null));
    }

    // --- completeInitialReview (B3b) ---

    @Test
    void completeInitialReview_fromInProgress_shouldSucceed() {
        ReviewCase rc = createPendingInitialReview();
        rc.startInitialReview("sub-1", "ss-v1");

        rc.completeInitialReview(new BigDecimal("85.50"));

        assertEquals(CaseStatus.INITIAL_REVIEW_COMPLETED, rc.getStatus());
        assertEquals(new BigDecimal("85.50"), rc.getFinalScore());
    }

    @Test
    void completeInitialReview_fromNonInProgress_shouldThrow() {
        ReviewCase rc = createPendingInitialReview(); // PENDING_INITIAL_REVIEW

        assertThrows(DomainStateConflictException.class,
                () -> rc.completeInitialReview(new BigDecimal("85.50")));
    }
```

Also add `import java.math.BigDecimal;` to the import block of ReviewCaseTest.java.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseTest" 2>&1 | tail -5`
Expected: Compilation error — `startInitialReview` and `completeInitialReview` methods do not exist.

- [ ] **Step 3: Add startInitialReview and completeInitialReview to ReviewCase**

Add the following methods to `ReviewCase.java`, after the `reopenInitialReview()` method (before `forceStatus`):

```java
    // --- B3b Domain Methods ---

    public void startInitialReview(String submissionId, String scoreSheetVersionRef) {
        if (this.status != CaseStatus.PENDING_INITIAL_REVIEW) {
            throw new DomainStateConflictException("CASE_NOT_PENDING_INITIAL_REVIEW",
                    "Case must be in PENDING_INITIAL_REVIEW to start. Current: " + this.status);
        }
        Objects.requireNonNull(submissionId, "submissionId must not be null");
        Objects.requireNonNull(scoreSheetVersionRef, "scoreSheetVersionRef must not be null");
        this.latestInitialSubmissionId = submissionId;
        this.scoreSheetVersionRef = scoreSheetVersionRef;
        this.status = CaseStatus.INITIAL_REVIEW_IN_PROGRESS;
    }

    public void completeInitialReview(BigDecimal totalScore) {
        if (this.status != CaseStatus.INITIAL_REVIEW_IN_PROGRESS) {
            throw new DomainStateConflictException("CASE_NOT_IN_INITIAL_REVIEW",
                    "Case must be in INITIAL_REVIEW_IN_PROGRESS to complete. Current: " + this.status);
        }
        Objects.requireNonNull(totalScore, "totalScore must not be null");
        this.finalScore = totalScore;
        this.status = CaseStatus.INITIAL_REVIEW_COMPLETED;
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseTest" 2>&1 | tail -5`
Expected: All tests PASS (existing + 6 new).

- [ ] **Step 5: Run full test suite**

Run: `cd services/quality-inspection-service && ./gradlew test 2>&1 | tail -5`
Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
cd services/quality-inspection-service
git add src/main/java/com/tzuchi/his/quality/inspection/domain/reviewcase/ReviewCase.java \
        src/test/java/com/tzuchi/his/quality/inspection/domain/reviewcase/ReviewCaseTest.java
git commit -m "feat(b3b): add startInitialReview + completeInitialReview to ReviewCase

- startInitialReview: PENDING_INITIAL_REVIEW → INITIAL_REVIEW_IN_PROGRESS
  Sets latestInitialSubmissionId and scoreSheetVersionRef
- completeInitialReview: INITIAL_REVIEW_IN_PROGRESS → INITIAL_REVIEW_COMPLETED
  Sets finalScore
- State guards with DomainStateConflictException

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 4: ScoreCalculationService + ScoreResult + Tests

**Files:**
- Create: `main/java/com/tzuchi/his/quality/inspection/application/reviewcase/ScoreResult.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/application/reviewcase/ScoreCalculationService.java`
- Create: `test/java/com/tzuchi/his/quality/inspection/application/reviewcase/ScoreCalculationServiceTest.java`

- [ ] **Step 1: Write the ScoreCalculationService tests**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoreCalculationServiceTest {

    private ScoreCalculationService service;

    @BeforeEach
    void setUp() {
        service = new ScoreCalculationService();
    }

    private ScoreSheetDefinition buildScoreSheet(ScoreItem... items) {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss-1", "p1");
        ScoreSheetSection section = new ScoreSheetSection("sec-1", "Section 1");
        for (ScoreItem item : items) {
            section.addItem(item);
        }
        ss.addSection(section);
        ss.publishVersion();
        return ss;
    }

    // --- BOOLEAN ---

    @Test
    void calculate_booleanTrue_shouldReturnMaxScoreTimesWeight() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Check 1", ScoreType.BOOLEAN,
                        new BigDecimal("10"), new BigDecimal("1.0")));

        List<AnswerItem> raw = Collections.singletonList(
                AnswerItem.ofRaw("Q001", "true"));

        ScoreResult result = service.calculate(raw, ss);

        assertEquals(1, result.getScoredAnswers().size());
        assertEquals(0, new BigDecimal("10.0").compareTo(result.getScoredAnswers().get(0).getDerivedScore()));
        assertEquals(0, new BigDecimal("10.0").compareTo(result.getTotalScore()));
    }

    @Test
    void calculate_booleanFalse_shouldReturnZero() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Check 1", ScoreType.BOOLEAN,
                        new BigDecimal("10"), new BigDecimal("1.0")));

        List<AnswerItem> raw = Collections.singletonList(
                AnswerItem.ofRaw("Q001", "false"));

        ScoreResult result = service.calculate(raw, ss);

        assertEquals(0, BigDecimal.ZERO.compareTo(result.getScoredAnswers().get(0).getDerivedScore()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalScore()));
    }

    // --- FIXED ---

    @Test
    void calculate_fixed_shouldReturnMaxScoreTimesWeight() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Fixed Item", ScoreType.FIXED,
                        new BigDecimal("5"), new BigDecimal("2.0")));

        List<AnswerItem> raw = Collections.singletonList(
                AnswerItem.ofRaw("Q001", "anything"));

        ScoreResult result = service.calculate(raw, ss);

        assertEquals(0, new BigDecimal("10.0").compareTo(result.getScoredAnswers().get(0).getDerivedScore()));
        assertEquals(0, new BigDecimal("10.0").compareTo(result.getTotalScore()));
    }

    // --- RANGE ---

    @Test
    void calculate_range_withinMax_shouldReturnValueTimesWeight() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Range Item", ScoreType.RANGE,
                        new BigDecimal("100"), new BigDecimal("1.0")));

        List<AnswerItem> raw = Collections.singletonList(
                AnswerItem.ofRaw("Q001", "85"));

        ScoreResult result = service.calculate(raw, ss);

        assertEquals(0, new BigDecimal("85.0").compareTo(result.getScoredAnswers().get(0).getDerivedScore()));
        assertEquals(0, new BigDecimal("85.0").compareTo(result.getTotalScore()));
    }

    @Test
    void calculate_range_exceedingMax_shouldCapAtMax() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Range Item", ScoreType.RANGE,
                        new BigDecimal("100"), new BigDecimal("1.0")));

        List<AnswerItem> raw = Collections.singletonList(
                AnswerItem.ofRaw("Q001", "150"));

        ScoreResult result = service.calculate(raw, ss);

        assertEquals(0, new BigDecimal("100.0").compareTo(result.getScoredAnswers().get(0).getDerivedScore()));
    }

    @Test
    void calculate_range_withWeight_shouldMultiply() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Range Item", ScoreType.RANGE,
                        new BigDecimal("100"), new BigDecimal("0.5")));

        List<AnswerItem> raw = Collections.singletonList(
                AnswerItem.ofRaw("Q001", "80"));

        ScoreResult result = service.calculate(raw, ss);

        assertEquals(0, new BigDecimal("40.0").compareTo(result.getScoredAnswers().get(0).getDerivedScore()));
    }

    // --- Multiple items ---

    @Test
    void calculate_multipleItems_shouldSumTotalScore() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Boolean", ScoreType.BOOLEAN,
                        new BigDecimal("10"), new BigDecimal("1.0")),
                new ScoreItem("Q002", "Q002", "Range", ScoreType.RANGE,
                        new BigDecimal("100"), new BigDecimal("1.0")));

        List<AnswerItem> raw = Arrays.asList(
                AnswerItem.ofRaw("Q001", "true"),
                AnswerItem.ofRaw("Q002", "75"));

        ScoreResult result = service.calculate(raw, ss);

        assertEquals(2, result.getScoredAnswers().size());
        assertEquals(0, new BigDecimal("85.0").compareTo(result.getTotalScore()));
    }

    // --- Edge cases ---

    @Test
    void calculate_unknownItemId_shouldBeIgnored() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Check", ScoreType.BOOLEAN,
                        new BigDecimal("10"), new BigDecimal("1.0")));

        List<AnswerItem> raw = Arrays.asList(
                AnswerItem.ofRaw("Q001", "true"),
                AnswerItem.ofRaw("Q999", "unknown"));

        ScoreResult result = service.calculate(raw, ss);

        assertEquals(1, result.getScoredAnswers().size());
        assertEquals(0, new BigDecimal("10.0").compareTo(result.getTotalScore()));
    }

    @Test
    void calculate_emptyAnswers_shouldReturnZero() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Check", ScoreType.BOOLEAN,
                        new BigDecimal("10"), new BigDecimal("1.0")));

        ScoreResult result = service.calculate(Collections.emptyList(), ss);

        assertTrue(result.getScoredAnswers().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalScore()));
    }

    @Test
    void calculate_range_negativeValue_shouldThrow() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Range", ScoreType.RANGE,
                        new BigDecimal("100"), new BigDecimal("1.0")));

        List<AnswerItem> raw = Collections.singletonList(
                AnswerItem.ofRaw("Q001", "-5"));

        assertThrows(IllegalArgumentException.class,
                () -> service.calculate(raw, ss));
    }

    @Test
    void calculate_range_nonNumeric_shouldThrow() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Range", ScoreType.RANGE,
                        new BigDecimal("100"), new BigDecimal("1.0")));

        List<AnswerItem> raw = Collections.singletonList(
                AnswerItem.ofRaw("Q001", "abc"));

        assertThrows(IllegalArgumentException.class,
                () -> service.calculate(raw, ss));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.application.reviewcase.ScoreCalculationServiceTest" 2>&1 | tail -5`
Expected: Compilation error — `ScoreCalculationService` and `ScoreResult` do not exist.

- [ ] **Step 3: Implement ScoreResult**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Result of score calculation — scored answers with derived scores and total.
 */
public class ScoreResult {

    private final List<AnswerItem> scoredAnswers;
    private final BigDecimal totalScore;

    public ScoreResult(List<AnswerItem> scoredAnswers, BigDecimal totalScore) {
        this.scoredAnswers = scoredAnswers;
        this.totalScore = totalScore;
    }

    public List<AnswerItem> getScoredAnswers() { return Collections.unmodifiableList(scoredAnswers); }
    public BigDecimal getTotalScore() { return totalScore; }
}
```

- [ ] **Step 4: Implement ScoreCalculationService**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-side scoring engine.
 * Calculates derivedScore for each answer based on ScoreSheetDefinition rules.
 */
@Service
public class ScoreCalculationService {

    /**
     * Calculate scores for raw answers against the score sheet definition.
     *
     * @param rawAnswers  frontend-submitted answers (itemId + answerValue only)
     * @param scoreSheet  the published ScoreSheetDefinition
     * @return ScoreResult with scored answers and total score
     */
    public ScoreResult calculate(List<AnswerItem> rawAnswers, ScoreSheetDefinition scoreSheet) {
        // Build itemId → ScoreItem lookup map from all sections
        Map<String, ScoreItem> itemMap = new HashMap<>();
        for (ScoreSheetSection section : scoreSheet.getSections()) {
            for (ScoreItem item : section.getItems()) {
                itemMap.put(item.getItemId(), item);
            }
        }

        List<AnswerItem> scoredAnswers = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;

        for (AnswerItem raw : rawAnswers) {
            ScoreItem definition = itemMap.get(raw.getItemId());
            if (definition == null) {
                // Unknown itemId — ignore per spec
                continue;
            }

            BigDecimal derivedScore = computeScore(raw, definition);
            scoredAnswers.add(AnswerItem.ofScored(raw.getItemId(), raw.getAnswerValue(), derivedScore));
            totalScore = totalScore.add(derivedScore);
        }

        return new ScoreResult(scoredAnswers, totalScore);
    }

    private BigDecimal computeScore(AnswerItem raw, ScoreItem definition) {
        BigDecimal weight = definition.getWeight();

        switch (definition.getScoreType()) {
            case BOOLEAN:
                return "true".equals(raw.getAnswerValue())
                        ? definition.getMaxScore().multiply(weight)
                        : BigDecimal.ZERO;

            case FIXED:
                return definition.getMaxScore().multiply(weight);

            case RANGE:
                BigDecimal value;
                try {
                    value = new BigDecimal(raw.getAnswerValue());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "RANGE answer for item " + raw.getItemId()
                                    + " must be numeric, got: " + raw.getAnswerValue());
                }
                if (value.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException(
                            "RANGE answer for item " + raw.getItemId()
                                    + " must not be negative, got: " + raw.getAnswerValue());
                }
                BigDecimal capped = value.min(definition.getMaxScore());
                return capped.multiply(weight);

            default:
                throw new IllegalStateException("Unknown ScoreType: " + definition.getScoreType());
        }
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.application.reviewcase.ScoreCalculationServiceTest" 2>&1 | tail -5`
Expected: All 12 tests PASS.

- [ ] **Step 6: Commit**

```bash
cd services/quality-inspection-service
git add src/main/java/com/tzuchi/his/quality/inspection/application/reviewcase/ScoreResult.java \
        src/main/java/com/tzuchi/his/quality/inspection/application/reviewcase/ScoreCalculationService.java \
        src/test/java/com/tzuchi/his/quality/inspection/application/reviewcase/ScoreCalculationServiceTest.java
git commit -m "feat(b3b): add ScoreCalculationService with BOOLEAN/FIXED/RANGE scoring

- BOOLEAN: true → maxScore × weight, else 0
- FIXED: maxScore × weight (always)
- RANGE: min(value, maxScore) × weight, validates numeric + non-negative
- Unknown itemIds ignored, totalScore = Σ(derivedScore)
- ScoreResult VO: scoredAnswers + totalScore

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 5: Domain Events + InMemoryReviewSubmissionRepository Test Fixture

**Files:**
- Create: `main/java/com/tzuchi/his/quality/inspection/domain/reviewcase/submission/event/InitialReviewStartedEvent.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/domain/reviewcase/submission/event/ReviewDraftSavedEvent.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/domain/reviewcase/submission/event/ReviewSubmissionSubmittedEvent.java`
- Create: `test/java/com/tzuchi/his/quality/inspection/testfixture/InMemoryReviewSubmissionRepository.java`

- [ ] **Step 1: Create InitialReviewStartedEvent**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

/**
 * Published when a reviewer starts the initial review on a case.
 */
public class InitialReviewStartedEvent extends DomainEvent {

    public InitialReviewStartedEvent(Object source, String actorUserId, String actorUserName,
                                      String hospitalId, String caseId,
                                      String reviewerId, String submissionId) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewCase", caseId, "INITIAL_REVIEW_STARTED",
              String.format("{\"reviewerId\":\"%s\",\"submissionId\":\"%s\"}", reviewerId, submissionId));
    }
}
```

- [ ] **Step 2: Create ReviewDraftSavedEvent**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.math.BigDecimal;

/**
 * Published when a reviewer saves a draft of the initial review.
 */
public class ReviewDraftSavedEvent extends DomainEvent {

    public ReviewDraftSavedEvent(Object source, String actorUserId, String actorUserName,
                                  String hospitalId, String caseId,
                                  String submissionId, BigDecimal totalScore) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewSubmission", submissionId, "DRAFT_SAVED",
              String.format("{\"caseId\":\"%s\",\"totalScore\":%s}", caseId, totalScore));
    }
}
```

- [ ] **Step 3: Create ReviewSubmissionSubmittedEvent**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.math.BigDecimal;

/**
 * Published when a reviewer submits the initial review.
 */
public class ReviewSubmissionSubmittedEvent extends DomainEvent {

    public ReviewSubmissionSubmittedEvent(Object source, String actorUserId, String actorUserName,
                                           String hospitalId, String caseId,
                                           String submissionId, BigDecimal totalScore,
                                           String reviewerId) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewSubmission", submissionId, "SUBMITTED",
              String.format("{\"caseId\":\"%s\",\"totalScore\":%s,\"reviewerId\":\"%s\"}",
                      caseId, totalScore, reviewerId));
    }
}
```

- [ ] **Step 4: Create InMemoryReviewSubmissionRepository**

```java
package com.tzuchi.his.quality.inspection.testfixture;

import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryReviewSubmissionRepository implements ReviewSubmissionRepository {
    public final Map<String, ReviewSubmission> store = new LinkedHashMap<>();

    @Override
    public void save(ReviewSubmission submission) {
        store.put(submission.getSubmissionId(), submission);
    }

    @Override
    public Optional<ReviewSubmission> findById(String submissionId) {
        return Optional.ofNullable(store.get(submissionId));
    }

    @Override
    public List<ReviewSubmission> findByCaseIdAndStage(String caseId, ReviewStage stage) {
        return store.values().stream()
                .filter(s -> s.getCaseId().equals(caseId) && s.getStage() == stage)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ReviewSubmission> findLatestByCaseIdAndStage(String caseId, ReviewStage stage) {
        List<ReviewSubmission> matching = findByCaseIdAndStage(caseId, stage);
        return matching.isEmpty() ? Optional.empty() : Optional.of(matching.get(matching.size() - 1));
    }
}
```

- [ ] **Step 5: Run full test suite to verify no regressions**

Run: `cd services/quality-inspection-service && ./gradlew test 2>&1 | tail -5`
Expected: All existing tests pass. New files compile without errors.

- [ ] **Step 6: Commit**

```bash
cd services/quality-inspection-service
git add src/main/java/com/tzuchi/his/quality/inspection/domain/reviewcase/submission/event/ \
        src/test/java/com/tzuchi/his/quality/inspection/testfixture/InMemoryReviewSubmissionRepository.java
git commit -m "feat(b3b): add 3 domain events + InMemoryReviewSubmissionRepository fixture

Events:
- InitialReviewStartedEvent (caseId, reviewerId, submissionId)
- ReviewDraftSavedEvent (caseId, submissionId, totalScore)
- ReviewSubmissionSubmittedEvent (caseId, submissionId, totalScore, reviewerId)

Test fixture:
- InMemoryReviewSubmissionRepository (implements all 4 repo methods)

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 6: StartInitialReviewUseCase + Tests

**Files:**
- Create: `main/java/com/tzuchi/his/quality/inspection/application/reviewcase/StartInitialReviewUseCase.java`
- Create: `test/java/com/tzuchi/his/quality/inspection/application/reviewcase/StartInitialReviewUseCaseTest.java`

- [ ] **Step 1: Write the StartInitialReviewUseCase tests**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.InitialReviewStartedEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreType;
import com.tzuchi.his.quality.inspection.testfixture.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StartInitialReviewUseCaseTest {

    private InMemoryReviewCaseRepository caseRepo;
    private InMemoryReviewProjectRepository projectRepo;
    private InMemoryScoreSheetRepository scoreSheetRepo;
    private InMemoryReviewSubmissionRepository submissionRepo;
    private CapturingDomainEventPublisher eventPublisher;
    private StartInitialReviewUseCase useCase;

    private static final String CASE_ID = "c1";
    private static final String PROJECT_ID = "p1";
    private static final String HOSPITAL_ID = "H001";
    private static final String REVIEWER_ID = "reviewer-1";
    private static final String SCORE_SHEET_ID = "ss-1";

    @BeforeEach
    void setUp() {
        caseRepo = new InMemoryReviewCaseRepository();
        projectRepo = new InMemoryReviewProjectRepository();
        scoreSheetRepo = new InMemoryScoreSheetRepository();
        submissionRepo = new InMemoryReviewSubmissionRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new StartInitialReviewUseCase(
                caseRepo, projectRepo, scoreSheetRepo, submissionRepo, eventPublisher);
    }

    private ActorContext reviewer() {
        return new ActorContext(REVIEWER_ID, "Test Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));
    }

    private ReviewCase seedAssignedCase() {
        ReviewCase rc = ReviewCase.createFromSampling(
                CASE_ID, "b1", PROJECT_ID, HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer(REVIEWER_ID, LocalDateTime.of(2026, 2, 1, 23, 59));
        caseRepo.save(rc);
        return rc;
    }

    private void seedProjectWithScoreSheet() {
        // Create and publish a score sheet
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft(SCORE_SHEET_ID, PROJECT_ID);
        ScoreSheetSection section = new ScoreSheetSection("sec-1", "Section 1");
        section.addItem(new ScoreItem("Q001", "Q001", "Check", ScoreType.BOOLEAN,
                new BigDecimal("10"), new BigDecimal("1.0")));
        ss.addSection(section);
        ss.publishVersion();
        scoreSheetRepo.save(ss);

        // Create a project bound to the score sheet
        ReviewProject project = ReviewProject.createDraft(PROJECT_ID, HOSPITAL_ID, "Test Project");
        project.bindScoreSheet(SCORE_SHEET_ID);
        project.publishNewVersion();
        project.activate();
        projectRepo.save(project);
    }

    @Test
    void execute_happyPath_shouldCreateSubmissionAndTransitionCase() {
        seedAssignedCase();
        seedProjectWithScoreSheet();

        String submissionId = useCase.execute(reviewer(), CASE_ID);

        assertNotNull(submissionId);

        // Case should be IN_PROGRESS
        ReviewCase savedCase = caseRepo.store.get(CASE_ID);
        assertEquals(CaseStatus.INITIAL_REVIEW_IN_PROGRESS, savedCase.getStatus());
        assertEquals(submissionId, savedCase.getLatestInitialSubmissionId());
        assertEquals(SCORE_SHEET_ID, savedCase.getScoreSheetVersionRef());

        // Submission should exist as DRAFT
        ReviewSubmission savedSub = submissionRepo.store.get(submissionId);
        assertNotNull(savedSub);
        assertEquals(SubmissionStatus.DRAFT, savedSub.getStatus());
        assertEquals(ReviewStage.INITIAL, savedSub.getStage());
        assertEquals(REVIEWER_ID, savedSub.getReviewerId());
        assertEquals(SCORE_SHEET_ID, savedSub.getBasedOnScoreSheetVersionId());
    }

    @Test
    void execute_shouldPublishEvent() {
        seedAssignedCase();
        seedProjectWithScoreSheet();

        useCase.execute(reviewer(), CASE_ID);

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(InitialReviewStartedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext manager = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of(HOSPITAL_ID));

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(manager, CASE_ID));
    }

    @Test
    void execute_notAssignedReviewer_shouldThrow() {
        seedAssignedCase();
        seedProjectWithScoreSheet();

        ActorContext otherReviewer = new ActorContext("other-reviewer", "Other",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(otherReviewer, CASE_ID));
    }

    @Test
    void execute_caseNotFound_shouldThrow() {
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(reviewer(), "no-such"));
    }

    @Test
    void execute_caseNotPendingInitialReview_shouldThrow() {
        ReviewCase rc = ReviewCase.createFromSampling(
                CASE_ID, "b1", PROJECT_ID, HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
        // Still in SELECTED_PENDING_ASSIGNMENT — not assigned yet
        caseRepo.save(rc);
        seedProjectWithScoreSheet();

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(reviewer(), CASE_ID));
    }

    @Test
    void execute_scoreSheetNotPublished_shouldThrow() {
        seedAssignedCase();

        // Create an unpublished score sheet
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft(SCORE_SHEET_ID, PROJECT_ID);
        ScoreSheetSection section = new ScoreSheetSection("sec-1", "Section 1");
        section.addItem(new ScoreItem("Q001", "Q001", "Check", ScoreType.BOOLEAN,
                new BigDecimal("10"), new BigDecimal("1.0")));
        ss.addSection(section);
        // NOT published
        scoreSheetRepo.save(ss);

        ReviewProject project = ReviewProject.createDraft(PROJECT_ID, HOSPITAL_ID, "Test Project");
        project.bindScoreSheet(SCORE_SHEET_ID);
        project.publishNewVersion();
        project.activate();
        projectRepo.save(project);

        assertThrows(DomainRuleViolationException.class,
                () -> useCase.execute(reviewer(), CASE_ID));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.application.reviewcase.StartInitialReviewUseCaseTest" 2>&1 | tail -5`
Expected: Compilation error — `StartInitialReviewUseCase` does not exist.

- [ ] **Step 3: Implement StartInitialReviewUseCase**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.InitialReviewStartedEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class StartInitialReviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewProjectRepository projectRepository;
    private final ScoreSheetRepository scoreSheetRepository;
    private final ReviewSubmissionRepository submissionRepository;
    private final DomainEventPublisher eventPublisher;

    public StartInitialReviewUseCase(ReviewCaseRepository caseRepository,
                                      ReviewProjectRepository projectRepository,
                                      ScoreSheetRepository scoreSheetRepository,
                                      ReviewSubmissionRepository submissionRepository,
                                      DomainEventPublisher eventPublisher) {
        this.caseRepository = caseRepository;
        this.projectRepository = projectRepository;
        this.scoreSheetRepository = scoreSheetRepository;
        this.submissionRepository = submissionRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Start initial review: create a draft submission and transition the case.
     *
     * @return the new submission ID
     */
    public String execute(ActorContext actor, String caseId) {
        actor.requireRole(DomainRole.REVIEWER);

        ReviewCase reviewCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND",
                        "ReviewCase not found: " + caseId));

        // Identity guard: only the assigned reviewer can start
        if (!actor.getUserId().equals(reviewCase.getReviewerAssignment().getReviewerId())) {
            throw new AccessDeniedException("NOT_ASSIGNED_REVIEWER",
                    "Only the assigned reviewer can start the initial review");
        }

        // Load project to get bound score sheet ID
        ReviewProject project = projectRepository.findById(reviewCase.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "ReviewProject not found: " + reviewCase.getProjectId()));

        String boundScoreSheetId = project.getBoundScoreSheetId();
        if (boundScoreSheetId == null) {
            throw new DomainRuleViolationException("NO_BOUND_SCORE_SHEET",
                    "Project has no bound score sheet");
        }

        // Load and validate score sheet is published
        ScoreSheetDefinition scoreSheet = scoreSheetRepository.findById(boundScoreSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                        "ScoreSheetDefinition not found: " + boundScoreSheetId));

        if (scoreSheet.getStatus() != ScoreSheetStatus.PUBLISHED) {
            throw new DomainRuleViolationException("SCORE_SHEET_NOT_PUBLISHED",
                    "Score sheet must be PUBLISHED to start review");
        }

        // Create draft submission
        String submissionId = UUID.randomUUID().toString();
        ReviewSubmission submission = ReviewSubmission.createInitialDraft(
                submissionId, caseId, actor.getUserId(), boundScoreSheetId);

        // Transition case
        reviewCase.startInitialReview(submissionId, boundScoreSheetId);

        // Persist
        submissionRepository.save(submission);
        caseRepository.save(reviewCase);

        // Publish event
        eventPublisher.publish(new InitialReviewStartedEvent(
                this, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId,
                actor.getUserId(), submissionId));

        return submissionId;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.application.reviewcase.StartInitialReviewUseCaseTest" 2>&1 | tail -5`
Expected: All 7 tests PASS.

- [ ] **Step 5: Run full test suite**

Run: `cd services/quality-inspection-service && ./gradlew test 2>&1 | tail -5`
Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
cd services/quality-inspection-service
git add src/main/java/com/tzuchi/his/quality/inspection/application/reviewcase/StartInitialReviewUseCase.java \
        src/test/java/com/tzuchi/his/quality/inspection/application/reviewcase/StartInitialReviewUseCaseTest.java
git commit -m "feat(b3b): add StartInitialReviewUseCase

- Role guard: REVIEWER only
- Identity guard: must be the assigned reviewer
- Loads project → boundScoreSheetId → validates PUBLISHED
- Creates ReviewSubmission.createInitialDraft
- Transitions case PENDING_INITIAL_REVIEW → INITIAL_REVIEW_IN_PROGRESS
- Publishes InitialReviewStartedEvent

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 7: SaveInitialReviewDraftUseCase + Tests

**Files:**
- Create: `main/java/com/tzuchi/his/quality/inspection/application/reviewcase/SaveInitialReviewDraftUseCase.java`
- Create: `test/java/com/tzuchi/his/quality/inspection/application/reviewcase/SaveInitialReviewDraftUseCaseTest.java`

- [ ] **Step 1: Write the SaveInitialReviewDraftUseCase tests**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.application.scoresheet.ScoreCalculationService;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.ReviewDraftSavedEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreType;
import com.tzuchi.his.quality.inspection.testfixture.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SaveInitialReviewDraftUseCaseTest {

    private InMemoryReviewSubmissionRepository submissionRepo;
    private InMemoryScoreSheetRepository scoreSheetRepo;
    private CapturingDomainEventPublisher eventPublisher;
    private SaveInitialReviewDraftUseCase useCase;

    private static final String SUBMISSION_ID = "sub-1";
    private static final String CASE_ID = "c1";
    private static final String REVIEWER_ID = "reviewer-1";
    private static final String HOSPITAL_ID = "H001";
    private static final String SCORE_SHEET_ID = "ss-1";

    @BeforeEach
    void setUp() {
        submissionRepo = new InMemoryReviewSubmissionRepository();
        scoreSheetRepo = new InMemoryScoreSheetRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        ScoreCalculationService scoreCalcService = new ScoreCalculationService();
        useCase = new SaveInitialReviewDraftUseCase(
                submissionRepo, scoreSheetRepo, eventPublisher, scoreCalcService);
    }

    private ActorContext reviewer() {
        return new ActorContext(REVIEWER_ID, "Test Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));
    }

    private ScoreSheetDefinition seedPublishedScoreSheet() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft(SCORE_SHEET_ID, "p1");
        ScoreSheetSection section = new ScoreSheetSection("sec-1", "Section 1");
        section.addItem(new ScoreItem("Q001", "Q001", "Bool Question", ScoreType.BOOLEAN,
                new BigDecimal("10"), new BigDecimal("1.0")));
        section.addItem(new ScoreItem("Q002", "Q002", "Range Question", ScoreType.RANGE,
                new BigDecimal("20"), new BigDecimal("0.5")));
        ss.addSection(section);
        ss.publishVersion();
        scoreSheetRepo.save(ss);
        return ss;
    }

    private ReviewSubmission seedDraftSubmission() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        submissionRepo.save(sub);
        return sub;
    }

    @Test
    void execute_happyPath_shouldUpdateDraftAndReturnScore() {
        seedPublishedScoreSheet();
        seedDraftSubmission();

        List<AnswerInput> answers = Arrays.asList(
                new AnswerInput("Q001", "true"),
                new AnswerInput("Q002", "15")
        );

        BigDecimal totalScore = useCase.execute(reviewer(), CASE_ID, answers, "draft comment");

        // Score: Q001 = 10*1.0 = 10, Q002 = min(15,20)*0.5 = 7.5 -> total = 17.5
        assertEquals(new BigDecimal("17.5"), totalScore);

        ReviewSubmission saved = submissionRepo.store.get(SUBMISSION_ID);
        assertEquals(SubmissionStatus.DRAFT, saved.getStatus());
        assertEquals(2, saved.getAnswerSet().size());
        assertEquals("draft comment", saved.getComment());

        // Each answer should have derived score
        AnswerItem q001 = saved.getAnswerSet().stream()
                .filter(a -> a.getItemId().equals("Q001")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("10.0"), q001.getDerivedScore());
    }

    @Test
    void execute_shouldPublishEvent() {
        seedPublishedScoreSheet();
        seedDraftSubmission();

        List<AnswerInput> answers = Collections.singletonList(new AnswerInput("Q001", "true"));

        useCase.execute(reviewer(), CASE_ID, answers, null);

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewDraftSavedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext manager = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of(HOSPITAL_ID));

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(manager, CASE_ID, Collections.emptyList(), null));
    }

    @Test
    void execute_notOwningReviewer_shouldThrow() {
        seedPublishedScoreSheet();
        seedDraftSubmission();

        ActorContext other = new ActorContext("other-reviewer", "Other",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(other, CASE_ID,
                        Collections.singletonList(new AnswerInput("Q001", "true")), null));
    }

    @Test
    void execute_submissionNotFound_shouldThrow() {
        // No submission seeded for CASE_ID
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(reviewer(), CASE_ID, Collections.emptyList(), null));
    }

    @Test
    void execute_submissionNotDraft_shouldThrow() {
        seedPublishedScoreSheet();
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        sub.updateDraft(Collections.emptyList(), BigDecimal.ZERO, null);
        sub.submit();
        submissionRepo.save(sub);

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(reviewer(), CASE_ID,
                        Collections.singletonList(new AnswerInput("Q001", "true")), null));
    }

    @Test
    void execute_emptyAnswers_shouldReturnZeroScore() {
        seedPublishedScoreSheet();
        seedDraftSubmission();

        BigDecimal totalScore = useCase.execute(reviewer(), CASE_ID, Collections.emptyList(), null);

        assertEquals(BigDecimal.ZERO, totalScore);
    }
}
```

- [ ] **Step 2: Create AnswerInput DTO**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase.dto;

public class AnswerInput {
    private final String itemId;
    private final String answerValue;

    public AnswerInput(String itemId, String answerValue) {
        this.itemId = itemId;
        this.answerValue = answerValue;
    }

    public String getItemId() {
        return itemId;
    }

    public String getAnswerValue() {
        return answerValue;
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.application.reviewcase.SaveInitialReviewDraftUseCaseTest" 2>&1 | tail -5`
Expected: Compilation error — `SaveInitialReviewDraftUseCase` does not exist.

- [ ] **Step 4: Implement SaveInitialReviewDraftUseCase**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.application.scoresheet.ScoreCalculationService;
import com.tzuchi.his.quality.inspection.application.scoresheet.ScoreResult;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.ReviewDraftSavedEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SaveInitialReviewDraftUseCase {

    private final ReviewSubmissionRepository submissionRepository;
    private final ScoreSheetRepository scoreSheetRepository;
    private final DomainEventPublisher eventPublisher;
    private final ScoreCalculationService scoreCalculationService;

    public SaveInitialReviewDraftUseCase(ReviewSubmissionRepository submissionRepository,
                                          ScoreSheetRepository scoreSheetRepository,
                                          DomainEventPublisher eventPublisher,
                                          ScoreCalculationService scoreCalculationService) {
        this.submissionRepository = submissionRepository;
        this.scoreSheetRepository = scoreSheetRepository;
        this.eventPublisher = eventPublisher;
        this.scoreCalculationService = scoreCalculationService;
    }

    /**
     * Save draft answers, calculate scores, return totalScore.
     *
     * @param caseId the review case ID (submission is found via findLatest)
     * @param answers the answer inputs from the reviewer
     * @param comment optional reviewer comment
     * @return the calculated total score
     */
    public BigDecimal execute(ActorContext actor, String caseId,
                               List<AnswerInput> answers, String comment) {
        actor.requireRole(DomainRole.REVIEWER);

        ReviewSubmission submission = submissionRepository
                .findLatestByCaseIdAndStage(caseId, ReviewStage.INITIAL)
                .orElseThrow(() -> new ResourceNotFoundException("SUBMISSION_NOT_FOUND",
                        "No INITIAL submission found for case: " + caseId));

        // Identity guard: only the submission owner
        if (!actor.getUserId().equals(submission.getReviewerId())) {
            throw new AccessDeniedException("NOT_SUBMISSION_OWNER",
                    "Only the reviewer who owns this submission can save a draft");
        }

        // If no answers, update with empty + zero
        if (answers == null || answers.isEmpty()) {
            submission.updateDraft(Collections.emptyList(), BigDecimal.ZERO, comment);
            submissionRepository.save(submission);

            eventPublisher.publish(new ReviewDraftSavedEvent(
                    this, actor.getUserId(), actor.getUserName(),
                    "", caseId, submission.getSubmissionId(), BigDecimal.ZERO));

            return BigDecimal.ZERO;
        }

        // Convert AnswerInput -> raw AnswerItem (no score yet)
        List<AnswerItem> rawItems = answers.stream()
                .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                .collect(Collectors.toList());

        // Load score sheet for scoring
        ScoreSheetDefinition scoreSheet = scoreSheetRepository
                .findById(submission.getBasedOnScoreSheetVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                        "ScoreSheetDefinition not found: " + submission.getBasedOnScoreSheetVersionId()));

        // Calculate scores
        ScoreResult result = scoreCalculationService.calculate(rawItems, scoreSheet);

        // Update draft
        submission.updateDraft(result.getScoredItems(), result.getTotalScore(), comment);
        submissionRepository.save(submission);

        // Publish event
        eventPublisher.publish(new ReviewDraftSavedEvent(
                this, actor.getUserId(), actor.getUserName(),
                "", caseId, submission.getSubmissionId(), result.getTotalScore()));

        return result.getTotalScore();
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.application.reviewcase.SaveInitialReviewDraftUseCaseTest" 2>&1 | tail -5`
Expected: All 7 tests PASS.

- [ ] **Step 6: Run full test suite**

Run: `cd services/quality-inspection-service && ./gradlew test 2>&1 | tail -5`
Expected: All tests pass.

- [ ] **Step 7: Commit**

```bash
cd services/quality-inspection-service
git add src/main/java/com/tzuchi/his/quality/inspection/application/reviewcase/SaveInitialReviewDraftUseCase.java \
        src/main/java/com/tzuchi/his/quality/inspection/application/reviewcase/dto/AnswerInput.java \
        src/test/java/com/tzuchi/his/quality/inspection/application/reviewcase/SaveInitialReviewDraftUseCaseTest.java
git commit -m "feat(b3b): add SaveInitialReviewDraftUseCase

- Loads submission via findLatestByCaseIdAndStage(caseId, INITIAL)
- Role + identity guard (must be submission owner reviewer)
- Converts AnswerInput -> raw AnswerItem -> scored via ScoreCalculationService
- Updates submission draft with scored items, totalScore, comment
- Publishes ReviewDraftSavedEvent

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 8: SubmitInitialReviewUseCase + Tests

**Files:**
- Create: `main/java/com/tzuchi/his/quality/inspection/application/reviewcase/SubmitInitialReviewUseCase.java`
- Create: `test/java/com/tzuchi/his/quality/inspection/application/reviewcase/SubmitInitialReviewUseCaseTest.java`

- [ ] **Step 1: Write the SubmitInitialReviewUseCase tests**

The submit endpoint takes NO request body. It loads the existing draft's answerSet, validates
required items, recalculates final scores, and submits.

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.application.scoresheet.ScoreCalculationService;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.ReviewSubmissionSubmittedEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.RequiredRule;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreType;
import com.tzuchi.his.quality.inspection.testfixture.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SubmitInitialReviewUseCaseTest {

    private InMemoryReviewCaseRepository caseRepo;
    private InMemoryReviewSubmissionRepository submissionRepo;
    private InMemoryScoreSheetRepository scoreSheetRepo;
    private CapturingDomainEventPublisher eventPublisher;
    private SubmitInitialReviewUseCase useCase;

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
        useCase = new SubmitInitialReviewUseCase(
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
        ss.addSection(section);
        ss.publishVersion();
        scoreSheetRepo.save(ss);
        return ss;
    }

    private ReviewCase seedCaseInProgress() {
        ReviewCase rc = ReviewCase.createFromSampling(
                CASE_ID, "b1", PROJECT_ID, HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer(REVIEWER_ID, LocalDateTime.of(2026, 2, 1, 23, 59));
        rc.startInitialReview(SUBMISSION_ID, SCORE_SHEET_ID);
        caseRepo.save(rc);
        return rc;
    }

    private ReviewSubmission seedDraftWithAnswers() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        List<AnswerItem> answers = Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.0")),
                AnswerItem.ofScored("Q002", "15", new BigDecimal("7.5"))
        );
        sub.updateDraft(answers, new BigDecimal("17.5"), "draft comment");
        submissionRepo.save(sub);
        return sub;
    }

    @Test
    void execute_happyPath_shouldSubmitAndCompleteCase() {
        seedScoreSheet();
        seedCaseInProgress();
        seedDraftWithAnswers();

        useCase.execute(reviewer(), CASE_ID);

        // Submission should be SUBMITTED
        ReviewSubmission saved = submissionRepo.store.get(SUBMISSION_ID);
        assertEquals(SubmissionStatus.SUBMITTED, saved.getStatus());

        // Case should be INITIAL_REVIEW_COMPLETED
        ReviewCase savedCase = caseRepo.store.get(CASE_ID);
        assertEquals(CaseStatus.INITIAL_REVIEW_COMPLETED, savedCase.getStatus());

        // finalScore = Q001:10*1.0 + Q002:min(15,20)*0.5 = 17.5
        assertEquals(new BigDecimal("17.5"), savedCase.getFinalScore());
    }

    @Test
    void execute_shouldPublishEvent() {
        seedScoreSheet();
        seedCaseInProgress();
        seedDraftWithAnswers();

        useCase.execute(reviewer(), CASE_ID);

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewSubmissionSubmittedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext manager = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of(HOSPITAL_ID));

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(manager, CASE_ID));
    }

    @Test
    void execute_notOwningReviewer_shouldThrow() {
        seedScoreSheet();
        seedCaseInProgress();
        seedDraftWithAnswers();

        ActorContext other = new ActorContext("other-reviewer", "Other",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(other, CASE_ID));
    }

    @Test
    void execute_submissionNotFound_shouldThrow() {
        // No submission seeded
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(reviewer(), CASE_ID));
    }

    @Test
    void execute_submissionAlreadySubmitted_shouldThrow() {
        seedScoreSheet();
        seedCaseInProgress();
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        sub.updateDraft(Collections.emptyList(), BigDecimal.ZERO, null);
        sub.submit();
        submissionRepo.save(sub);

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(reviewer(), CASE_ID));
    }

    @Test
    void execute_missingRequiredItems_shouldThrow() {
        seedScoreSheet();
        seedCaseInProgress();

        // Save draft with only Q002 answered -- Q001 (ALWAYS_REQUIRED) is missing
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        List<AnswerItem> partialAnswers = Collections.singletonList(
                AnswerItem.ofScored("Q002", "15", new BigDecimal("7.5")));
        sub.updateDraft(partialAnswers, new BigDecimal("7.5"), null);
        submissionRepo.save(sub);

        assertThrows(DomainRuleViolationException.class,
                () -> useCase.execute(reviewer(), CASE_ID));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.application.reviewcase.SubmitInitialReviewUseCaseTest" 2>&1 | tail -5`
Expected: Compilation error — `SubmitInitialReviewUseCase` does not exist.

- [ ] **Step 3: Implement SubmitInitialReviewUseCase**

The submit use case takes NO answers — it uses the existing draft's answerSet.

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.scoresheet.ScoreCalculationService;
import com.tzuchi.his.quality.inspection.application.scoresheet.ScoreResult;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.ReviewSubmissionSubmittedEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.RequiredRule;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class SubmitInitialReviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewSubmissionRepository submissionRepository;
    private final ScoreSheetRepository scoreSheetRepository;
    private final DomainEventPublisher eventPublisher;
    private final ScoreCalculationService scoreCalculationService;

    public SubmitInitialReviewUseCase(ReviewCaseRepository caseRepository,
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

    /**
     * Submit the initial review using the existing draft answers.
     * No request body -- uses answerSet already saved in the draft.
     */
    public void execute(ActorContext actor, String caseId) {
        actor.requireRole(DomainRole.REVIEWER);

        ReviewSubmission submission = submissionRepository
                .findLatestByCaseIdAndStage(caseId, ReviewStage.INITIAL)
                .orElseThrow(() -> new ResourceNotFoundException("SUBMISSION_NOT_FOUND",
                        "No INITIAL submission found for case: " + caseId));

        // Identity guard
        if (!actor.getUserId().equals(submission.getReviewerId())) {
            throw new AccessDeniedException("NOT_SUBMISSION_OWNER",
                    "Only the reviewer who owns this submission can submit");
        }

        // Load score sheet for validation + final scoring
        ScoreSheetDefinition scoreSheet = scoreSheetRepository
                .findById(submission.getBasedOnScoreSheetVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                        "ScoreSheetDefinition not found: " + submission.getBasedOnScoreSheetVersionId()));

        // Get current answers from the draft
        List<AnswerItem> currentAnswers = submission.getAnswerSet();

        // Validate required items are answered
        Set<String> answeredItemIds = currentAnswers.stream()
                .map(AnswerItem::getItemId)
                .collect(Collectors.toSet());

        List<String> missingRequired = scoreSheet.getSections().stream()
                .flatMap(s -> s.getItems().stream())
                .filter(item -> item.getRequiredRule() == RequiredRule.ALWAYS_REQUIRED)
                .map(ScoreItem::getItemId)
                .filter(id -> !answeredItemIds.contains(id))
                .collect(Collectors.toList());

        if (!missingRequired.isEmpty()) {
            throw new DomainRuleViolationException("MISSING_REQUIRED_ITEMS",
                    "Required items not answered: " + String.join(", ", missingRequired));
        }

        // Recalculate final scores from existing answers (as raw items)
        List<AnswerItem> rawItems = currentAnswers.stream()
                .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                .collect(Collectors.toList());

        ScoreResult result = scoreCalculationService.calculate(rawItems, scoreSheet);

        // Update draft with final scores, then submit
        submission.updateDraft(result.getScoredItems(), result.getTotalScore(), submission.getComment());
        submission.submit();
        submissionRepository.save(submission);

        // Complete review case
        ReviewCase reviewCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND",
                        "ReviewCase not found: " + caseId));
        reviewCase.completeInitialReview(result.getTotalScore());
        caseRepository.save(reviewCase);

        // Publish event
        eventPublisher.publish(new ReviewSubmissionSubmittedEvent(
                this, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId,
                submission.getSubmissionId(), result.getTotalScore(), actor.getUserId()));
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.application.reviewcase.SubmitInitialReviewUseCaseTest" 2>&1 | tail -5`
Expected: All 7 tests PASS.

- [ ] **Step 5: Run full test suite**

Run: `cd services/quality-inspection-service && ./gradlew test 2>&1 | tail -5`
Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
cd services/quality-inspection-service
git add src/main/java/com/tzuchi/his/quality/inspection/application/reviewcase/SubmitInitialReviewUseCase.java \
        src/test/java/com/tzuchi/his/quality/inspection/application/reviewcase/SubmitInitialReviewUseCaseTest.java
git commit -m "feat(b3b): add SubmitInitialReviewUseCase

- Loads submission via findLatestByCaseIdAndStage (no request body)
- Role + identity guard
- Validates all ALWAYS_REQUIRED score items are answered
- Recalculates final scores from existing draft answerSet
- Submits ReviewSubmission (DRAFT -> SUBMITTED)
- Completes ReviewCase (IN_PROGRESS -> INITIAL_REVIEW_COMPLETED)
- Sets finalScore on ReviewCase
- Publishes ReviewSubmissionSubmittedEvent

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---
## Task 9: JPA Persistence — ReviewSubmissionEntity + AnswerSetJsonConverter + RepositoryImpl + Tests

**Files:**
- Create: `main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/submission/ReviewSubmissionEntity.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/submission/AnswerSetJsonConverter.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/submission/ReviewSubmissionJpaRepository.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/submission/ReviewSubmissionRepositoryImpl.java`
- Create: `test/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/submission/ReviewSubmissionPersistenceTest.java`

- [ ] **Step 1: Create AnswerSetJsonConverter**

```java
package com.tzuchi.his.quality.inspection.infrastructure.persistence.submission;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Converter
public class AnswerSetJsonConverter implements AttributeConverter<List<AnswerItem>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<AnswerItem> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            List<Map<String, Object>> list = attribute.stream().map(a -> {
                Map<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("itemId", a.getItemId());
                map.put("answerValue", a.getAnswerValue());
                map.put("derivedScore", a.getDerivedScore());
                return map;
            }).collect(Collectors.toList());
            return MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize answerSet to JSON", e);
        }
    }

    @Override
    public List<AnswerItem> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || "[]".equals(dbData)) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> list = MAPPER.readValue(dbData,
                    new TypeReference<List<Map<String, Object>>>() {});
            return list.stream().map(m -> {
                String itemId = (String) m.get("itemId");
                String answerValue = (String) m.get("answerValue");
                BigDecimal derivedScore = m.get("derivedScore") != null
                        ? new BigDecimal(m.get("derivedScore").toString())
                        : null;
                return AnswerItem.reconstitute(itemId, answerValue, derivedScore);
            }).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize answerSet from JSON", e);
        }
    }
}
```

- [ ] **Step 2: Create ReviewSubmissionEntity**

```java
package com.tzuchi.his.quality.inspection.infrastructure.persistence.submission;

import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "review_submission")
public class ReviewSubmissionEntity {

    @Id
    @Column(name = "submission_id")
    private String submissionId;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStage stage;

    @Column(name = "reviewer_id", nullable = false)
    private String reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;

    @Column(name = "revision_no")
    private int revisionNo;

    @Convert(converter = AnswerSetJsonConverter.class)
    @Column(name = "answer_set_json", columnDefinition = "TEXT")
    private List<AnswerItem> answerSet;

    @Column(name = "total_score", precision = 10, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "comment")
    private String comment;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "based_on_score_sheet_version_id")
    private String basedOnScoreSheetVersionId;

    @Column(name = "superseded_by_submission_id")
    private String supersededBySubmissionId;

    @Column(name = "attachment_refs_json", columnDefinition = "TEXT")
    private String attachmentRefsJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected ReviewSubmissionEntity() {}

    public String getSubmissionId() { return submissionId; }
    public void setSubmissionId(String submissionId) { this.submissionId = submissionId; }

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public ReviewStage getStage() { return stage; }
    public void setStage(ReviewStage stage) { this.stage = stage; }

    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }

    public SubmissionStatus getStatus() { return status; }
    public void setStatus(SubmissionStatus status) { this.status = status; }

    public int getRevisionNo() { return revisionNo; }
    public void setRevisionNo(int revisionNo) { this.revisionNo = revisionNo; }

    public List<AnswerItem> getAnswerSet() { return answerSet; }
    public void setAnswerSet(List<AnswerItem> answerSet) { this.answerSet = answerSet; }

    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public String getBasedOnScoreSheetVersionId() { return basedOnScoreSheetVersionId; }
    public void setBasedOnScoreSheetVersionId(String basedOnScoreSheetVersionId) { this.basedOnScoreSheetVersionId = basedOnScoreSheetVersionId; }

    public String getSupersededBySubmissionId() { return supersededBySubmissionId; }
    public void setSupersededBySubmissionId(String supersededBySubmissionId) { this.supersededBySubmissionId = supersededBySubmissionId; }

    public String getAttachmentRefsJson() { return attachmentRefsJson; }
    public void setAttachmentRefsJson(String attachmentRefsJson) { this.attachmentRefsJson = attachmentRefsJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 3: Create ReviewSubmissionJpaRepository**

```java
package com.tzuchi.his.quality.inspection.infrastructure.persistence.submission;

import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewSubmissionJpaRepository extends JpaRepository<ReviewSubmissionEntity, String> {

    List<ReviewSubmissionEntity> findByCaseIdAndStage(String caseId, ReviewStage stage);

    @Query("SELECT e FROM ReviewSubmissionEntity e " +
           "WHERE e.caseId = :caseId AND e.stage = :stage " +
           "ORDER BY e.revisionNo DESC")
    List<ReviewSubmissionEntity> findByCaseIdAndStageOrderByRevisionNoDesc(
            @Param("caseId") String caseId, @Param("stage") ReviewStage stage);
}
```

- [ ] **Step 4: Create ReviewSubmissionRepositoryImpl**

```java
package com.tzuchi.his.quality.inspection.infrastructure.persistence.submission;

import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ReviewSubmissionRepositoryImpl implements ReviewSubmissionRepository {

    private final ReviewSubmissionJpaRepository jpa;

    public ReviewSubmissionRepositoryImpl(ReviewSubmissionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(ReviewSubmission submission) {
        jpa.save(toEntity(submission));
    }

    @Override
    public Optional<ReviewSubmission> findById(String submissionId) {
        return jpa.findById(submissionId).map(this::toDomain);
    }

    @Override
    public List<ReviewSubmission> findByCaseIdAndStage(String caseId, ReviewStage stage) {
        return jpa.findByCaseIdAndStage(caseId, stage).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ReviewSubmission> findLatestByCaseIdAndStage(String caseId, ReviewStage stage) {
        List<ReviewSubmissionEntity> results =
                jpa.findByCaseIdAndStageOrderByRevisionNoDesc(caseId, stage);
        return results.isEmpty() ? Optional.empty() : Optional.of(toDomain(results.get(0)));
    }

    private ReviewSubmission toDomain(ReviewSubmissionEntity e) {
        return ReviewSubmission.reconstitute(
                e.getSubmissionId(), e.getCaseId(), e.getStage(),
                e.getReviewerId(), e.getStatus(), e.getRevisionNo(),
                e.getAnswerSet() != null ? e.getAnswerSet() : Collections.emptyList(),
                e.getTotalScore(), e.getComment(), e.getSubmittedAt(),
                e.getBasedOnScoreSheetVersionId(), e.getSupersededBySubmissionId(),
                e.getAttachmentRefsJson(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private ReviewSubmissionEntity toEntity(ReviewSubmission s) {
        ReviewSubmissionEntity e = new ReviewSubmissionEntity();
        e.setSubmissionId(s.getSubmissionId());
        e.setCaseId(s.getCaseId());
        e.setStage(s.getStage());
        e.setReviewerId(s.getReviewerId());
        e.setStatus(s.getStatus());
        e.setRevisionNo(s.getRevisionNo());
        e.setAnswerSet(s.getAnswerSet());
        e.setTotalScore(s.getTotalScore());
        e.setComment(s.getComment());
        e.setSubmittedAt(s.getSubmittedAt());
        e.setBasedOnScoreSheetVersionId(s.getBasedOnScoreSheetVersionId());
        e.setSupersededBySubmissionId(s.getSupersededBySubmissionId());
        e.setAttachmentRefsJson(s.getAttachmentRefsJson());
        e.setCreatedAt(s.getCreatedAt());
        e.setUpdatedAt(s.getUpdatedAt());
        return e;
    }
}
```

- [ ] **Step 5: Write ReviewSubmissionPersistenceTest**

```java
package com.tzuchi.his.quality.inspection.infrastructure.persistence.submission;

import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReviewSubmissionPersistenceTest {

    @Autowired
    private ReviewSubmissionRepositoryImpl repository;

    @Autowired
    private ReviewSubmissionJpaRepository jpa;

    @Test
    void saveAndFindById_draftSubmission_shouldRoundTrip() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                "sub-1", "c1", "reviewer-1", "ss-1");

        repository.save(sub);

        Optional<ReviewSubmission> found = repository.findById("sub-1");
        assertTrue(found.isPresent());
        ReviewSubmission loaded = found.get();
        assertEquals("sub-1", loaded.getSubmissionId());
        assertEquals("c1", loaded.getCaseId());
        assertEquals(ReviewStage.INITIAL, loaded.getStage());
        assertEquals("reviewer-1", loaded.getReviewerId());
        assertEquals(SubmissionStatus.DRAFT, loaded.getStatus());
        assertEquals(1, loaded.getRevisionNo());
        assertEquals(BigDecimal.ZERO, loaded.getTotalScore());
        assertNotNull(loaded.getCreatedAt());
        assertTrue(loaded.getAnswerSet().isEmpty());
    }

    @Test
    void saveAndFindById_withAnswerSet_shouldPreserveJsonRoundTrip() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                "sub-2", "c1", "reviewer-1", "ss-1");
        List<AnswerItem> answers = Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.0")),
                AnswerItem.ofScored("Q002", "15", new BigDecimal("7.5"))
        );
        sub.updateDraft(answers, new BigDecimal("17.5"), "draft comment");

        repository.save(sub);

        ReviewSubmission loaded = repository.findById("sub-2").orElseThrow();
        assertEquals(2, loaded.getAnswerSet().size());
        assertEquals("Q001", loaded.getAnswerSet().get(0).getItemId());
        assertEquals("true", loaded.getAnswerSet().get(0).getAnswerValue());
        assertEquals(new BigDecimal("10.0"), loaded.getAnswerSet().get(0).getDerivedScore());
        assertEquals("Q002", loaded.getAnswerSet().get(1).getItemId());
        assertEquals(new BigDecimal("17.5"), loaded.getTotalScore());
        assertEquals("draft comment", loaded.getComment());
    }

    @Test
    void saveAndFindById_submittedSubmission_shouldPreserveStatus() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                "sub-3", "c1", "reviewer-1", "ss-1");
        sub.updateDraft(Collections.emptyList(), BigDecimal.ZERO, null);
        sub.submit();

        repository.save(sub);

        ReviewSubmission loaded = repository.findById("sub-3").orElseThrow();
        assertEquals(SubmissionStatus.SUBMITTED, loaded.getStatus());
        assertNotNull(loaded.getSubmittedAt());
    }

    @Test
    void findByCaseIdAndStage_shouldReturnMatchingSubmissions() {
        ReviewSubmission sub1 = ReviewSubmission.createInitialDraft(
                "sub-a", "c1", "reviewer-1", "ss-1");
        ReviewSubmission sub2 = ReviewSubmission.createInitialDraft(
                "sub-b", "c1", "reviewer-1", "ss-1");
        repository.save(sub1);
        repository.save(sub2);

        List<ReviewSubmission> results = repository.findByCaseIdAndStage("c1", ReviewStage.INITIAL);
        assertEquals(2, results.size());
    }

    @Test
    void findLatestByCaseIdAndStage_shouldReturnHighestRevisionNo() {
        ReviewSubmission sub1 = ReviewSubmission.createInitialDraft(
                "sub-first", "c1", "reviewer-1", "ss-1");
        repository.save(sub1);

        // Seed a second submission with higher revisionNo directly via JPA
        ReviewSubmissionEntity e2 = new ReviewSubmissionEntity();
        e2.setSubmissionId("sub-second");
        e2.setCaseId("c1");
        e2.setStage(ReviewStage.INITIAL);
        e2.setReviewerId("reviewer-1");
        e2.setStatus(SubmissionStatus.DRAFT);
        e2.setRevisionNo(2);
        e2.setTotalScore(BigDecimal.ZERO);
        e2.setBasedOnScoreSheetVersionId("ss-1");
        e2.setCreatedAt(java.time.LocalDateTime.now());
        e2.setUpdatedAt(java.time.LocalDateTime.now());
        jpa.save(e2);

        Optional<ReviewSubmission> latest = repository.findLatestByCaseIdAndStage("c1", ReviewStage.INITIAL);
        assertTrue(latest.isPresent());
        assertEquals("sub-second", latest.get().getSubmissionId());
    }

    @Test
    void findById_notFound_shouldReturnEmpty() {
        assertTrue(repository.findById("nonexistent").isEmpty());
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.infrastructure.persistence.submission.ReviewSubmissionPersistenceTest" 2>&1 | tail -5`
Expected: All 6 tests PASS.

- [ ] **Step 7: Run full test suite**

Run: `cd services/quality-inspection-service && ./gradlew test 2>&1 | tail -5`
Expected: All tests pass.

- [ ] **Step 8: Commit**

```bash
cd services/quality-inspection-service
git add src/main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/submission/ \
        src/test/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/submission/
git commit -m "feat(b3b): add JPA persistence for ReviewSubmission

- ReviewSubmissionEntity (all columns incl. answer_set_json)
- AnswerSetJsonConverter (List<AnswerItem> ↔ JSON)
- ReviewSubmissionJpaRepository (Spring Data + custom query)
- ReviewSubmissionRepositoryImpl (toDomain/toEntity mapping)
- Round-trip persistence tests (6 tests)

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 10: REST Layer — DTOs + Controller + TestTokenFactory + InitialReviewApiTest

**Files:**
- Create: `main/java/com/tzuchi/his/quality/inspection/interfaces/rest/dto/StartInitialReviewResponse.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/interfaces/rest/dto/SaveInitialReviewDraftRequest.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/interfaces/rest/dto/SaveDraftResponse.java`
- Modify: `main/java/com/tzuchi/his/quality/inspection/interfaces/rest/ReviewCaseController.java`
- Modify: `test/java/com/tzuchi/his/quality/inspection/TestTokenFactory.java`
- Create: `test/java/com/tzuchi/his/quality/inspection/interfaces/rest/InitialReviewApiTest.java`

- [ ] **Step 1: Create StartInitialReviewResponse DTO**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

public class StartInitialReviewResponse {

    private String submissionId;

    public StartInitialReviewResponse() {}

    public StartInitialReviewResponse(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getSubmissionId() { return submissionId; }
    public void setSubmissionId(String submissionId) { this.submissionId = submissionId; }
}
```

- [ ] **Step 2: Create SaveInitialReviewDraftRequest DTO (with inner AnswerInputDto)**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

public class SaveInitialReviewDraftRequest {

    @NotNull(message = "answers must not be null")
    @Size(min = 1, message = "answers must have at least one item")
    @Valid
    private List<AnswerInputDto> answers;

    private String comment;

    public List<AnswerInputDto> getAnswers() { return answers; }
    public void setAnswers(List<AnswerInputDto> answers) { this.answers = answers; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public static class AnswerInputDto {

        @NotBlank(message = "itemId must not be blank")
        private String itemId;

        @NotNull(message = "answerValue must not be null")
        private String answerValue;

        public AnswerInputDto() {}

        public AnswerInputDto(String itemId, String answerValue) {
            this.itemId = itemId;
            this.answerValue = answerValue;
        }

        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }

        public String getAnswerValue() { return answerValue; }
        public void setAnswerValue(String answerValue) { this.answerValue = answerValue; }
    }
}
```

- [ ] **Step 3: Create SaveDraftResponse DTO**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.math.BigDecimal;

public class SaveDraftResponse {

    private BigDecimal totalScore;

    public SaveDraftResponse() {}

    public SaveDraftResponse(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
}
```

- [ ] **Step 4: Add reviewerToken() to TestTokenFactory**

Add two methods to `TestTokenFactory.java` after the existing `unknownRoleToken()` method:

```java
    /** Returns a JWT with REVIEWER role and specified userId + hospitalId. */
    public static String reviewerToken(String userId, String hospitalId) {
        return buildToken(userId, "Test Reviewer", hospitalId,
                Map.of("role_name", "reviewer", "enabled", true));
    }

    /** Returns a JWT with REVIEWER role, default userId and hospitalId. */
    public static String reviewerToken() {
        return reviewerToken("reviewer-1", DEFAULT_HOSPITAL_ID);
    }
```

The full file after modification:

```java
package com.tzuchi.his.quality.inspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates unsigned test JWTs for @SpringBootTest integration tests.
 * The service does not re-verify the JWT signature (gateway already did),
 * so a fake signature is accepted.
 */
public class TestTokenFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String DEFAULT_HOSPITAL_ID = "H001";

    /** Returns a JWT with HOSPITAL_MANAGER role and default hospitalId (H001). */
    public static String hospitalManagerToken() {
        return hospitalManagerToken(DEFAULT_HOSPITAL_ID);
    }

    /** Returns a JWT with HOSPITAL_MANAGER role and specified hospitalId. */
    public static String hospitalManagerToken(String hospitalId) {
        return buildToken("u-test-01", "Test Manager", hospitalId,
                Map.of("role_name", "hospital_manager", "enabled", true));
    }

    /** Returns a JWT with SYSTEM_ADMIN role and specified hospitalId. */
    public static String systemAdminToken(String hospitalId) {
        return buildToken("u-admin-01", "Test Admin", hospitalId,
                Map.of("role_name", "system_admin", "enabled", true));
    }

    /** Returns a JWT with SYSTEM_ADMIN role and default hospitalId (H001). */
    public static String systemAdminToken() {
        return systemAdminToken(DEFAULT_HOSPITAL_ID);
    }

    /** Returns a JWT with an unknown role (no DomainRole match). */
    public static String unknownRoleToken() {
        return buildToken("u-unknown", "Unknown Role User", null,
                Map.of("role_name", "unknown_role", "enabled", true));
    }

    /** Returns a JWT with REVIEWER role and specified userId + hospitalId. */
    public static String reviewerToken(String userId, String hospitalId) {
        return buildToken(userId, "Test Reviewer", hospitalId,
                Map.of("role_name", "reviewer", "enabled", true));
    }

    /** Returns a JWT with REVIEWER role, default userId and hospitalId. */
    public static String reviewerToken() {
        return reviewerToken("reviewer-1", DEFAULT_HOSPITAL_ID);
    }

    private static String buildToken(String userId, String userName,
                                     String hospitalId, Map<String, Object> role) {
        try {
            String header = base64url(MAPPER.writeValueAsString(
                    Map.of("alg", "RS512", "typ", "JWT")));

            // Build user object — use LinkedHashMap to allow null values
            Map<String, Object> userObj = new LinkedHashMap<>();
            userObj.put("user_id", userId);
            userObj.put("user_name", userName);
            userObj.put("user_role", new Object[]{role});
            if (hospitalId != null) {
                userObj.put("hospital_id", hospitalId);
            }
            String tzUserJson = MAPPER.writeValueAsString(Map.of("user", userObj));

            String payloadJson = MAPPER.writeValueAsString(Map.of(
                    "iss", "api-gateway",
                    "tz-user", tzUserJson
            ));
            String payload = base64url(payloadJson);

            return header + "." + payload + ".test-sig";
        } catch (Exception e) {
            throw new RuntimeException("Failed to build test token", e);
        }
    }

    private static String base64url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
    }
}
```

- [ ] **Step 5: Modify ReviewCaseController — add 3 B3b endpoints**

Replace the entire `ReviewCaseController.java`:

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.*;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/review-cases")
public class ReviewCaseController {

    private final AssignReviewCaseUseCase assignUseCase;
    private final ReassignReviewCaseUseCase reassignUseCase;
    private final CancelReviewCaseUseCase cancelUseCase;
    private final ReturnInitialReviewUseCase returnUseCase;
    private final ReopenInitialReviewUseCase reopenUseCase;
    private final StartInitialReviewUseCase startInitialReviewUseCase;
    private final SaveInitialReviewDraftUseCase saveInitialReviewDraftUseCase;
    private final SubmitInitialReviewUseCase submitInitialReviewUseCase;

    public ReviewCaseController(AssignReviewCaseUseCase assignUseCase,
                                 ReassignReviewCaseUseCase reassignUseCase,
                                 CancelReviewCaseUseCase cancelUseCase,
                                 ReturnInitialReviewUseCase returnUseCase,
                                 ReopenInitialReviewUseCase reopenUseCase,
                                 StartInitialReviewUseCase startInitialReviewUseCase,
                                 SaveInitialReviewDraftUseCase saveInitialReviewDraftUseCase,
                                 SubmitInitialReviewUseCase submitInitialReviewUseCase) {
        this.assignUseCase = assignUseCase;
        this.reassignUseCase = reassignUseCase;
        this.cancelUseCase = cancelUseCase;
        this.returnUseCase = returnUseCase;
        this.reopenUseCase = reopenUseCase;
        this.startInitialReviewUseCase = startInitialReviewUseCase;
        this.saveInitialReviewDraftUseCase = saveInitialReviewDraftUseCase;
        this.submitInitialReviewUseCase = submitInitialReviewUseCase;
    }

    // --- B3a endpoints ---

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

    // --- B3b endpoints ---

    @PostMapping("/{caseId}/start-initial-review")
    public ResponseEntity<ApiResponse<StartInitialReviewResponse>> startInitialReview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        String submissionId = startInitialReviewUseCase.execute(actor, caseId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(new StartInitialReviewResponse(submissionId)));
    }

    @PostMapping("/{caseId}/initial-review/save-draft")
    public ResponseEntity<ApiResponse<SaveDraftResponse>> saveInitialReviewDraft(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @Valid @RequestBody SaveInitialReviewDraftRequest request) {
        List<AnswerInput> answers = request.getAnswers().stream()
                .map(dto -> new AnswerInput(dto.getItemId(), dto.getAnswerValue()))
                .collect(Collectors.toList());

        BigDecimal totalScore = saveInitialReviewDraftUseCase.execute(
                actor, caseId, answers, request.getComment());

        return ResponseEntity.ok(ApiResponse.ok(new SaveDraftResponse(totalScore)));
    }

    @PostMapping("/{caseId}/initial-review/submit")
    public ResponseEntity<ApiResponse<Void>> submitInitialReview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        submitInitialReviewUseCase.execute(actor, caseId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
```

- [ ] **Step 6: Write InitialReviewApiTest**

This test seeds the full chain: Project (ACTIVE) + ScoreSheet (PUBLISHED) + Case (PENDING_INITIAL_REVIEW, assigned) via JPA entities.

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InitialReviewApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ReviewProjectJpaRepository projectJpa;
    @Autowired private ScoreSheetJpaRepository scoreSheetJpa;
    @Autowired private ReviewCaseJpaRepository caseJpa;
    @Autowired private ReviewSubmissionJpaRepository submissionJpa;

    private static final String BASE = "/review-cases";
    private static final String CASE_ID = "c1";
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

    // --- Seed helpers ---

    private void seedProjectAndScoreSheet() {
        // Score sheet with two items: BOOLEAN (Q001, ALWAYS_REQUIRED) + RANGE (Q002, OPTIONAL)
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

        // Active project bound to score sheet
        ReviewProjectEntity proj = new ReviewProjectEntity();
        proj.setProjectId(PROJECT_ID);
        proj.setName("Test Project");
        proj.setHospitalId(HOSPITAL_ID);
        proj.setStatus(com.tzuchi.his.quality.inspection.domain.project.ProjectStatus.ACTIVE);
        proj.setVersionNumber(1);
        proj.setVersionPublished(true);
        proj.setBoundScoreSheetId(SCORE_SHEET_ID);
        projectJpa.save(proj);
    }

    private void seedAssignedCase() {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(CASE_ID);
        e.setBatchId("b1");
        e.setProjectId(PROJECT_ID);
        e.setHospitalId(HOSPITAL_ID);
        e.setSourceCaseKey("SK-001");
        e.setMedicalRecordType("OUTPATIENT");
        e.setStatus(CaseStatus.PENDING_INITIAL_REVIEW);
        e.setReviewerId(REVIEWER_ID);
        e.setReviewerAssignedAt(LocalDateTime.now());
        e.setDueAt(LocalDateTime.of(2026, 2, 1, 23, 59));
        caseJpa.save(e);
    }

    private void seedInProgressCaseWithSubmission() {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(CASE_ID);
        e.setBatchId("b1");
        e.setProjectId(PROJECT_ID);
        e.setHospitalId(HOSPITAL_ID);
        e.setSourceCaseKey("SK-001");
        e.setMedicalRecordType("OUTPATIENT");
        e.setStatus(CaseStatus.INITIAL_REVIEW_IN_PROGRESS);
        e.setReviewerId(REVIEWER_ID);
        e.setReviewerAssignedAt(LocalDateTime.now());
        e.setDueAt(LocalDateTime.of(2026, 2, 1, 23, 59));
        e.setLatestInitialSubmissionId("sub-1");
        e.setScoreSheetVersionRef(SCORE_SHEET_ID);
        caseJpa.save(e);

        ReviewSubmissionEntity sub = new ReviewSubmissionEntity();
        sub.setSubmissionId("sub-1");
        sub.setCaseId(CASE_ID);
        sub.setStage(ReviewStage.INITIAL);
        sub.setReviewerId(REVIEWER_ID);
        sub.setStatus(SubmissionStatus.DRAFT);
        sub.setRevisionNo(1);
        sub.setTotalScore(BigDecimal.ZERO);
        sub.setBasedOnScoreSheetVersionId(SCORE_SHEET_ID);
        sub.setCreatedAt(LocalDateTime.now());
        sub.setUpdatedAt(LocalDateTime.now());
        submissionJpa.save(sub);
    }

    private void seedInProgressCaseWithDraftAnswers() {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(CASE_ID);
        e.setBatchId("b1");
        e.setProjectId(PROJECT_ID);
        e.setHospitalId(HOSPITAL_ID);
        e.setSourceCaseKey("SK-001");
        e.setMedicalRecordType("OUTPATIENT");
        e.setStatus(CaseStatus.INITIAL_REVIEW_IN_PROGRESS);
        e.setReviewerId(REVIEWER_ID);
        e.setReviewerAssignedAt(LocalDateTime.now());
        e.setDueAt(LocalDateTime.of(2026, 2, 1, 23, 59));
        e.setLatestInitialSubmissionId("sub-1");
        e.setScoreSheetVersionRef(SCORE_SHEET_ID);
        caseJpa.save(e);

        ReviewSubmissionEntity sub = new ReviewSubmissionEntity();
        sub.setSubmissionId("sub-1");
        sub.setCaseId(CASE_ID);
        sub.setStage(ReviewStage.INITIAL);
        sub.setReviewerId(REVIEWER_ID);
        sub.setStatus(SubmissionStatus.DRAFT);
        sub.setRevisionNo(1);
        // Pre-populate with both Q001 and Q002 answered
        sub.setAnswerSet(java.util.Arrays.asList(
                com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem
                        .ofScored("Q001", "true", new BigDecimal("10.0")),
                com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem
                        .ofScored("Q002", "15", new BigDecimal("7.5"))
        ));
        sub.setTotalScore(new BigDecimal("17.5"));
        sub.setComment("draft comment");
        sub.setBasedOnScoreSheetVersionId(SCORE_SHEET_ID);
        sub.setCreatedAt(LocalDateTime.now());
        sub.setUpdatedAt(LocalDateTime.now());
        submissionJpa.save(sub);
    }

    // --- START INITIAL REVIEW ---

    @Test
    void startInitialReview_shouldReturn201WithSubmissionId() throws Exception {
        seedAssignedCase();

        mockMvc.perform(post(BASE + "/c1/start-initial-review")
                        .header("api-key", reviewerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.submissionId").isString());
    }

    @Test
    void startInitialReview_wrongRole_shouldReturn403() throws Exception {
        seedAssignedCase();

        mockMvc.perform(post(BASE + "/c1/start-initial-review")
                        .header("api-key", hmToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void startInitialReview_notAssignedReviewer_shouldReturn403() throws Exception {
        seedAssignedCase();

        String otherToken = TestTokenFactory.reviewerToken("other-reviewer", HOSPITAL_ID);

        mockMvc.perform(post(BASE + "/c1/start-initial-review")
                        .header("api-key", otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void startInitialReview_caseNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(post(BASE + "/nonexistent/start-initial-review")
                        .header("api-key", reviewerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void startInitialReview_wrongState_shouldReturn409() throws Exception {
        // Case is already IN_PROGRESS
        seedInProgressCaseWithSubmission();

        mockMvc.perform(post(BASE + "/c1/start-initial-review")
                        .header("api-key", reviewerToken))
                .andExpect(status().isConflict());
    }

    // --- SAVE DRAFT ---

    @Test
    void saveDraft_shouldReturn200WithTotalScore() throws Exception {
        seedInProgressCaseWithSubmission();

        String body = "{\"answers\":[" +
                "{\"itemId\":\"Q001\",\"answerValue\":\"true\"}," +
                "{\"itemId\":\"Q002\",\"answerValue\":\"15\"}" +
                "],\"comment\":\"my draft comment\"}";

        mockMvc.perform(post(BASE + "/c1/initial-review/save-draft")
                        .header("api-key", reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalScore").value(17.5));
    }

    @Test
    void saveDraft_wrongRole_shouldReturn403() throws Exception {
        seedInProgressCaseWithSubmission();

        String body = "{\"answers\":[{\"itemId\":\"Q001\",\"answerValue\":\"true\"}]}";

        mockMvc.perform(post(BASE + "/c1/initial-review/save-draft")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void saveDraft_missingAnswers_shouldReturn400() throws Exception {
        seedInProgressCaseWithSubmission();

        mockMvc.perform(post(BASE + "/c1/initial-review/save-draft")
                        .header("api-key", reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveDraft_notOwner_shouldReturn403() throws Exception {
        seedInProgressCaseWithSubmission();

        String otherToken = TestTokenFactory.reviewerToken("other-reviewer", HOSPITAL_ID);
        String body = "{\"answers\":[{\"itemId\":\"Q001\",\"answerValue\":\"true\"}]}";

        mockMvc.perform(post(BASE + "/c1/initial-review/save-draft")
                        .header("api-key", otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // --- SUBMIT ---

    @Test
    void submit_shouldReturn200() throws Exception {
        seedInProgressCaseWithDraftAnswers();

        mockMvc.perform(post(BASE + "/c1/initial-review/submit")
                        .header("api-key", reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void submit_wrongRole_shouldReturn403() throws Exception {
        seedInProgressCaseWithDraftAnswers();

        mockMvc.perform(post(BASE + "/c1/initial-review/submit")
                        .header("api-key", hmToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void submit_notOwner_shouldReturn403() throws Exception {
        seedInProgressCaseWithDraftAnswers();

        String otherToken = TestTokenFactory.reviewerToken("other-reviewer", HOSPITAL_ID);

        mockMvc.perform(post(BASE + "/c1/initial-review/submit")
                        .header("api-key", otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void submit_missingRequiredItems_shouldReturn422() throws Exception {
        // Seed submission with only Q002 (optional) — Q001 (required) is missing
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(CASE_ID);
        e.setBatchId("b1");
        e.setProjectId(PROJECT_ID);
        e.setHospitalId(HOSPITAL_ID);
        e.setSourceCaseKey("SK-001");
        e.setMedicalRecordType("OUTPATIENT");
        e.setStatus(CaseStatus.INITIAL_REVIEW_IN_PROGRESS);
        e.setReviewerId(REVIEWER_ID);
        e.setReviewerAssignedAt(LocalDateTime.now());
        e.setDueAt(LocalDateTime.of(2026, 2, 1, 23, 59));
        e.setLatestInitialSubmissionId("sub-1");
        e.setScoreSheetVersionRef(SCORE_SHEET_ID);
        caseJpa.save(e);

        ReviewSubmissionEntity sub = new ReviewSubmissionEntity();
        sub.setSubmissionId("sub-1");
        sub.setCaseId(CASE_ID);
        sub.setStage(ReviewStage.INITIAL);
        sub.setReviewerId(REVIEWER_ID);
        sub.setStatus(SubmissionStatus.DRAFT);
        sub.setRevisionNo(1);
        // Only Q002 answered — Q001 (ALWAYS_REQUIRED) missing
        sub.setAnswerSet(Collections.singletonList(
                com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem
                        .ofScored("Q002", "15", new BigDecimal("7.5"))));
        sub.setTotalScore(new BigDecimal("7.5"));
        sub.setBasedOnScoreSheetVersionId(SCORE_SHEET_ID);
        sub.setCreatedAt(LocalDateTime.now());
        sub.setUpdatedAt(LocalDateTime.now());
        submissionJpa.save(sub);

        mockMvc.perform(post(BASE + "/c1/initial-review/submit")
                        .header("api-key", reviewerToken))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void submit_noApiKey_shouldReturn401() throws Exception {
        mockMvc.perform(post(BASE + "/c1/initial-review/submit"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "com.tzuchi.his.quality.inspection.interfaces.rest.InitialReviewApiTest" 2>&1 | tail -10`
Expected: All 10 tests PASS.

- [ ] **Step 8: Run full test suite**

Run: `cd services/quality-inspection-service && ./gradlew test 2>&1 | tail -5`
Expected: All tests pass (including all existing B3a tests).

- [ ] **Step 9: Commit**

```bash
cd services/quality-inspection-service
git add src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/ReviewCaseController.java \
        src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/dto/StartInitialReviewResponse.java \
        src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/dto/SaveInitialReviewDraftRequest.java \
        src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/dto/SaveDraftResponse.java \
        src/test/java/com/tzuchi/his/quality/inspection/TestTokenFactory.java \
        src/test/java/com/tzuchi/his/quality/inspection/interfaces/rest/InitialReviewApiTest.java
git commit -m "feat(b3b): add REST endpoints for initial review operations

- POST /{caseId}/start-initial-review (201 Created)
- POST /{caseId}/initial-review/save-draft (200 OK)
- POST /{caseId}/initial-review/submit (200 OK, no body)
- DTOs: StartInitialReviewResponse, SaveInitialReviewDraftRequest, SaveDraftResponse
- TestTokenFactory: add reviewerToken() for REVIEWER role
- InitialReviewApiTest: 10 integration tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---
