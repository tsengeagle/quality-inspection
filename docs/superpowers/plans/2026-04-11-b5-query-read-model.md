# B5 — Backend Query / Read Model Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement all 7 remaining query/read endpoints defined in api-spec.md (case list, case detail, submission detail, audit trails, lifecycle, dashboard, export).

**Architecture:** Follow the established QueryPort pattern (application interface → infrastructure adapter using JPA Criteria API). Dashboard/Export go into the `reporting/` layer. All query use cases take `ActorContext` and do basic role check. Pagination uses the existing `Page<T>` wrapper.

**Tech Stack:** Java 11, Spring Boot 2.7.18, JPA/Hibernate, H2 for tests, Gradle 7.6.4

**Spec:** `docs/superpowers/specs/2026-04-11-b5-query-read-model-design.md`

**Base path:** `services/quality-inspection-service/`  
**Package root:** `com.tzuchi.his.quality.inspection`

**Build & test command:**
```bash
cd services/quality-inspection-service && ./gradlew clean test
```

---

## Phase B5a — Case + Submission Queries

---

### Task 1: ReviewCaseQueryCriteria + ReviewCaseSummary

**Files:**
- Create: `src/main/java/com/tzuchi/his/quality/inspection/application/reviewcase/query/ReviewCaseQueryCriteria.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/application/reviewcase/query/ReviewCaseSummary.java`

- [ ] **Step 1: Create ReviewCaseQueryCriteria**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;

public class ReviewCaseQueryCriteria {

    private final String hospitalId;
    private final String projectId;
    private final String batchId;
    private final CaseStatus status;
    private final String reviewerId;
    private final String rereviewerId;
    private final String medicalRecordType;
    private final int page;
    private final int size;

    public ReviewCaseQueryCriteria(String hospitalId, String projectId, String batchId,
                                    CaseStatus status, String reviewerId, String rereviewerId,
                                    String medicalRecordType, int page, int size) {
        this.hospitalId = hospitalId;
        this.projectId = projectId;
        this.batchId = batchId;
        this.status = status;
        this.reviewerId = reviewerId;
        this.rereviewerId = rereviewerId;
        this.medicalRecordType = medicalRecordType;
        this.page = Math.max(0, page);
        this.size = size > 0 ? Math.min(size, 100) : 20;
    }

    public String getHospitalId() { return hospitalId; }
    public String getProjectId() { return projectId; }
    public String getBatchId() { return batchId; }
    public CaseStatus getStatus() { return status; }
    public String getReviewerId() { return reviewerId; }
    public String getRereviewerId() { return rereviewerId; }
    public String getMedicalRecordType() { return medicalRecordType; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}
```

- [ ] **Step 2: Create ReviewCaseSummary**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.time.LocalDateTime;

public class ReviewCaseSummary {

    private final String caseId;
    private final String projectId;
    private final String batchId;
    private final String hospitalId;
    private final String status;
    private final String reviewerId;
    private final String rereviewerId;
    private final String medicalRecordType;
    private final String sourceCaseKey;
    private final LocalDateTime createdAt;
    private final LocalDateTime assignedAt;
    private final LocalDateTime dueAt;

    public ReviewCaseSummary(String caseId, String projectId, String batchId,
                              String hospitalId, String status, String reviewerId,
                              String rereviewerId, String medicalRecordType,
                              String sourceCaseKey, LocalDateTime createdAt,
                              LocalDateTime assignedAt, LocalDateTime dueAt) {
        this.caseId = caseId;
        this.projectId = projectId;
        this.batchId = batchId;
        this.hospitalId = hospitalId;
        this.status = status;
        this.reviewerId = reviewerId;
        this.rereviewerId = rereviewerId;
        this.medicalRecordType = medicalRecordType;
        this.sourceCaseKey = sourceCaseKey;
        this.createdAt = createdAt;
        this.assignedAt = assignedAt;
        this.dueAt = dueAt;
    }

    public String getCaseId() { return caseId; }
    public String getProjectId() { return projectId; }
    public String getBatchId() { return batchId; }
    public String getHospitalId() { return hospitalId; }
    public String getStatus() { return status; }
    public String getReviewerId() { return reviewerId; }
    public String getRereviewerId() { return rereviewerId; }
    public String getMedicalRecordType() { return medicalRecordType; }
    public String getSourceCaseKey() { return sourceCaseKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public LocalDateTime getDueAt() { return dueAt; }
}
```

- [ ] **Step 3: Compile check**

Run: `cd services/quality-inspection-service && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(B5a): add ReviewCaseQueryCriteria and ReviewCaseSummary

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 2: ReviewCaseQueryPort + ReviewCaseDetailView

**Files:**
- Create: `src/main/java/com/tzuchi/his/quality/inspection/application/reviewcase/query/ReviewCaseQueryPort.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/application/reviewcase/query/ReviewCaseDetailView.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/application/reviewcase/query/SubmissionSummaryItem.java`

- [ ] **Step 1: Create ReviewCaseQueryPort**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import com.tzuchi.his.quality.inspection.application.common.Page;

import java.util.Optional;

/**
 * Read-model query port for ReviewCase summaries and details.
 * Implementations are in the infrastructure layer.
 */
public interface ReviewCaseQueryPort {
    Page<ReviewCaseSummary> findCases(ReviewCaseQueryCriteria criteria);
    Optional<ReviewCaseDetailView> findDetailById(String caseId);
}
```

- [ ] **Step 2: Create SubmissionSummaryItem**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.time.LocalDateTime;

public class SubmissionSummaryItem {

    private final String submissionId;
    private final String stage;
    private final int revisionNo;
    private final String status;
    private final LocalDateTime submittedAt;

