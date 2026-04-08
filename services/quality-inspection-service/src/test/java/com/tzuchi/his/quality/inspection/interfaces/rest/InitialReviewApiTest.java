package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
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
import java.util.*;

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
        sub.setAnswerSet(Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.0")),
                AnswerItem.ofScored("Q002", "15", new BigDecimal("7.5"))
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
        sub.setAnswerSet(Collections.singletonList(
                AnswerItem.ofScored("Q002", "15", new BigDecimal("7.5"))));
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
