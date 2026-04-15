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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
            void getCaseDetail_returnsInitialBootstrapContextForReviewer() throws Exception {
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.initialReviewContext.pageMode").value("EDIT_DRAFT"))
                .andExpect(jsonPath("$.data.initialReviewContext.hasDraft").value(true))
                .andExpect(jsonPath("$.data.initialReviewContext.hasSubmittedBase").value(false))
                .andExpect(jsonPath("$.data.initialReviewContext.scoreSheetVersionId").value("ss-init-v1"))
                .andExpect(jsonPath("$.data.initialReviewContext.currentSubmission.submissionId").value("sub-init-draft"))
                .andExpect(jsonPath("$.data.initialReviewContext.allowedActions[0]").value("SAVE_INITIAL_REVIEW_DRAFT"))
                .andExpect(jsonPath("$.data.initialReviewContext.allowedActions[1]").value("SUBMIT_INITIAL_REVIEW"));
            }

            @Test
            void getCaseDetail_returnsClosedRereviewExceptionContextForRereviewer() throws Exception {
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
                .andExpect(jsonPath("$.data.rereviewContext.pageMode").value("REVISION_EXCEPTION_CLOSED"))
                .andExpect(jsonPath("$.data.rereviewContext.closedRevisionException").value(true))
                .andExpect(jsonPath("$.data.rereviewContext.basedOnVersion.submissionId").value("sub-rereview-1"))
                .andExpect(jsonPath("$.data.rereviewContext.initialSubmissionId").value("sub-init-1"))
                .andExpect(jsonPath("$.data.rereviewContext.initialReviewerId").value("reviewer-1"))
                .andExpect(jsonPath("$.data.rereviewContext.finalScore").value(85))
                .andExpect(jsonPath("$.data.rereviewContext.finalConclusion").value("FINAL_PASS"))
                .andExpect(jsonPath("$.data.rereviewContext.allowedActions[0]").value("REVISE_REREVIEW"));
            }

    @Test
    void getCaseDetail_notFound_returns404() throws Exception {
        mockMvc.perform(get("/review-cases/missing")
                        .header("api-key", hmToken))
                .andExpect(status().isNotFound());
    }
}
