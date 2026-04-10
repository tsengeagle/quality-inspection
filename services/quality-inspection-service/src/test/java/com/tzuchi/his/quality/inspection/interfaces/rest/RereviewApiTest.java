package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RereviewApiTest {

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
    private static final String HOSPITAL_ID = "H001";
    private static final String REVIEWER_ID = "reviewer-1";
    private static final String REREVIEWER_ID = "rereviewer-1";

    private final String hmToken = TestTokenFactory.hospitalManagerToken();
    private final String reviewerToken = TestTokenFactory.reviewerToken(REVIEWER_ID, HOSPITAL_ID);
    private final String rereviewerToken = TestTokenFactory.rereviewerToken(REREVIEWER_ID, HOSPITAL_ID);

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
        proj.setQualityThreshold(new BigDecimal("60.0"));
        proj.setInitialReviewWeight(new BigDecimal("0.5"));
        proj.setRereviewWeight(new BigDecimal("0.5"));
        projectJpa.save(proj);
    }

    private void seedCaseInInitialReviewCompleted() {
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
        e.setLatestInitialSubmissionId("init-sub-1");
        e.setScoreSheetVersionRef(SCORE_SHEET_ID);
        e.setInitialReviewScore(new BigDecimal("17.5"));
        caseJpa.save(e);

        ReviewSubmissionEntity initSub = new ReviewSubmissionEntity();
        initSub.setSubmissionId("init-sub-1");
        initSub.setCaseId(CASE_ID);
        initSub.setStage(ReviewStage.INITIAL);
        initSub.setReviewerId(REVIEWER_ID);
        initSub.setStatus(SubmissionStatus.SUBMITTED);
        initSub.setRevisionNo(1);
        initSub.setAnswerSet(Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.0")),
                AnswerItem.ofScored("Q002", "15", new BigDecimal("7.5"))
        ));
        initSub.setTotalScore(new BigDecimal("17.5"));
        initSub.setBasedOnScoreSheetVersionId(SCORE_SHEET_ID);
        initSub.setSubmittedAt(LocalDateTime.now());
        initSub.setCreatedAt(LocalDateTime.now());
        initSub.setUpdatedAt(LocalDateTime.now());
        submissionJpa.save(initSub);
    }

    private void seedCaseInPendingRereview() {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(CASE_ID);
        e.setBatchId("b1");
        e.setProjectId(PROJECT_ID);
        e.setHospitalId(HOSPITAL_ID);
        e.setSourceCaseKey("SK-001");
        e.setMedicalRecordType("OUTPATIENT");
        e.setStatus(CaseStatus.PENDING_REREVIEW);
        e.setReviewerId(REVIEWER_ID);
        e.setReviewerAssignedAt(LocalDateTime.now());
        e.setDueAt(LocalDateTime.of(2026, 2, 1, 23, 59));
        e.setRereviewRequired(true);
        e.setLatestInitialSubmissionId("init-sub-1");
        e.setScoreSheetVersionRef(SCORE_SHEET_ID);
        e.setInitialReviewScore(new BigDecimal("17.5"));
        caseJpa.save(e);

        ReviewSubmissionEntity initSub = new ReviewSubmissionEntity();
        initSub.setSubmissionId("init-sub-1");
        initSub.setCaseId(CASE_ID);
        initSub.setStage(ReviewStage.INITIAL);
        initSub.setReviewerId(REVIEWER_ID);
        initSub.setStatus(SubmissionStatus.SUBMITTED);
        initSub.setRevisionNo(1);
        initSub.setAnswerSet(Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.0")),
                AnswerItem.ofScored("Q002", "15", new BigDecimal("7.5"))
        ));
        initSub.setTotalScore(new BigDecimal("17.5"));
        initSub.setBasedOnScoreSheetVersionId(SCORE_SHEET_ID);
        initSub.setSubmittedAt(LocalDateTime.now());
        initSub.setCreatedAt(LocalDateTime.now());
        initSub.setUpdatedAt(LocalDateTime.now());
        submissionJpa.save(initSub);
    }

    private void seedCaseInPendingRereviewWithAssignment() {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(CASE_ID);
        e.setBatchId("b1");
        e.setProjectId(PROJECT_ID);
        e.setHospitalId(HOSPITAL_ID);
        e.setSourceCaseKey("SK-001");
        e.setMedicalRecordType("OUTPATIENT");
        e.setStatus(CaseStatus.PENDING_REREVIEW);
        e.setReviewerId(REVIEWER_ID);
        e.setReviewerAssignedAt(LocalDateTime.now());
        e.setDueAt(LocalDateTime.of(2026, 2, 1, 23, 59));
        e.setRereviewRequired(true);
        e.setRereviewerId(REREVIEWER_ID);
        e.setRereviewerAssignedAt(LocalDateTime.now());
        e.setRereviewerDueAt(LocalDateTime.of(2026, 6, 30, 23, 59));
        e.setLatestInitialSubmissionId("init-sub-1");
        e.setScoreSheetVersionRef(SCORE_SHEET_ID);
        e.setInitialReviewScore(new BigDecimal("17.5"));
        caseJpa.save(e);

        ReviewSubmissionEntity initSub = new ReviewSubmissionEntity();
        initSub.setSubmissionId("init-sub-1");
        initSub.setCaseId(CASE_ID);
        initSub.setStage(ReviewStage.INITIAL);
        initSub.setReviewerId(REVIEWER_ID);
        initSub.setStatus(SubmissionStatus.SUBMITTED);
        initSub.setRevisionNo(1);
        initSub.setAnswerSet(Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.0")),
                AnswerItem.ofScored("Q002", "15", new BigDecimal("7.5"))
        ));
        initSub.setTotalScore(new BigDecimal("17.5"));
        initSub.setBasedOnScoreSheetVersionId(SCORE_SHEET_ID);
        initSub.setSubmittedAt(LocalDateTime.now());
        initSub.setCreatedAt(LocalDateTime.now());
        initSub.setUpdatedAt(LocalDateTime.now());
        submissionJpa.save(initSub);
    }

    // --- TEST 1: FULL REREVIEW FLOW ---

    @Test
    void fullRereviewFlow() throws Exception {
        // Seed PENDING_REREVIEW case
        seedCaseInPendingRereview();

        // Step 1: Assign rereviewer
        String assignBody = "{\"rereviewerId\":\"" + REREVIEWER_ID + 
                "\",\"dueAt\":\"2026-06-30T23:59:00\"}";
        mockMvc.perform(post(BASE + "/" + CASE_ID + "/assign-rereviewer")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Step 2: Start rereview (as assigned rereviewer)
        mockMvc.perform(post(BASE + "/" + CASE_ID + "/start-rereview")
                        .header("api-key", rereviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Step 3: Query for the auto-created rereview submission
        List<ReviewSubmissionEntity> rereviewSubs = 
                submissionJpa.findByCaseIdAndStage(CASE_ID, ReviewStage.REREVIEW);
        if (rereviewSubs.isEmpty()) {
            throw new AssertionError("Rereview submission not found after start-rereview");
        }
        String submissionId = rereviewSubs.get(0).getSubmissionId();

        // Step 4: Save draft
        String draftBody = "{\"submissionId\":\"" + submissionId + 
                "\",\"answers\":[" +
                "{\"itemId\":\"Q001\",\"answerValue\":\"true\"}," +
                "{\"itemId\":\"Q002\",\"answerValue\":\"18\"}" +
                "],\"totalScore\":19.0,\"comment\":\"draft comment\"}";
        mockMvc.perform(put(BASE + "/" + CASE_ID + "/rereview-draft")
                        .header("api-key", rereviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Step 5: Submit rereview (with real calculated answers)
        String submitBody = "{\"submissionId\":\"" + submissionId + 
                "\",\"answers\":[" +
                "{\"itemId\":\"Q001\",\"answerValue\":\"true\"}," +
                "{\"itemId\":\"Q002\",\"answerValue\":\"18\"}" +
                "],\"comment\":\"submit comment\"}";
        mockMvc.perform(post(BASE + "/" + CASE_ID + "/submit-rereview")
                        .header("api-key", rereviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Refresh submissionId (now points to the submitted revision)
        String submittedSubmissionId = submissionId;

        // Step 6: Revise rereview (refer to the submitted submission)
        String reviseBody = "{\"submissionId\":\"" + submittedSubmissionId + 
                "\",\"answers\":[" +
                "{\"itemId\":\"Q001\",\"answerValue\":\"true\"}," +
                "{\"itemId\":\"Q002\",\"answerValue\":\"20\"}" +
                "],\"comment\":\"revised comment\"}";
        mockMvc.perform(post(BASE + "/" + CASE_ID + "/revise-rereview")
                        .header("api-key", rereviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviseBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Step 7: Get rereview revisions
        mockMvc.perform(get(BASE + "/" + CASE_ID + "/rereview-revisions")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.caseId").value(CASE_ID))
                .andExpect(jsonPath("$.data.submissions").isArray())
                .andExpect(jsonPath("$.data.submissions.length()").value(greaterThanOrEqualTo(2)));
    }

    // --- TEST 2: CLOSE WITHOUT REREVIEW ---

    @Test
    void closeWithoutRereview_happyPath() throws Exception {
        seedCaseInInitialReviewCompleted();

        mockMvc.perform(post(BASE + "/" + CASE_ID + "/close-without-rereview")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify case is now CLOSED
        ReviewCaseEntity updated = caseJpa.findById(CASE_ID).orElseThrow();
        if (updated.getStatus() != CaseStatus.CLOSED) {
            throw new AssertionError("Case should be CLOSED after close-without-rereview");
        }
        if (updated.getFinalScore() == null || 
            updated.getFinalScore().compareTo(new BigDecimal("17.5")) != 0) {
            throw new AssertionError("FinalScore should equal initialReviewScore (17.5), but was: " + 
                    updated.getFinalScore());
        }
    }

    // --- TEST 3: SEND TO REREVIEW (MANUAL ESCALATION) ---

    @Test
    void sendToRereview_manualEscalation() throws Exception {
        seedCaseInInitialReviewCompleted();

        mockMvc.perform(post(BASE + "/" + CASE_ID + "/send-to-rereview")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify case is now PENDING_REREVIEW
        ReviewCaseEntity updated = caseJpa.findById(CASE_ID).orElseThrow();
        if (updated.getStatus() != CaseStatus.PENDING_REREVIEW) {
            throw new AssertionError("Case should be PENDING_REREVIEW after send-to-rereview");
        }
    }

    // --- TEST 4: ASSIGN REREVIEWER SAME AS INITIAL REVIEWER → 422 ---

    @Test
    void assignRereviewer_sameAsInitialReviewer_shouldReturn422() throws Exception {
        seedCaseInPendingRereview();

        String assignBody = "{\"rereviewerId\":\"" + REVIEWER_ID + 
                "\",\"dueAt\":\"2026-06-30T23:59:00\"}";
        mockMvc.perform(post(BASE + "/" + CASE_ID + "/assign-rereviewer")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("REREVIEWER_SAME_AS_REVIEWER"));
    }

    // --- TEST 5: START REREVIEW WRONG ACTOR → 422 ---

    @Test
    void startRereview_wrongActor_shouldReturn422() throws Exception {
        seedCaseInPendingRereviewWithAssignment();

        String wrongToken = TestTokenFactory.rereviewerToken("wrong-user", HOSPITAL_ID);
        mockMvc.perform(post(BASE + "/" + CASE_ID + "/start-rereview")
                        .header("api-key", wrongToken))
                .andExpect(status().isUnprocessableEntity());
    }
}
