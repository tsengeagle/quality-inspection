package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.infrastructure.audit.AuditTrailJpaRepository;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseJpaRepository;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.submission.ReviewSubmissionEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.submission.ReviewSubmissionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@RecordApplicationEvents
class SaveRereviewDraftIntegrityIntegrationTest {
    @Autowired private SaveRereviewDraftUseCase useCase;
    @Autowired private ReviewCaseJpaRepository cases;
    @Autowired private ReviewSubmissionJpaRepository submissions;
    @Autowired private AuditTrailJpaRepository audit;
    @Autowired private ObjectMapper mapper;
    @Autowired private ApplicationEvents events;

    @BeforeEach
    void seed() {
        audit.deleteAll();
        submissions.deleteAll();
        cases.deleteAll();
        cases.save(caseEntity("c1", "H001"));
        cases.save(caseEntity("c2", "H001"));
        cases.save(caseEntity("c3", "H002"));
        submissions.save(draft("s1", "c1"));
        submissions.save(draft("s2", "c2"));
        submissions.save(draft("s3", "c3"));
    }

    private ReviewCaseEntity caseEntity(String id, String hospital) {
        ReviewCaseEntity rc = new ReviewCaseEntity();
        rc.setCaseId(id);
        rc.setBatchId("batch");
        rc.setProjectId("project");
        rc.setHospitalId(hospital);
        rc.setStatus(CaseStatus.REREVIEW_IN_PROGRESS);
        rc.setRereviewerId("doctor");
        rc.setRereviewerAssignedAt(Instant.parse("2026-01-01T00:00:00Z"));
        rc.setLatestRereviewSubmissionId("s" + id.substring(1));
        rc.setInitialReviewScore(new BigDecimal("90"));
        rc.setFinalScore(new BigDecimal("80"));
        return rc;
    }

    private ReviewSubmissionEntity draft(String id, String caseId) {
        ReviewSubmissionEntity sub = new ReviewSubmissionEntity();
        sub.setSubmissionId(id);
        sub.setCaseId(caseId);
        sub.setStage(ReviewStage.REREVIEW);
        sub.setReviewerId("doctor");
        sub.setStatus(SubmissionStatus.DRAFT);
        sub.setRevisionNo(1);
        sub.setAnswerSet(Collections.singletonList(AnswerItem.ofRaw("Q1", "old")));
        sub.setTotalScore(new BigDecimal("12"));
        sub.setComment("old comment");
        sub.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        sub.setUpdatedAt(sub.getCreatedAt());
        return sub;
    }

    private ActorContext actor() {
        return new ActorContext("doctor", "Doctor", EnumSet.of(DomainRole.REREVIEWER), Optional.of("H001"));
    }

    private static final List<AnswerInput> CHANGED_ANSWERS = Collections.singletonList(new AnswerInput("Q1", "changed"));