    public SubmissionSummaryItem(String submissionId, String stage, int revisionNo,
                                  String status, LocalDateTime submittedAt) {
        this.submissionId = submissionId;
        this.stage = stage;
        this.revisionNo = revisionNo;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    public String getSubmissionId() { return submissionId; }
    public String getStage() { return stage; }
    public int getRevisionNo() { return revisionNo; }
    public String getStatus() { return status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
}
```

- [ ] **Step 3: Create ReviewCaseDetailView**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ReviewCaseDetailView {

    private final String caseId;
    private final String projectId;
    private final String batchId;
    private final String hospitalId;
    private final String status;
    private final String reviewerId;
    private final String rereviewerId;
    private final String medicalRecordType;
    private final String sourceCaseKey;
    private final BigDecimal initialReviewScore;
    private final BigDecimal finalScore;
    private final String finalConclusion;
    private final LocalDateTime createdAt;
    private final LocalDateTime assignedAt;
    private final LocalDateTime dueAt;
    private final LocalDateTime closedAt;
    private final LocalDateTime cancelledAt;
    private final List<SubmissionSummaryItem> submissions;

    public ReviewCaseDetailView(String caseId, String projectId, String batchId,
                                 String hospitalId, String status, String reviewerId,
                                 String rereviewerId, String medicalRecordType,
                                 String sourceCaseKey, BigDecimal initialReviewScore,
                                 BigDecimal finalScore, String finalConclusion,
                                 LocalDateTime createdAt, LocalDateTime assignedAt,
                                 LocalDateTime dueAt, LocalDateTime closedAt,
                                 LocalDateTime cancelledAt,
                                 List<SubmissionSummaryItem> submissions) {
        this.caseId = caseId;
        this.projectId = projectId;
        this.batchId = batchId;
        this.hospitalId = hospitalId;
        this.status = status;
        this.reviewerId = reviewerId;
        this.rereviewerId = rereviewerId;
        this.medicalRecordType = medicalRecordType;
        this.sourceCaseKey = sourceCaseKey;
        this.initialReviewScore = initialReviewScore;
        this.finalScore = finalScore;
        this.finalConclusion = finalConclusion;
        this.createdAt = createdAt;
        this.assignedAt = assignedAt;
        this.dueAt = dueAt;
        this.closedAt = closedAt;
        this.cancelledAt = cancelledAt;
        this.submissions = submissions;
    }

    public String getCaseId() { return caseId; }
    public String getProjectId() { return projectId; }
    public String getBatchId() { return batchId; }
    public String getHospitalId() { return hospitalId; }
    public String getStatus() { return status; }
    public String getReviewerId() { return reviewerId; }
    public String getRereviewerId() { return rereviewerId; }
    public String getMedicalRecordType() { return medicalRecordType; }
    public String getSourceCaseKey() { return sourceCaseKey; }
    public BigDecimal getInitialReviewScore() { return initialReviewScore; }
    public BigDecimal getFinalScore() { return finalScore; }
    public String getFinalConclusion() { return finalConclusion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public LocalDateTime getDueAt() { return dueAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public List<SubmissionSummaryItem> getSubmissions() { return submissions; }
}
```

- [ ] **Step 4: Compile check**

Run: `cd services/quality-inspection-service && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(B5a): add ReviewCaseQueryPort, ReviewCaseDetailView, SubmissionSummaryItem

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 3: ListReviewCasesUseCase + Test

**Files:**
- Create: `src/main/java/com/tzuchi/his/quality/inspection/application/reviewcase/ListReviewCasesUseCase.java`
- Create: `src/test/java/com/tzuchi/his/quality/inspection/application/reviewcase/ListReviewCasesUseCaseTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseQueryCriteria;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseQueryPort;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ListReviewCasesUseCaseTest {

    private InMemoryReviewCaseQueryPort queryPort;
    private ListReviewCasesUseCase useCase;

    @BeforeEach
    void setUp() {
        queryPort = new InMemoryReviewCaseQueryPort();
        useCase = new ListReviewCasesUseCase(queryPort);
    }

    @Test
    void hospitalManager_canListCases() {
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
        ReviewCaseQueryCriteria criteria = new ReviewCaseQueryCriteria(null, null, null, null, null, null, null, 0, 20);

        Page<ReviewCaseSummary> result = useCase.execute(actor, criteria);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @Test
    void reviewer_canListCases() {
        ActorContext actor = new ActorContext("r1", "Reviewer", EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));
        ReviewCaseQueryCriteria criteria = new ReviewCaseQueryCriteria(null, null, null, null, null, null, null, 0, 20);

        Page<ReviewCaseSummary> result = useCase.execute(actor, criteria);

        assertNotNull(result);
    }

    @Test
    void unknownRole_shouldThrowUnauthorized() {
        ActorContext actor = new ActorContext("u1", "Nobody", EnumSet.noneOf(DomainRole.class), Optional.empty());
        ReviewCaseQueryCriteria criteria = new ReviewCaseQueryCriteria(null, null, null, null, null, null, null, 0, 20);

        assertThrows(UnauthorizedException.class, () -> useCase.execute(actor, criteria));
    }

    @Test
    void delegatesToQueryPort() {
        queryPort.setPage(new Page<>(
                Collections.singletonList(new ReviewCaseSummary("c1", "p1", "b1", "H001", "CLOSED", "r1", null, "OUTPATIENT", "SK-1", null, null, null)),
                0, 20, 1));
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
        ReviewCaseQueryCriteria criteria = new ReviewCaseQueryCriteria(null, null, null, null, null, null, null, 0, 20);

        Page<ReviewCaseSummary> result = useCase.execute(actor, criteria);

        assertEquals(1, result.getContent().size());
        assertEquals("c1", result.getContent().get(0).getCaseId());
    }

    // --- InMemory test double ---
    static class InMemoryReviewCaseQueryPort implements ReviewCaseQueryPort {
        private Page<ReviewCaseSummary> page = new Page<>(Collections.emptyList(), 0, 20, 0);

        void setPage(Page<ReviewCaseSummary> page) { this.page = page; }

        @Override
        public Page<ReviewCaseSummary> findCases(ReviewCaseQueryCriteria criteria) {
            return page;
        }

        @Override
        public Optional<com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseDetailView> findDetailById(String caseId) {
            return Optional.empty();
        }
    }
}
```

- [ ] **Step 2: Run test — expect FAIL (ListReviewCasesUseCase not found)**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "*.ListReviewCasesUseCaseTest" 2>&1 | tail -20`
Expected: Compilation error — `ListReviewCasesUseCase` does not exist

- [ ] **Step 3: Create ListReviewCasesUseCase**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseQueryCriteria;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseQueryPort;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListReviewCasesUseCase {

    private final ReviewCaseQueryPort queryPort;

    public ListReviewCasesUseCase(ReviewCaseQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public Page<ReviewCaseSummary> execute(ActorContext actor, ReviewCaseQueryCriteria criteria) {
        actor.requireAnyRole(
                DomainRole.HOSPITAL_MANAGER, DomainRole.REVIEWER,
                DomainRole.REREVIEWER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);
        return queryPort.findCases(criteria);
    }
}
```

- [ ] **Step 4: Run test — expect PASS**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "*.ListReviewCasesUseCaseTest"`
Expected: All 4 tests PASS

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(B5a): add ListReviewCasesUseCase with tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 4: GetReviewCaseDetailUseCase + Test

**Files:**
- Create: `src/main/java/com/tzuchi/his/quality/inspection/application/reviewcase/GetReviewCaseDetailUseCase.java`
- Create: `src/test/java/com/tzuchi/his/quality/inspection/application/reviewcase/GetReviewCaseDetailUseCaseTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.*;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetReviewCaseDetailUseCaseTest {

    private InMemoryQueryPort queryPort;
    private GetReviewCaseDetailUseCase useCase;

    @BeforeEach
    void setUp() {
        queryPort = new InMemoryQueryPort();
        useCase = new GetReviewCaseDetailUseCase(queryPort);
    }

    @Test
    void returnsDetailWhenFound() {
        ReviewCaseDetailView detail = new ReviewCaseDetailView(
                "c1", "p1", "b1", "H001", "CLOSED", "r1", null,
                "OUTPATIENT", "SK-1", BigDecimal.valueOf(85), BigDecimal.valueOf(85),
                "PASS", null, null, null, null, null, Collections.emptyList());
        queryPort.setDetail("c1", detail);
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        ReviewCaseDetailView result = useCase.execute(actor, "c1");

        assertEquals("c1", result.getCaseId());
        assertEquals("CLOSED", result.getStatus());
    }

    @Test
    void throwsNotFoundWhenMissing() {
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(actor, "missing"));
    }

    @Test
    void unknownRole_shouldThrowUnauthorized() {
        ActorContext actor = new ActorContext("u1", "Nobody", EnumSet.noneOf(DomainRole.class), Optional.empty());

        assertThrows(UnauthorizedException.class, () -> useCase.execute(actor, "c1"));
    }

    static class InMemoryQueryPort implements ReviewCaseQueryPort {
        private final Map<String, ReviewCaseDetailView> details = new HashMap<>();

        void setDetail(String caseId, ReviewCaseDetailView view) { details.put(caseId, view); }

        @Override
        public Page<ReviewCaseSummary> findCases(ReviewCaseQueryCriteria criteria) {
            return new Page<>(Collections.emptyList(), 0, 20, 0);
        }

        @Override
        public Optional<ReviewCaseDetailView> findDetailById(String caseId) {
            return Optional.ofNullable(details.get(caseId));
        }
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "*.GetReviewCaseDetailUseCaseTest" 2>&1 | tail -20`
Expected: Compilation error — `GetReviewCaseDetailUseCase` does not exist

- [ ] **Step 3: Create GetReviewCaseDetailUseCase**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseDetailView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseQueryPort;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetReviewCaseDetailUseCase {

    private final ReviewCaseQueryPort queryPort;

    public GetReviewCaseDetailUseCase(ReviewCaseQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public ReviewCaseDetailView execute(ActorContext actor, String caseId) {
        actor.requireAnyRole(
                DomainRole.HOSPITAL_MANAGER, DomainRole.REVIEWER,
                DomainRole.REREVIEWER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);
        return queryPort.findDetailById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", caseId));
    }
}
```

- [ ] **Step 4: Run test — expect PASS**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "*.GetReviewCaseDetailUseCaseTest"`
Expected: All 3 tests PASS

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(B5a): add GetReviewCaseDetailUseCase with tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 5: SubmissionQueryPort + SubmissionDetailView + GetSubmissionDetailUseCase + Test

**Files:**
- Create: `src/main/java/com/tzuchi/his/quality/inspection/application/reviewcase/query/SubmissionQueryPort.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/application/reviewcase/query/SubmissionDetailView.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/application/reviewcase/query/AnswerDetail.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/application/reviewcase/GetSubmissionDetailUseCase.java`
- Create: `src/test/java/com/tzuchi/his/quality/inspection/application/reviewcase/GetSubmissionDetailUseCaseTest.java`

- [ ] **Step 1: Create SubmissionQueryPort**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.util.Optional;

/**
 * Read-model query port for ReviewSubmission details.
 * Implementations are in the infrastructure layer.
 */
public interface SubmissionQueryPort {
    Optional<SubmissionDetailView> findDetailById(String submissionId);
}
```

- [ ] **Step 2: Create AnswerDetail**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.math.BigDecimal;

public class AnswerDetail {

    private final String itemId;
    private final String answerValue;
    private final BigDecimal derivedScore;

    public AnswerDetail(String itemId, String answerValue, BigDecimal derivedScore) {
        this.itemId = itemId;
        this.answerValue = answerValue;
        this.derivedScore = derivedScore;
    }

    public String getItemId() { return itemId; }
    public String getAnswerValue() { return answerValue; }
    public BigDecimal getDerivedScore() { return derivedScore; }
}
```

- [ ] **Step 3: Create SubmissionDetailView**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class SubmissionDetailView {

    private final String submissionId;
    private final String caseId;
    private final String stage;
    private final int revisionNo;
    private final String status;
    private final String reviewerId;
    private final List<AnswerDetail> answers;
    private final String comment;
    private final String attachmentRefsJson;
    private final BigDecimal totalScore;
    private final LocalDateTime createdAt;
    private final LocalDateTime submittedAt;

    public SubmissionDetailView(String submissionId, String caseId, String stage,
                                 int revisionNo, String status, String reviewerId,
                                 List<AnswerDetail> answers, String comment,
                                 String attachmentRefsJson, BigDecimal totalScore,
                                 LocalDateTime createdAt, LocalDateTime submittedAt) {
        this.submissionId = submissionId;
        this.caseId = caseId;
        this.stage = stage;
        this.revisionNo = revisionNo;
        this.status = status;
        this.reviewerId = reviewerId;
        this.answers = answers;
        this.comment = comment;
        this.attachmentRefsJson = attachmentRefsJson;
        this.totalScore = totalScore;
        this.createdAt = createdAt;
        this.submittedAt = submittedAt;
    }

    public String getSubmissionId() { return submissionId; }
    public String getCaseId() { return caseId; }
    public String getStage() { return stage; }
    public int getRevisionNo() { return revisionNo; }
    public String getStatus() { return status; }
    public String getReviewerId() { return reviewerId; }
    public List<AnswerDetail> getAnswers() { return answers; }
    public String getComment() { return comment; }
    public String getAttachmentRefsJson() { return attachmentRefsJson; }
    public BigDecimal getTotalScore() { return totalScore; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
}
```

- [ ] **Step 4: Write the failing test for GetSubmissionDetailUseCase**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionDetailView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionQueryPort;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetSubmissionDetailUseCaseTest {

    private InMemorySubmissionQueryPort queryPort;
    private GetSubmissionDetailUseCase useCase;

    @BeforeEach
    void setUp() {
        queryPort = new InMemorySubmissionQueryPort();
        useCase = new GetSubmissionDetailUseCase(queryPort);
    }

    @Test
    void returnsDetailWhenFound() {
        SubmissionDetailView detail = new SubmissionDetailView(
                "sub1", "c1", "INITIAL", 1, "SUBMITTED", "r1",
                Collections.emptyList(), "ok", null, BigDecimal.valueOf(90), null, null);
        queryPort.setDetail("sub1", detail);
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        SubmissionDetailView result = useCase.execute(actor, "sub1");

        assertEquals("sub1", result.getSubmissionId());
    }

    @Test
    void throwsNotFoundWhenMissing() {
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(actor, "missing"));
    }

    @Test
    void unknownRole_shouldThrowUnauthorized() {
        ActorContext actor = new ActorContext("u1", "Nobody", EnumSet.noneOf(DomainRole.class), Optional.empty());
        assertThrows(UnauthorizedException.class, () -> useCase.execute(actor, "sub1"));
    }

    static class InMemorySubmissionQueryPort implements SubmissionQueryPort {
        private final Map<String, SubmissionDetailView> map = new HashMap<>();
        void setDetail(String id, SubmissionDetailView v) { map.put(id, v); }

        @Override
        public Optional<SubmissionDetailView> findDetailById(String submissionId) {
            return Optional.ofNullable(map.get(submissionId));
        }
    }
}
```

- [ ] **Step 5: Run test — expect FAIL**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "*.GetSubmissionDetailUseCaseTest" 2>&1 | tail -20`

- [ ] **Step 6: Create GetSubmissionDetailUseCase**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionDetailView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionQueryPort;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetSubmissionDetailUseCase {

    private final SubmissionQueryPort queryPort;

    public GetSubmissionDetailUseCase(SubmissionQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public SubmissionDetailView execute(ActorContext actor, String submissionId) {
        actor.requireAnyRole(
                DomainRole.HOSPITAL_MANAGER, DomainRole.REVIEWER,
                DomainRole.REREVIEWER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);
        return queryPort.findDetailById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewSubmission", submissionId));
    }
}
```

- [ ] **Step 7: Run test — expect PASS**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "*.GetSubmissionDetailUseCaseTest"`
Expected: All 3 tests PASS

- [ ] **Step 8: Commit**

```bash
git add -A && git commit -m "feat(B5a): add SubmissionQueryPort, SubmissionDetailView, GetSubmissionDetailUseCase with tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 6: ReviewCaseQueryAdapter (infrastructure)

**Files:**
- Create: `src/main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/reviewcase/ReviewCaseQueryAdapter.java`

- [ ] **Step 1: Create ReviewCaseQueryAdapter**

This follows the exact pattern of `SamplingBatchQueryAdapter` — using JPA Criteria API for dynamic filtering.

```java
package com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.*;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.submission.ReviewSubmissionEntity;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ReviewCaseQueryAdapter implements ReviewCaseQueryPort {

    private final EntityManager em;

    public ReviewCaseQueryAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Page<ReviewCaseSummary> findCases(ReviewCaseQueryCriteria criteria) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // Count
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<ReviewCaseEntity> countRoot = countQuery.from(ReviewCaseEntity.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(buildPredicates(cb, countRoot, criteria).toArray(new Predicate[0]));
        long total = em.createQuery(countQuery).getSingleResult();

        // Data
        CriteriaQuery<ReviewCaseEntity> dataQuery = cb.createQuery(ReviewCaseEntity.class);
        Root<ReviewCaseEntity> dataRoot = dataQuery.from(ReviewCaseEntity.class);
        dataQuery.where(buildPredicates(cb, dataRoot, criteria).toArray(new Predicate[0]));
        dataQuery.orderBy(cb.desc(dataRoot.get("caseId")));

        TypedQuery<ReviewCaseEntity> typedQuery = em.createQuery(dataQuery);
        typedQuery.setFirstResult(criteria.getPage() * criteria.getSize());
        typedQuery.setMaxResults(criteria.getSize());

        List<ReviewCaseSummary> content = typedQuery.getResultList().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        return new Page<>(content, criteria.getPage(), criteria.getSize(), total);
    }

    @Override
    public Optional<ReviewCaseDetailView> findDetailById(String caseId) {
        ReviewCaseEntity entity = em.find(ReviewCaseEntity.class, caseId);
        if (entity == null) {
            return Optional.empty();
        }
        List<SubmissionSummaryItem> submissions = loadSubmissions(caseId);
        return Optional.of(toDetailView(entity, submissions));
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<ReviewCaseEntity> root,
                                             ReviewCaseQueryCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();
        if (criteria.getHospitalId() != null) {
            predicates.add(cb.equal(root.get("hospitalId"), criteria.getHospitalId()));
        }
        if (criteria.getProjectId() != null) {
            predicates.add(cb.equal(root.get("projectId"), criteria.getProjectId()));
        }
        if (criteria.getBatchId() != null) {
            predicates.add(cb.equal(root.get("batchId"), criteria.getBatchId()));
        }
        if (criteria.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
        }
        if (criteria.getReviewerId() != null) {
            predicates.add(cb.equal(root.get("reviewerId"), criteria.getReviewerId()));
        }
        if (criteria.getRereviewerId() != null) {
            predicates.add(cb.equal(root.get("rereviewerId"), criteria.getRereviewerId()));
        }
        if (criteria.getMedicalRecordType() != null) {
            predicates.add(cb.equal(root.get("medicalRecordType"), criteria.getMedicalRecordType()));
        }
        return predicates;
    }

    private List<SubmissionSummaryItem> loadSubmissions(String caseId) {
        TypedQuery<ReviewSubmissionEntity> q = em.createQuery(
                "SELECT s FROM ReviewSubmissionEntity s WHERE s.caseId = :caseId ORDER BY s.revisionNo DESC",
                ReviewSubmissionEntity.class);
        q.setParameter("caseId", caseId);
        return q.getResultList().stream()
                .map(s -> new SubmissionSummaryItem(
                        s.getSubmissionId(), s.getStage().name(), s.getRevisionNo(),
                        s.getStatus().name(), s.getSubmittedAt()))
                .collect(Collectors.toList());
    }

    private ReviewCaseSummary toSummary(ReviewCaseEntity e) {
        return new ReviewCaseSummary(
                e.getCaseId(), e.getProjectId(), e.getBatchId(), e.getHospitalId(),
                e.getStatus().name(), e.getReviewerId(), e.getRereviewerId(),
                e.getMedicalRecordType(), e.getSourceCaseKey(),
                null, e.getReviewerAssignedAt(), e.getDueAt());
    }

    private ReviewCaseDetailView toDetailView(ReviewCaseEntity e,
                                               List<SubmissionSummaryItem> submissions) {
        return new ReviewCaseDetailView(
                e.getCaseId(), e.getProjectId(), e.getBatchId(), e.getHospitalId(),
                e.getStatus().name(), e.getReviewerId(), e.getRereviewerId(),
                e.getMedicalRecordType(), e.getSourceCaseKey(),
                e.getInitialReviewScore(), e.getFinalScore(), e.getFinalConclusion(),
                null, e.getReviewerAssignedAt(), e.getDueAt(),
                e.getClosedAt(), e.getCancelledAt(), submissions);
    }
}
```

- [ ] **Step 2: Compile check**

Run: `cd services/quality-inspection-service && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat(B5a): add ReviewCaseQueryAdapter with Criteria API

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 7: SubmissionQueryAdapter (infrastructure)

**Files:**
- Create: `src/main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/submission/SubmissionQueryAdapter.java`

- [ ] **Step 1: Create SubmissionQueryAdapter**

```java
package com.tzuchi.his.quality.inspection.infrastructure.persistence.submission;

import com.tzuchi.his.quality.inspection.application.reviewcase.query.AnswerDetail;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionDetailView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionQueryPort;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SubmissionQueryAdapter implements SubmissionQueryPort {

    private final EntityManager em;

    public SubmissionQueryAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<SubmissionDetailView> findDetailById(String submissionId) {
        ReviewSubmissionEntity entity = em.find(ReviewSubmissionEntity.class, submissionId);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toDetailView(entity));
    }

    private SubmissionDetailView toDetailView(ReviewSubmissionEntity e) {
        List<AnswerDetail> answers = (e.getAnswerSet() != null)
                ? e.getAnswerSet().stream()
                    .map(a -> new AnswerDetail(a.getItemId(), a.getAnswerValue(), a.getDerivedScore()))
                    .collect(Collectors.toList())
                : Collections.emptyList();

        return new SubmissionDetailView(
                e.getSubmissionId(), e.getCaseId(), e.getStage().name(),
                e.getRevisionNo(), e.getStatus().name(), e.getReviewerId(),
                answers, e.getComment(), e.getAttachmentRefsJson(),
                e.getTotalScore(), e.getCreatedAt(), e.getSubmittedAt());
    }
}
```

- [ ] **Step 2: Compile check**

Run: `cd services/quality-inspection-service && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat(B5a): add SubmissionQueryAdapter

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 8: Controller endpoints + SubmissionController + API tests

**Files:**
- Modify: `src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/ReviewCaseController.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/SubmissionController.java`
- Create: `src/test/java/com/tzuchi/his/quality/inspection/interfaces/rest/ReviewCaseQueryApiTest.java`
- Create: `src/test/java/com/tzuchi/his/quality/inspection/interfaces/rest/SubmissionQueryApiTest.java`

- [ ] **Step 1: Add list + detail GET endpoints to ReviewCaseController**

Add these fields and constructor parameters to `ReviewCaseController`:

New fields:
```java
private final ListReviewCasesUseCase listReviewCasesUseCase;
private final GetReviewCaseDetailUseCase getReviewCaseDetailUseCase;
```

Add to constructor signature and body.

New endpoints (add before the existing `@PostMapping` methods):

```java
@GetMapping
public ResponseEntity<ApiResponse<Page<ReviewCaseSummary>>> listCases(
        @CurrentActor ActorContext actor,
        @RequestParam(required = false) String hospitalId,
        @RequestParam(required = false) String projectId,
        @RequestParam(required = false) String batchId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String reviewerId,
        @RequestParam(required = false) String rereviewerId,
        @RequestParam(required = false) String medicalRecordType,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    CaseStatus caseStatus = null;
    if (status != null) {
        try {
            caseStatus = CaseStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + status);
        }
    }
    ReviewCaseQueryCriteria criteria = new ReviewCaseQueryCriteria(
            hospitalId, projectId, batchId, caseStatus,
            reviewerId, rereviewerId, medicalRecordType, page, size);
    Page<ReviewCaseSummary> result = listReviewCasesUseCase.execute(actor, criteria);
    return ResponseEntity.ok(ApiResponse.ok(result));
}

@GetMapping("/{caseId}")
public ResponseEntity<ApiResponse<ReviewCaseDetailView>> getCaseDetail(
        @CurrentActor ActorContext actor,
        @PathVariable String caseId) {
    ReviewCaseDetailView detail = getReviewCaseDetailUseCase.execute(actor, caseId);
    return ResponseEntity.ok(ApiResponse.ok(detail));
}
```

Required imports to add:
```java
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseQueryCriteria;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseSummary;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseDetailView;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
```

- [ ] **Step 2: Create SubmissionController**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.GetSubmissionDetailUseCase;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionDetailView;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/review-submissions")
public class SubmissionController {

    private final GetSubmissionDetailUseCase getSubmissionDetailUseCase;

    public SubmissionController(GetSubmissionDetailUseCase getSubmissionDetailUseCase) {
        this.getSubmissionDetailUseCase = getSubmissionDetailUseCase;
    }

    @GetMapping("/{submissionId}")
    public ResponseEntity<ApiResponse<SubmissionDetailView>> getSubmissionDetail(
            @CurrentActor ActorContext actor,
            @PathVariable String submissionId) {
        SubmissionDetailView detail = getSubmissionDetailUseCase.execute(actor, submissionId);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }
}
```

- [ ] **Step 3: Write ReviewCaseQueryApiTest**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReviewCaseQueryApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ReviewCaseJpaRepository caseRepo;

    private final String hmToken = TestTokenFactory.hospitalManagerToken();

    private void seedCase(String caseId, CaseStatus status) {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(caseId);
        e.setBatchId("b1");
        e.setProjectId("p1");
        e.setHospitalId("H001");
        e.setSourceCaseKey("SK-001");
        e.setMedicalRecordType("OUTPATIENT");
        e.setStatus(status);
        caseRepo.save(e);
    }

    @Test
    void listCases_returnsPagedResult() throws Exception {
        seedCase("c1", CaseStatus.CLOSED);
        seedCase("c2", CaseStatus.PENDING_INITIAL_REVIEW);

        mockMvc.perform(get("/review-cases")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void listCases_filterByStatus() throws Exception {
        seedCase("c1", CaseStatus.CLOSED);
        seedCase("c2", CaseStatus.PENDING_INITIAL_REVIEW);

        mockMvc.perform(get("/review-cases")
                        .param("status", "CLOSED")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].caseId").value("c1"));
    }

    @Test
    void listCases_invalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/review-cases")
                        .param("status", "INVALID")
                        .header("api-key", hmToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCaseDetail_returnsDetail() throws Exception {
        seedCase("c1", CaseStatus.CLOSED);

        mockMvc.perform(get("/review-cases/c1")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.caseId").value("c1"))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @Test
    void getCaseDetail_notFound_returns404() throws Exception {
        mockMvc.perform(get("/review-cases/missing")
                        .header("api-key", hmToken))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 4: Write SubmissionQueryApiTest**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.submission.ReviewSubmissionEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.submission.ReviewSubmissionJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SubmissionQueryApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ReviewSubmissionJpaRepository submissionRepo;

    private final String hmToken = TestTokenFactory.hospitalManagerToken();

    private void seedSubmission(String submissionId) {
        ReviewSubmissionEntity e = new ReviewSubmissionEntity();
        e.setSubmissionId(submissionId);
        e.setCaseId("c1");
        e.setStage(ReviewStage.INITIAL);
        e.setReviewerId("r1");
        e.setStatus(SubmissionStatus.SUBMITTED);
        e.setRevisionNo(1);
        e.setTotalScore(BigDecimal.valueOf(85));
        e.setComment("Looks good");
        submissionRepo.save(e);
    }

    @Test
    void getSubmissionDetail_returnsDetail() throws Exception {
        seedSubmission("sub1");

        mockMvc.perform(get("/review-submissions/sub1")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submissionId").value("sub1"))
                .andExpect(jsonPath("$.data.stage").value("INITIAL"));
    }

    @Test
    void getSubmissionDetail_notFound_returns404() throws Exception {
        mockMvc.perform(get("/review-submissions/missing")
                        .header("api-key", hmToken))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 5: Run all tests**

Run: `cd services/quality-inspection-service && ./gradlew clean test`
Expected: All tests PASS (including new ones)

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(B5a): add case list, case detail, submission detail endpoints with API tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Phase B5b — Audit / Lifecycle Queries

---

### Task 9: AuditTrailQueryPort + AuditTrailQueryCriteria + AuditTrailEntry

**Files:**
- Create: `src/main/java/com/tzuchi/his/quality/inspection/application/audit/AuditTrailQueryPort.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/application/audit/AuditTrailQueryCriteria.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/application/audit/AuditTrailEntry.java`

- [ ] **Step 1: Create AuditTrailEntry**

```java
package com.tzuchi.his.quality.inspection.application.audit;

import java.time.Instant;

public class AuditTrailEntry {

    private final String id;
    private final String entityType;
    private final String entityId;
    private final String action;
    private final String actorUserId;
    private final String actorUserName;
    private final String hospitalId;
    private final Instant occurredAt;
    private final String detail;

    public AuditTrailEntry(String id, String entityType, String entityId, String action,
                            String actorUserId, String actorUserName, String hospitalId,
                            Instant occurredAt, String detail) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.actorUserId = actorUserId;
        this.actorUserName = actorUserName;
        this.hospitalId = hospitalId;
        this.occurredAt = occurredAt;
        this.detail = detail;
    }

    public String getId() { return id; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getAction() { return action; }
    public String getActorUserId() { return actorUserId; }
    public String getActorUserName() { return actorUserName; }
    public String getHospitalId() { return hospitalId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getDetail() { return detail; }
}
```

- [ ] **Step 2: Create AuditTrailQueryCriteria**

```java
package com.tzuchi.his.quality.inspection.application.audit;

import java.time.Instant;

public class AuditTrailQueryCriteria {

    private final String aggregateType;
    private final String aggregateId;
    private final String hospitalId;
    private final String eventName;
    private final Instant from;
    private final Instant to;
    private final int page;
    private final int size;

    public AuditTrailQueryCriteria(String aggregateType, String aggregateId,
                                    String hospitalId, String eventName,
                                    Instant from, Instant to, int page, int size) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.hospitalId = hospitalId;
        this.eventName = eventName;
        this.from = from;
        this.to = to;
        this.page = Math.max(0, page);
        this.size = size > 0 ? Math.min(size, 100) : 20;
    }

    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getHospitalId() { return hospitalId; }
    public String getEventName() { return eventName; }
    public Instant getFrom() { return from; }
    public Instant getTo() { return to; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}
```

- [ ] **Step 3: Create AuditTrailQueryPort**

```java
package com.tzuchi.his.quality.inspection.application.audit;

import com.tzuchi.his.quality.inspection.application.common.Page;

import java.util.List;

/**
 * Read-model query port for audit trail entries.
 * Implementations are in the infrastructure layer.
 */
public interface AuditTrailQueryPort {
    Page<AuditTrailEntry> findAuditTrails(AuditTrailQueryCriteria criteria);
    List<AuditTrailEntry> findLifecycleByEntityId(String entityType, String entityId);
}
```

- [ ] **Step 4: Compile check**

Run: `cd services/quality-inspection-service && ./gradlew compileJava`

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(B5b): add AuditTrailQueryPort, AuditTrailQueryCriteria, AuditTrailEntry

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 10: ListAuditTrailsUseCase + GetCaseLifecycleUseCase + Tests

**Files:**
- Create: `src/main/java/com/tzuchi/his/quality/inspection/application/audit/ListAuditTrailsUseCase.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/application/audit/GetCaseLifecycleUseCase.java`
- Create: `src/test/java/com/tzuchi/his/quality/inspection/application/audit/ListAuditTrailsUseCaseTest.java`
- Create: `src/test/java/com/tzuchi/his/quality/inspection/application/audit/GetCaseLifecycleUseCaseTest.java`

- [ ] **Step 1: Write ListAuditTrailsUseCaseTest**

```java
package com.tzuchi.his.quality.inspection.application.audit;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ListAuditTrailsUseCaseTest {

    private InMemoryAuditTrailQueryPort queryPort;
    private ListAuditTrailsUseCase useCase;

    @BeforeEach
    void setUp() {
        queryPort = new InMemoryAuditTrailQueryPort();
        useCase = new ListAuditTrailsUseCase(queryPort);
    }

    @Test
    void hospitalManager_canQuery() {
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
        AuditTrailQueryCriteria criteria = new AuditTrailQueryCriteria(null, null, null, null, null, null, 0, 20);

        Page<AuditTrailEntry> result = useCase.execute(actor, criteria);
        assertNotNull(result);
    }

    @Test
    void reviewer_shouldThrowUnauthorized() {
        ActorContext actor = new ActorContext("r1", "Reviewer", EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));
        AuditTrailQueryCriteria criteria = new AuditTrailQueryCriteria(null, null, null, null, null, null, 0, 20);

        assertThrows(UnauthorizedException.class, () -> useCase.execute(actor, criteria));
    }

    @Test
    void delegatesToQueryPort() {
        AuditTrailEntry entry = new AuditTrailEntry("e1", "ReviewCase", "c1", "ASSIGN_REVIEWER", "u1", "Mgr", "H001", Instant.now(), "{}");
        queryPort.setPage(new Page<>(Collections.singletonList(entry), 0, 20, 1));
        ActorContext actor = new ActorContext("u1", "Admin", EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));
        AuditTrailQueryCriteria criteria = new AuditTrailQueryCriteria(null, null, null, null, null, null, 0, 20);

        Page<AuditTrailEntry> result = useCase.execute(actor, criteria);
        assertEquals(1, result.getContent().size());
    }

    static class InMemoryAuditTrailQueryPort implements AuditTrailQueryPort {
        private Page<AuditTrailEntry> page = new Page<>(Collections.emptyList(), 0, 20, 0);
        void setPage(Page<AuditTrailEntry> p) { this.page = p; }

        @Override
        public Page<AuditTrailEntry> findAuditTrails(AuditTrailQueryCriteria criteria) { return page; }

        @Override
        public List<AuditTrailEntry> findLifecycleByEntityId(String entityType, String entityId) {
            return Collections.emptyList();
        }
    }
}
```

- [ ] **Step 2: Write GetCaseLifecycleUseCaseTest**

```java
package com.tzuchi.his.quality.inspection.application.audit;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetCaseLifecycleUseCaseTest {

    private InMemoryAuditPort queryPort;
    private GetCaseLifecycleUseCase useCase;

    @BeforeEach
    void setUp() {
        queryPort = new InMemoryAuditPort();
        useCase = new GetCaseLifecycleUseCase(queryPort);
    }

    @Test
    void returnsLifecycleEvents() {
        AuditTrailEntry e1 = new AuditTrailEntry("e1", "ReviewCase", "c1", "CREATED", "u1", "Mgr", "H001", Instant.now(), "{}");
        queryPort.setLifecycle("ReviewCase", "c1", Collections.singletonList(e1));
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        List<AuditTrailEntry> result = useCase.execute(actor, "c1");
        assertEquals(1, result.size());
        assertEquals("CREATED", result.get(0).getAction());
    }

    @Test
    void unknownRole_shouldThrowUnauthorized() {
        ActorContext actor = new ActorContext("u1", "Nobody", EnumSet.noneOf(DomainRole.class), Optional.empty());
        assertThrows(UnauthorizedException.class, () -> useCase.execute(actor, "c1"));
    }

    static class InMemoryAuditPort implements AuditTrailQueryPort {
        private final Map<String, List<AuditTrailEntry>> lifecycles = new HashMap<>();
        void setLifecycle(String entityType, String entityId, List<AuditTrailEntry> entries) {
            lifecycles.put(entityType + ":" + entityId, entries);
        }

        @Override
        public Page<AuditTrailEntry> findAuditTrails(AuditTrailQueryCriteria criteria) {
            return new Page<>(Collections.emptyList(), 0, 20, 0);
        }

        @Override
        public List<AuditTrailEntry> findLifecycleByEntityId(String entityType, String entityId) {
            return lifecycles.getOrDefault(entityType + ":" + entityId, Collections.emptyList());
        }
    }
}
```

- [ ] **Step 3: Run tests — expect FAIL**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "*.ListAuditTrailsUseCaseTest" --tests "*.GetCaseLifecycleUseCaseTest" 2>&1 | tail -10`

- [ ] **Step 4: Create ListAuditTrailsUseCase**

```java
package com.tzuchi.his.quality.inspection.application.audit;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListAuditTrailsUseCase {

    private final AuditTrailQueryPort queryPort;

    public ListAuditTrailsUseCase(AuditTrailQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public Page<AuditTrailEntry> execute(ActorContext actor, AuditTrailQueryCriteria criteria) {
        actor.requireAnyRole(DomainRole.SYSTEM_ADMIN, DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR);
        return queryPort.findAuditTrails(criteria);
    }
}
```

- [ ] **Step 5: Create GetCaseLifecycleUseCase**

```java
package com.tzuchi.his.quality.inspection.application.audit;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GetCaseLifecycleUseCase {

    private final AuditTrailQueryPort queryPort;

    public GetCaseLifecycleUseCase(AuditTrailQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public List<AuditTrailEntry> execute(ActorContext actor, String caseId) {
        actor.requireAnyRole(
                DomainRole.HOSPITAL_MANAGER, DomainRole.REVIEWER,
                DomainRole.REREVIEWER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);
        return queryPort.findLifecycleByEntityId("ReviewCase", caseId);
    }
}
```

- [ ] **Step 6: Run tests — expect PASS**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "*.ListAuditTrailsUseCaseTest" --tests "*.GetCaseLifecycleUseCaseTest"`
Expected: All 5 tests PASS

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat(B5b): add ListAuditTrailsUseCase and GetCaseLifecycleUseCase with tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 11: AuditTrailQueryAdapter + AuditController + lifecycle endpoint + API tests

**Files:**
- Create: `src/main/java/com/tzuchi/his/quality/inspection/infrastructure/audit/AuditTrailQueryAdapter.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/AuditController.java`
- Modify: `src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/ReviewCaseController.java` (add lifecycle endpoint)
- Create: `src/test/java/com/tzuchi/his/quality/inspection/interfaces/rest/AuditApiTest.java`

- [ ] **Step 1: Create AuditTrailQueryAdapter**

```java
package com.tzuchi.his.quality.inspection.infrastructure.audit;

import com.tzuchi.his.quality.inspection.application.audit.AuditTrailEntry;
import com.tzuchi.his.quality.inspection.application.audit.AuditTrailQueryCriteria;
import com.tzuchi.his.quality.inspection.application.audit.AuditTrailQueryPort;
import com.tzuchi.his.quality.inspection.application.common.Page;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuditTrailQueryAdapter implements AuditTrailQueryPort {

    private final EntityManager em;

    public AuditTrailQueryAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Page<AuditTrailEntry> findAuditTrails(AuditTrailQueryCriteria criteria) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<AuditTrailEntity> countRoot = countQuery.from(AuditTrailEntity.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(buildPredicates(cb, countRoot, criteria).toArray(new Predicate[0]));
        long total = em.createQuery(countQuery).getSingleResult();

        CriteriaQuery<AuditTrailEntity> dataQuery = cb.createQuery(AuditTrailEntity.class);
        Root<AuditTrailEntity> dataRoot = dataQuery.from(AuditTrailEntity.class);
        dataQuery.where(buildPredicates(cb, dataRoot, criteria).toArray(new Predicate[0]));
        dataQuery.orderBy(cb.desc(dataRoot.get("occurredAt")));

        TypedQuery<AuditTrailEntity> typedQuery = em.createQuery(dataQuery);
        typedQuery.setFirstResult(criteria.getPage() * criteria.getSize());
        typedQuery.setMaxResults(criteria.getSize());

        List<AuditTrailEntry> content = typedQuery.getResultList().stream()
                .map(this::toEntry)
                .collect(Collectors.toList());

        return new Page<>(content, criteria.getPage(), criteria.getSize(), total);
    }

    @Override
    public List<AuditTrailEntry> findLifecycleByEntityId(String entityType, String entityId) {
        TypedQuery<AuditTrailEntity> q = em.createQuery(
                "SELECT a FROM AuditTrailEntity a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.occurredAt ASC",
                AuditTrailEntity.class);
        q.setParameter("entityType", entityType);
        q.setParameter("entityId", entityId);
        return q.getResultList().stream()
                .map(this::toEntry)
                .collect(Collectors.toList());
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<AuditTrailEntity> root,
                                             AuditTrailQueryCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();
        if (criteria.getAggregateType() != null) {
            predicates.add(cb.equal(root.get("entityType"), criteria.getAggregateType()));
        }
        if (criteria.getAggregateId() != null) {
            predicates.add(cb.equal(root.get("entityId"), criteria.getAggregateId()));
        }
        if (criteria.getHospitalId() != null) {
            predicates.add(cb.equal(root.get("hospitalId"), criteria.getHospitalId()));
        }
        if (criteria.getEventName() != null) {
            predicates.add(cb.equal(root.get("action"), criteria.getEventName()));
        }
        if (criteria.getFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), criteria.getFrom()));
        }
        if (criteria.getTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), criteria.getTo()));
        }
        return predicates;
    }

    private AuditTrailEntry toEntry(AuditTrailEntity e) {
        return new AuditTrailEntry(
                e.getId(), e.getEntityType(), e.getEntityId(), e.getAction(),
                e.getActorUserId(), e.getActorUserName(), e.getHospitalId(),
                e.getOccurredAt(), e.getDetail());
    }
}
```

- [ ] **Step 2: Create AuditController**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.audit.AuditTrailEntry;
import com.tzuchi.his.quality.inspection.application.audit.AuditTrailQueryCriteria;
import com.tzuchi.his.quality.inspection.application.audit.ListAuditTrailsUseCase;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/audit-trails")
public class AuditController {

    private final ListAuditTrailsUseCase listAuditTrailsUseCase;

    public AuditController(ListAuditTrailsUseCase listAuditTrailsUseCase) {
        this.listAuditTrailsUseCase = listAuditTrailsUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditTrailEntry>>> listAuditTrails(
            @CurrentActor ActorContext actor,
            @RequestParam(required = false) String aggregateType,
            @RequestParam(required = false) String aggregateId,
            @RequestParam(required = false) String hospitalId,
            @RequestParam(required = false) String eventName,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AuditTrailQueryCriteria criteria = new AuditTrailQueryCriteria(
                aggregateType, aggregateId, hospitalId, eventName, from, to, page, size);
        Page<AuditTrailEntry> result = listAuditTrailsUseCase.execute(actor, criteria);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
```

