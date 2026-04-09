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
import static org.junit.jupiter.api.Assertions.assertEquals;
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