    @ParameterizedTest
    @ValueSource(strings = {"otherCase", "otherHospitalSubmission", "otherHospitalCase", "initialStage",
            "otherOwner", "submitted", "superseded", "stale", "missingPointer", "successor",
            "SELECTED_PENDING_ASSIGNMENT", "PENDING_INITIAL_REVIEW", "INITIAL_REVIEW_IN_PROGRESS",
            "INITIAL_REVIEW_COMPLETED", "RETURNED", "PENDING_REREVIEW", "CLOSED", "CANCELLED"})
    void illegalDraftWrite_rejectsWithoutPersistedChangesOrEvents(String scenario) throws Exception {
        String[] target = {"c1", "s1"}; // caseId, submissionId
        ReviewSubmissionEntity sub = submissions.findById("s1").orElseThrow();
        ReviewCaseEntity rc = cases.findById("c1").orElseThrow();
        if (scenario.equals("otherCase")) target[1] = "s2";
        if (scenario.equals("otherHospitalSubmission")) target[1] = "s3";
        if (scenario.equals("otherHospitalCase")) {
            target[0] = "c3";
            target[1] = "s3";
        }
        if (scenario.equals("initialStage")) sub.setStage(ReviewStage.INITIAL);
        if (scenario.equals("otherOwner")) sub.setReviewerId("other-doctor");
        if (scenario.equals("submitted")) sub.setStatus(SubmissionStatus.SUBMITTED);
        if (scenario.equals("superseded")) {
            sub.setStatus(SubmissionStatus.SUPERSEDED);
            sub.setSupersededBySubmissionId("s-new");
        }
        if (scenario.equals("stale")) {
            submissions.save(draft("s-new", "c1"));
            rc.setLatestRereviewSubmissionId("s-new");
        }
        if (scenario.equals("missingPointer")) rc.setLatestRereviewSubmissionId(null);
        if (scenario.equals("successor")) sub.setSupersededBySubmissionId("s-new");
        for (CaseStatus status : CaseStatus.values()) {
            if (scenario.equals(status.name())) rc.setStatus(status);
        }
        submissions.save(sub);
        cases.save(rc);
        Map<String, String> beforeSubmissions = submissionSnapshot();
        Map<String, String> beforeCases = caseSnapshot();
        long beforeEvents = events.stream(DomainEvent.class).count();
        long beforeAudit = audit.count();

        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> useCase.execute(actor(), target[0], target[1], CHANGED_ANSWERS, "changed comment"));
        // D-069 第 3 點：案件狀態不對是 DomainRuleViolationException（CASE_NOT_IN_REREVIEW_STAGE）
        assertTrue(failure instanceof DomainStateConflictException || failure instanceof DomainRuleViolationException
                        || failure instanceof AccessDeniedException,
                "Expected an explicit integrity/authorization guard: " + failure);
        assertEquals(beforeSubmissions, submissionSnapshot());
        assertEquals(beforeCases, caseSnapshot());
        assertEquals(beforeEvents, events.stream(DomainEvent.class).count());
        assertEquals(beforeAudit, audit.count());
    }

    @Test
    void currentOwnedDraft_savesAnswersAndCommentAndKeepsScore() throws Exception {
        Map<String, String> beforeCases = caseSnapshot();
        SaveDraftResult result = useCase.execute(actor(), "c1", "s1", CHANGED_ANSWERS, "changed comment");
        ReviewSubmissionEntity saved = submissions.findById("s1").orElseThrow();
        assertEquals("DRAFT", result.getStatus());
        assertEquals(SubmissionStatus.DRAFT, saved.getStatus());
        assertEquals(ReviewStage.REREVIEW, saved.getStage());
        assertEquals("changed", saved.getAnswerSet().get(0).getAnswerValue());
        // D-069 第 7 點：分數不是草稿的輸入，維持原值
        assertEquals(0, new BigDecimal("12").compareTo(saved.getTotalScore()));
        assertEquals("changed comment", saved.getComment());
        // D-037：草稿儲存是案件的業務更新，只推進案件的 updatedAt 與版本，其他案件欄位不變。
        assertEquals(withoutCaseTimeAndVersion(beforeCases), withoutCaseTimeAndVersion(caseSnapshot()));
        assertNotNull(cases.findById("c1").orElseThrow().getUpdatedAt());
    }

    private Map<String, String> withoutCaseTimeAndVersion(Map<String, String> snapshot) throws Exception {
        Map<String, String> result = new TreeMap<>();
        for (Map.Entry<String, String> entry : snapshot.entrySet()) {
            com.fasterxml.jackson.databind.node.ObjectNode node =
                    (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(entry.getValue());
            node.remove("version");
            node.remove("updatedAt");
            result.put(entry.getKey(), mapper.writeValueAsString(node));
        }
        return result;
    }

    private Map<String, String> submissionSnapshot() throws Exception {
        Map<String, String> snapshot = new TreeMap<>();
        for (ReviewSubmissionEntity sub : submissions.findAll()) {
            snapshot.put(sub.getSubmissionId(), mapper.writeValueAsString(sub));
        }
        return snapshot;
    }

    private Map<String, String> caseSnapshot() throws Exception {
        Map<String, String> snapshot = new TreeMap<>();
        for (ReviewCaseEntity rc : cases.findAll()) {
            snapshot.put(rc.getCaseId(), mapper.writeValueAsString(rc));
        }
        return snapshot;
    }
}