- [ ] **Step 3: Add lifecycle endpoint to ReviewCaseController**

Add new field and constructor parameter:
```java
private final GetCaseLifecycleUseCase getCaseLifecycleUseCase;
```

Add endpoint:
```java
@GetMapping("/{caseId}/lifecycle")
public ResponseEntity<ApiResponse<List<AuditTrailEntry>>> getCaseLifecycle(
        @CurrentActor ActorContext actor,
        @PathVariable String caseId) {
    List<AuditTrailEntry> entries = getCaseLifecycleUseCase.execute(actor, caseId);
    return ResponseEntity.ok(ApiResponse.ok(entries));
}
```

Required imports:
```java
import com.tzuchi.his.quality.inspection.application.audit.AuditTrailEntry;
import com.tzuchi.his.quality.inspection.application.audit.GetCaseLifecycleUseCase;
```

- [ ] **Step 4: Write AuditApiTest**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.infrastructure.audit.AuditTrailEntity;
import com.tzuchi.his.quality.inspection.infrastructure.audit.AuditTrailJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuditApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuditTrailJpaRepository auditRepo;

    private final String hmToken = TestTokenFactory.hospitalManagerToken();
    private final String reviewerToken = TestTokenFactory.reviewerToken();

    private void seedAudit(String id, String entityType, String entityId, String action) {
        AuditTrailEntity e = new AuditTrailEntity();
        e.setId(id);
        e.setEntityType(entityType);
        e.setEntityId(entityId);
        e.setAction(action);
        e.setActorUserId("u1");
        e.setActorUserName("Test User");
        e.setHospitalId("H001");
        e.setOccurredAt(Instant.now());
        e.setDetail("{}");
        auditRepo.save(e);
    }

    @Test
    void listAuditTrails_returns200() throws Exception {
        seedAudit("a1", "ReviewCase", "c1", "ASSIGN_REVIEWER");

        mockMvc.perform(get("/audit-trails")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void listAuditTrails_reviewer_shouldReturn403() throws Exception {
        mockMvc.perform(get("/audit-trails")
                        .header("api-key", reviewerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCaseLifecycle_returns200() throws Exception {
        seedAudit("a1", "ReviewCase", "c1", "CREATED");
        seedAudit("a2", "ReviewCase", "c1", "ASSIGN_REVIEWER");

        mockMvc.perform(get("/review-cases/c1/lifecycle")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
```

- [ ] **Step 5: Run all tests**

Run: `cd services/quality-inspection-service && ./gradlew clean test`
Expected: All tests PASS

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(B5b): add audit trails, case lifecycle endpoints with tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Phase B5c — Dashboard + Export

---

### Task 12: DashboardQueryService + Test

**Files:**
- Create: `src/main/java/com/tzuchi/his/quality/inspection/reporting/DashboardQueryCriteria.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/reporting/DashboardSummary.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/reporting/DashboardQueryService.java`
- Create: `src/test/java/com/tzuchi/his/quality/inspection/reporting/DashboardQueryServiceTest.java`

- [ ] **Step 1: Create DashboardQueryCriteria**

```java
package com.tzuchi.his.quality.inspection.reporting;

import java.time.Instant;

public class DashboardQueryCriteria {

    private final String hospitalId;
    private final String projectId;
    private final Instant from;
    private final Instant to;

    public DashboardQueryCriteria(String hospitalId, String projectId, Instant from, Instant to) {
        this.hospitalId = hospitalId;
        this.projectId = projectId;
        this.from = from;
        this.to = to;
    }

    public String getHospitalId() { return hospitalId; }
    public String getProjectId() { return projectId; }
    public Instant getFrom() { return from; }
    public Instant getTo() { return to; }
}
```

- [ ] **Step 2: Create DashboardSummary**

```java
package com.tzuchi.his.quality.inspection.reporting;

public class DashboardSummary {

    private final long totalCases;
    private final long closedCases;
    private final long rereviewedCases;
    private final long overdueCases;
    private final double completionRate;
    private final double rereviewRate;
    private final double overdueRate;

    public DashboardSummary(long totalCases, long closedCases,
                             long rereviewedCases, long overdueCases) {
        this.totalCases = totalCases;
        this.closedCases = closedCases;
        this.rereviewedCases = rereviewedCases;
        this.overdueCases = overdueCases;
        this.completionRate = totalCases > 0 ? (double) closedCases / totalCases : 0.0;
        this.rereviewRate = totalCases > 0 ? (double) rereviewedCases / totalCases : 0.0;
        this.overdueRate = totalCases > 0 ? (double) overdueCases / totalCases : 0.0;
    }

    public long getTotalCases() { return totalCases; }
    public long getClosedCases() { return closedCases; }
    public long getRereviewedCases() { return rereviewedCases; }
    public long getOverdueCases() { return overdueCases; }
    public double getCompletionRate() { return completionRate; }
    public double getRereviewRate() { return rereviewRate; }
    public double getOverdueRate() { return overdueRate; }
}
```

- [ ] **Step 3: Write DashboardQueryServiceTest**

```java
package com.tzuchi.his.quality.inspection.reporting;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DashboardQueryServiceTest {

    @Test
    void zeroTotalCases_allRatesAreZero() {
        DashboardSummary s = new DashboardSummary(0, 0, 0, 0);
        assertEquals(0.0, s.getCompletionRate());
        assertEquals(0.0, s.getRereviewRate());
        assertEquals(0.0, s.getOverdueRate());
    }

    @Test
    void ratesComputedCorrectly() {
        DashboardSummary s = new DashboardSummary(10, 5, 3, 2);
        assertEquals(0.5, s.getCompletionRate(), 0.001);
        assertEquals(0.3, s.getRereviewRate(), 0.001);
        assertEquals(0.2, s.getOverdueRate(), 0.001);
    }

    @Test
    void reviewer_shouldThrowUnauthorized() {
        DashboardQueryService service = new DashboardQueryService(null);
        ActorContext actor = new ActorContext("r1", "Reviewer", EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));
        DashboardQueryCriteria criteria = new DashboardQueryCriteria(null, null, null, null);

        assertThrows(UnauthorizedException.class, () -> service.getSummary(actor, criteria));
    }
}
```

- [ ] **Step 4: Run test — expect FAIL**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "*.DashboardQueryServiceTest" 2>&1 | tail -10`

- [ ] **Step 5: Create DashboardQueryService**

```java
package com.tzuchi.his.quality.inspection.reporting;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardQueryService {

    private final EntityManager em;

    public DashboardQueryService(EntityManager em) {
        this.em = em;
    }

    public DashboardSummary getSummary(ActorContext actor, DashboardQueryCriteria criteria) {
        actor.requireAnyRole(DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);

        String whereClause = buildWhereClause(criteria);
        List<Object> params = buildParams(criteria);

        long total = countWithCondition("", whereClause, params);
        long closed = countWithCondition("AND e.status = 'CLOSED'", whereClause, params);
        long rereviewed = countWithCondition("AND e.rereviewerId IS NOT NULL", whereClause, params);
        long overdue = countWithCondition(
                "AND e.dueAt < CURRENT_TIMESTAMP AND e.status NOT IN ('CLOSED', 'CANCELLED')",
                whereClause, params);

        return new DashboardSummary(total, closed, rereviewed, overdue);
    }

    private long countWithCondition(String extraCondition, String whereClause,
                                     List<Object> params) {
        String jpql = "SELECT COUNT(e) FROM ReviewCaseEntity e WHERE 1=1 "
                + whereClause + " " + extraCondition;
        Query q = em.createQuery(jpql);
        for (int i = 0; i < params.size(); i++) {
            q.setParameter(i + 1, params.get(i));
        }
        return ((Number) q.getSingleResult()).longValue();
    }

    private String buildWhereClause(DashboardQueryCriteria criteria) {
        StringBuilder sb = new StringBuilder();
        int paramIndex = 1;
        if (criteria.getHospitalId() != null) {
            sb.append("AND e.hospitalId = ?").append(paramIndex);
        }
        if (criteria.getProjectId() != null) {
            sb.append("AND e.projectId = ?").append(paramIndex + (criteria.getHospitalId() != null ? 1 : 0));
        }
        return sb.toString();
    }

    private List<Object> buildParams(DashboardQueryCriteria criteria) {
        List<Object> params = new ArrayList<>();
        if (criteria.getHospitalId() != null) {
            params.add(criteria.getHospitalId());
        }
        if (criteria.getProjectId() != null) {
            params.add(criteria.getProjectId());
        }
        return params;
    }
}
```

- [ ] **Step 6: Run test — expect PASS**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "*.DashboardQueryServiceTest"`
Expected: All 3 tests PASS

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat(B5c): add DashboardQueryService, DashboardSummary, DashboardQueryCriteria with tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 13: ExportQueryService + Test

**Files:**
- Create: `src/main/java/com/tzuchi/his/quality/inspection/reporting/ExportCriteria.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/reporting/ExportRow.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/reporting/ExportResult.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/reporting/ExportQueryService.java`
- Create: `src/test/java/com/tzuchi/his/quality/inspection/reporting/ExportQueryServiceTest.java`

- [ ] **Step 1: Create ExportCriteria**

```java
package com.tzuchi.his.quality.inspection.reporting;

import java.time.Instant;

public class ExportCriteria {

    private final String reportType;
    private final String hospitalId;
    private final Instant from;
    private final Instant to;

    public ExportCriteria(String reportType, String hospitalId, Instant from, Instant to) {
        this.reportType = reportType;
        this.hospitalId = hospitalId;
        this.from = from;
        this.to = to;
    }

    public String getReportType() { return reportType; }
    public String getHospitalId() { return hospitalId; }
    public Instant getFrom() { return from; }
    public Instant getTo() { return to; }
}
```

- [ ] **Step 2: Create ExportRow**

```java
package com.tzuchi.his.quality.inspection.reporting;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExportRow {

    private final String caseId;
    private final String projectId;
    private final String hospitalId;
    private final String status;
    private final String reviewerId;
    private final String rereviewerId;
    private final BigDecimal initialScore;
    private final BigDecimal finalScore;
    private final LocalDateTime createdAt;
    private final LocalDateTime closedAt;

    public ExportRow(String caseId, String projectId, String hospitalId, String status,
                      String reviewerId, String rereviewerId, BigDecimal initialScore,
                      BigDecimal finalScore, LocalDateTime createdAt, LocalDateTime closedAt) {
        this.caseId = caseId;
        this.projectId = projectId;
        this.hospitalId = hospitalId;
        this.status = status;
        this.reviewerId = reviewerId;
        this.rereviewerId = rereviewerId;
        this.initialScore = initialScore;
        this.finalScore = finalScore;
        this.createdAt = createdAt;
        this.closedAt = closedAt;
    }

    public String getCaseId() { return caseId; }
    public String getProjectId() { return projectId; }
    public String getHospitalId() { return hospitalId; }
    public String getStatus() { return status; }
    public String getReviewerId() { return reviewerId; }
    public String getRereviewerId() { return rereviewerId; }
    public BigDecimal getInitialScore() { return initialScore; }
    public BigDecimal getFinalScore() { return finalScore; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
}
```

- [ ] **Step 3: Create ExportResult**

```java
package com.tzuchi.his.quality.inspection.reporting;

import java.util.List;

public class ExportResult {

    private final String reportType;
    private final List<ExportRow> rows;
    private final DashboardSummary summary;

    public ExportResult(String reportType, List<ExportRow> rows, DashboardSummary summary) {
        this.reportType = reportType;
        this.rows = rows;
        this.summary = summary;
    }

    public String getReportType() { return reportType; }
    public List<ExportRow> getRows() { return rows; }
    public DashboardSummary getSummary() { return summary; }
}
```

- [ ] **Step 4: Write ExportQueryServiceTest**

```java
package com.tzuchi.his.quality.inspection.reporting;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.interfaces.rest.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ExportQueryServiceTest {

    @Test
    void invalidReportType_shouldThrowBadRequest() {
        ExportQueryService service = new ExportQueryService(null, null);
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
        ExportCriteria criteria = new ExportCriteria("INVALID", null, null, null);

        assertThrows(BadRequestException.class, () -> service.export(actor, criteria));
    }

    @Test
    void reviewer_shouldThrowUnauthorized() {
        ExportQueryService service = new ExportQueryService(null, null);
        ActorContext actor = new ActorContext("r1", "Reviewer", EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));
        ExportCriteria criteria = new ExportCriteria("REVIEW_COMPLETION", null, null, null);

        assertThrows(UnauthorizedException.class, () -> service.export(actor, criteria));
    }
}
```

- [ ] **Step 5: Run test — expect FAIL**

- [ ] **Step 6: Create ExportQueryService**

```java
package com.tzuchi.his.quality.inspection.reporting;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.interfaces.rest.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ExportQueryService {

    private final EntityManager em;
    private final DashboardQueryService dashboardQueryService;

    public ExportQueryService(EntityManager em, DashboardQueryService dashboardQueryService) {
        this.em = em;
        this.dashboardQueryService = dashboardQueryService;
    }

    public ExportResult export(ActorContext actor, ExportCriteria criteria) {
        actor.requireAnyRole(DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);

        if (!"REVIEW_COMPLETION".equals(criteria.getReportType())) {
            throw new BadRequestException("Unsupported reportType: " + criteria.getReportType());
        }

        List<ExportRow> rows = queryRows(criteria);
        DashboardQueryCriteria dashCriteria = new DashboardQueryCriteria(
                criteria.getHospitalId(), null, criteria.getFrom(), criteria.getTo());
        DashboardSummary summary = dashboardQueryService.getSummary(actor, dashCriteria);

        return new ExportResult(criteria.getReportType(), rows, summary);
    }

    private List<ExportRow> queryRows(ExportCriteria criteria) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ReviewCaseEntity> cq = cb.createQuery(ReviewCaseEntity.class);
        Root<ReviewCaseEntity> root = cq.from(ReviewCaseEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        if (criteria.getHospitalId() != null) {
            predicates.add(cb.equal(root.get("hospitalId"), criteria.getHospitalId()));
        }
        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.asc(root.get("caseId")));

        TypedQuery<ReviewCaseEntity> q = em.createQuery(cq);
        q.setMaxResults(10000);

        return q.getResultList().stream()
                .map(e -> new ExportRow(
                        e.getCaseId(), e.getProjectId(), e.getHospitalId(),
                        e.getStatus().name(), e.getReviewerId(), e.getRereviewerId(),
                        e.getInitialReviewScore(), e.getFinalScore(),
                        null, e.getClosedAt()))
                .collect(Collectors.toList());
    }
}
```

- [ ] **Step 7: Run test — expect PASS**

Run: `cd services/quality-inspection-service && ./gradlew test --tests "*.ExportQueryServiceTest"`
Expected: All 2 tests PASS

- [ ] **Step 8: Commit**

```bash
git add -A && git commit -m "feat(B5c): add ExportQueryService, ExportResult, ExportRow, ExportCriteria with tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 14: ReportController + API tests

**Files:**
- Create: `src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/ReportController.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/dto/ExportRequest.java`
- Create: `src/test/java/com/tzuchi/his/quality/inspection/interfaces/rest/ReportApiTest.java`

- [ ] **Step 1: Create ExportRequest DTO**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.constraints.NotBlank;
import java.time.Instant;

public class ExportRequest {

    @NotBlank
    private String reportType;
    private String hospitalId;
    private Instant from;
    private Instant to;

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
    public Instant getFrom() { return from; }
    public void setFrom(Instant from) { this.from = from; }
    public Instant getTo() { return to; }
    public void setTo(Instant to) { this.to = to; }
}
```

- [ ] **Step 2: Create ReportController**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ApiResponse;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ExportRequest;
import com.tzuchi.his.quality.inspection.reporting.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Instant;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final DashboardQueryService dashboardQueryService;
    private final ExportQueryService exportQueryService;

    public ReportController(DashboardQueryService dashboardQueryService,
                             ExportQueryService exportQueryService) {
        this.dashboardQueryService = dashboardQueryService;
        this.exportQueryService = exportQueryService;
    }

    @GetMapping("/dashboard-summary")
    public ResponseEntity<ApiResponse<DashboardSummary>> getDashboardSummary(
            @CurrentActor ActorContext actor,
            @RequestParam(required = false) String hospitalId,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        DashboardQueryCriteria criteria = new DashboardQueryCriteria(hospitalId, projectId, from, to);
        DashboardSummary result = dashboardQueryService.getSummary(actor, criteria);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/export")
    public ResponseEntity<ApiResponse<ExportResult>> exportReport(
            @CurrentActor ActorContext actor,
            @Valid @RequestBody ExportRequest request) {
        ExportCriteria criteria = new ExportCriteria(
                request.getReportType(), request.getHospitalId(),
                request.getFrom(), request.getTo());
        ExportResult result = exportQueryService.export(actor, criteria);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
```

- [ ] **Step 3: Write ReportApiTest**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseJpaRepository;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ExportRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReportApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ReviewCaseJpaRepository caseRepo;

    private final String hmToken = TestTokenFactory.hospitalManagerToken();

    private void seedCase(String caseId, CaseStatus status) {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(caseId);
        e.setBatchId("b1");
        e.setProjectId("p1");
        e.setHospitalId("H001");
        e.setSourceCaseKey("SK-001");
        e.setMedicalRecordType("OUTPATIENT");
        e.setStatus(status);
        caseRepo.save(e);
    }

    @Test
    void dashboardSummary_returns200() throws Exception {
        seedCase("c1", CaseStatus.CLOSED);
        seedCase("c2", CaseStatus.PENDING_INITIAL_REVIEW);

        mockMvc.perform(get("/reports/dashboard-summary")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCases").value(2))
                .andExpect(jsonPath("$.data.closedCases").value(1));
    }

    @Test
    void exportReport_validType_returns200() throws Exception {
        seedCase("c1", CaseStatus.CLOSED);

        ExportRequest req = new ExportRequest();
        req.setReportType("REVIEW_COMPLETION");

        mockMvc.perform(post("/reports/export")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportType").value("REVIEW_COMPLETION"))
                .andExpect(jsonPath("$.data.rows").isArray());
    }

    @Test
    void exportReport_invalidType_returns400() throws Exception {
        ExportRequest req = new ExportRequest();
        req.setReportType("INVALID");

        mockMvc.perform(post("/reports/export")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 4: Run all tests**

Run: `cd services/quality-inspection-service && ./gradlew clean test`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(B5c): add ReportController with dashboard-summary and export endpoints, API tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Final Verification

---

### Task 15: Full test suite + cleanup

- [ ] **Step 1: Run full test suite**

Run: `cd services/quality-inspection-service && ./gradlew clean test`
Expected: All tests PASS (60 existing + ~25 new ≈ 85+ tests)

- [ ] **Step 2: Verify endpoint count**

Run: `cd services/quality-inspection-service && grep -rn "@GetMapping\|@PostMapping\|@PutMapping" src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/ | wc -l`
Expected: Count increased by 7 from baseline

- [ ] **Step 3: Final commit (if any cleanup needed)**

```bash
git add -A && git commit -m "chore(B5): final cleanup and verification

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Summary

| Phase | Tasks | New Files | New Tests | Endpoints |
|-------|-------|-----------|-----------|-----------|
| B5a | 1-8 | ~12 | ~14 scenarios | GET /review-cases, GET /review-cases/{caseId}, GET /review-submissions/{submissionId} |
| B5b | 9-11 | ~7 | ~8 scenarios | GET /audit-trails, GET /review-cases/{caseId}/lifecycle |
| B5c | 12-14 | ~8 | ~8 scenarios | GET /reports/dashboard-summary, POST /reports/export |
| Final | 15 | 0 | 0 | Verification |
