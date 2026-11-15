package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseJpaRepository;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.submission.ReviewSubmissionEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.submission.ReviewSubmissionJpaRepository;
import org.junit.jupiter.api.Test;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.staffdirectory.HisStaffDirectoryEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.staffdirectory.HisStaffDirectoryJpaRepository;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.AssignmentCandidateRole;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReviewCaseQueryApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ReviewCaseJpaRepository caseRepo;
    @Autowired private ReviewSubmissionJpaRepository submissionRepo;
    @Autowired private HisStaffDirectoryJpaRepository staffDirectoryRepo;

    @BeforeEach
    void seedStaffDirectory() {
        staffDirectoryRepo.saveAll(List.of(
                new HisStaffDirectoryEntity("reviewer-1", "Dr. Reviewer One", "H001", AssignmentCandidateRole.REVIEWER, true),
                new HisStaffDirectoryEntity("reviewer-1", "Dr. Reviewer One", "H001", AssignmentCandidateRole.REREVIEWER, true),
                new HisStaffDirectoryEntity("reviewer-2", "Dr. Reviewer Two", "H001", AssignmentCandidateRole.REVIEWER, true),
                new HisStaffDirectoryEntity("rereviewer-1", "Dr. ReReviewer One", "H001", AssignmentCandidateRole.REREVIEWER, true)
        ));
    }

    private final String hmToken = TestTokenFactory.hospitalManagerToken();
    private final String reviewerToken = TestTokenFactory.reviewerToken();
    private final String rereviewerToken = TestTokenFactory.rereviewerToken();

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

    private void seedSubmission(String submissionId, String caseId, ReviewStage stage,
                                String reviewerId, SubmissionStatus status,
                                int revisionNo, BigDecimal totalScore,
                                LocalDateTime submittedAt) {
        ReviewSubmissionEntity e = new ReviewSubmissionEntity();
        e.setSubmissionId(submissionId);
        e.setCaseId(caseId);
        e.setStage(stage);
        e.setReviewerId(reviewerId);
        e.setStatus(status);
        e.setRevisionNo(revisionNo);
        e.setTotalScore(totalScore);
        e.setSubmittedAt(submittedAt);
        submissionRepo.save(e);
    }

    @Test
    void listCases_returnsPagedResult() throws Exception {
        seedCase("c1", CaseStatus.CLOSED);
        seedCase("c2", CaseStatus.PENDING_INITIAL_REVIEW);

        mockMvc.perform(get("/review-cases")
                .param("batchId", "b1")
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
    void getCaseDetail_returnsDetail_D035RejectsUnknown() throws Exception {
        seedCase("c1", CaseStatus.CLOSED);

        mockMvc.perform(get("/review-cases/c1")
                        .header("api-key", hmToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

            @Test
            void getCaseDetail_returnsInitialBootstrapContextForReviewer_D035RejectsUnknown() throws Exception {
            ReviewCaseEntity e = new ReviewCaseEntity();
            e.setCaseId("c1");
            e.setBatchId("b1");
            e.setProjectId("p1");
            e.setHospitalId("H001");
            e.setSourceCaseKey("SK-001");
            e.setMedicalRecordType("OUTPATIENT");
            e.setStatus(CaseStatus.INITIAL_REVIEW_IN_PROGRESS);
            e.setReviewerId("reviewer-1");
            e.setLatestInitialSubmissionId("sub-init-draft");
            e.setScoreSheetVersionRef("ss-init-v1");
            caseRepo.save(e);
            seedSubmission("sub-init-draft", "c1", ReviewStage.INITIAL, "reviewer-1",
                SubmissionStatus.DRAFT, 1, BigDecimal.ZERO, null);

            mockMvc.perform(get("/review-cases/c1")
                    .header("api-key", reviewerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
            }

            @Test
            void getCaseDetail_closedCaseIsReadonlyForRereviewer_D032() throws Exception {
            ReviewCaseEntity e = new ReviewCaseEntity();
            e.setCaseId("c1");
            e.setBatchId("b1");
            e.setProjectId("p1");
            e.setHospitalId("H001");
            e.setSourceCaseKey("SK-001");
            e.setMedicalRecordType("OUTPATIENT");
            e.setStatus(CaseStatus.CLOSED);
            e.setReviewerId("reviewer-1");
            e.setRereviewerId("rereviewer-1");
            e.setLatestInitialSubmissionId("sub-init-1");
            e.setLatestRereviewSubmissionId("sub-rereview-1");
            e.setScoreSheetVersionRef("ss-rereview-v1");
            e.setInitialReviewScore(BigDecimal.valueOf(82));
            e.setFinalScore(BigDecimal.valueOf(85));
            e.setFinalConclusion("FINAL_PASS");
            caseRepo.save(e);
            seedSubmission("sub-init-1", "c1", ReviewStage.INITIAL, "reviewer-1",
                SubmissionStatus.SUBMITTED, 1, BigDecimal.valueOf(82),
                LocalDateTime.of(2026, 4, 1, 9, 0));
            seedSubmission("sub-rereview-1", "c1", ReviewStage.REREVIEW, "rereviewer-1",
                SubmissionStatus.SUBMITTED, 1, BigDecimal.valueOf(88),
                LocalDateTime.of(2026, 4, 2, 10, 0));

            mockMvc.perform(get("/review-cases/c1")
                    .header("api-key", rereviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rereviewContext.pageMode").value("READONLY_BLOCKED"))
                .andExpect(jsonPath("$.data.rereviewContext.closedRevisionException").value(false))
                .andExpect(jsonPath("$.data.rereviewContext.blockedReason").value("CASE_ALREADY_CLOSED"))
                .andExpect(jsonPath("$.data.rereviewContext.basedOnVersion.submissionId").value("sub-rereview-1"))
                .andExpect(jsonPath("$.data.rereviewContext.initialSubmissionId").value("sub-init-1"))
                .andExpect(jsonPath("$.data.rereviewContext.initialReviewerId").value("reviewer-1"))
                .andExpect(jsonPath("$.data.rereviewContext.finalScore").isEmpty())
                .andExpect(jsonPath("$.data.rereviewContext.finalConclusion").isEmpty())
                .andExpect(jsonPath("$.data.rereviewContext.allowedActions").isEmpty());
            }

    @Test
    void getCaseDetail_notFound_returns404() throws Exception {
        mockMvc.perform(get("/review-cases/missing")
                        .header("api-key", hmToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssignmentContext_returnsMixedAssignmentContext() throws Exception {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId("c-assign");
        e.setBatchId("b1");
        e.setProjectId("p1");
        e.setHospitalId("H001");
        e.setSourceCaseKey("SK-001");
        e.setMedicalRecordType("OUTPATIENT");
        e.setStatus(CaseStatus.PENDING_REREVIEW);
        e.setReviewerId("reviewer-1");
        e.setReviewerAssignedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        e.setDueAt(LocalDateTime.of(2026, 4, 10, 23, 59));
        caseRepo.save(e);

        mockMvc.perform(get("/review-cases/c-assign/assignment-context")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.projectName").value("p1"))
            .andExpect(jsonPath("$.data.sourceCaseKey").value("SK-001"))
                .andExpect(jsonPath("$.data.pageMode").value("MIXED_ASSIGNMENT"))
                .andExpect(jsonPath("$.data.allowedActions[0]").value("ASSIGN_REREVIEWER"))
                .andExpect(jsonPath("$.data.currentReviewer.userId").value("reviewer-1"))
                .andExpect(jsonPath("$.data.currentReviewer.displayName").value("Dr. Reviewer One"))
                .andExpect(jsonPath("$.data.reviewerDueAt").value("2026-04-10T23:59:00"));
    }

    @Test
    void getAssignmentContext_closedCase_returnsBlockedContext() throws Exception {
        seedCase("c-blocked", CaseStatus.CLOSED);

        mockMvc.perform(get("/review-cases/c-blocked/assignment-context")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageMode").value("READONLY_BLOCKED"))
                .andExpect(jsonPath("$.data.blocked").value(true))
                .andExpect(jsonPath("$.data.blockedReason").value("CASE_ALREADY_CLOSED"));
    }

    @Test
    void listAssignmentCandidates_returnsSelectableReviewerCandidates() throws Exception {
        seedCase("c-candidate", CaseStatus.SELECTED_PENDING_ASSIGNMENT);

        mockMvc.perform(get("/review-cases/c-candidate/assignment-candidates")
                        .param("role", "REVIEWER")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].role").value("REVIEWER"))
                .andExpect(jsonPath("$.data[0].selectable").value(true));
    }

    @Test
    void listAssignmentCandidates_rereviewerSameAsReviewer_isBlocked() throws Exception {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId("c-rereview");
        e.setBatchId("b1");
        e.setProjectId("p1");
        e.setHospitalId("H001");
        e.setSourceCaseKey("SK-001");
        e.setMedicalRecordType("OUTPATIENT");
        e.setStatus(CaseStatus.PENDING_REREVIEW);
        e.setReviewerId("reviewer-1");
        caseRepo.save(e);

        mockMvc.perform(get("/review-cases/c-rereview/assignment-candidates")
                        .param("role", "REREVIEWER")
                .param("keyword", "Dr. Reviewer One")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value("reviewer-1"))
                .andExpect(jsonPath("$.data[0].selectable").value(false))
                .andExpect(jsonPath("$.data[0].blockedReason").value("REREVIEWER_SAME_AS_REVIEWER"));
    }

    @Test
    void listAssignmentCandidates_invalidRole_returns400() throws Exception {
        seedCase("c-invalid-role", CaseStatus.SELECTED_PENDING_ASSIGNMENT);

        mockMvc.perform(get("/review-cases/c-invalid-role/assignment-candidates")
                        .param("role", "INVALID")
                        .header("api-key", hmToken))
                .andExpect(status().isBadRequest());
    }
}
