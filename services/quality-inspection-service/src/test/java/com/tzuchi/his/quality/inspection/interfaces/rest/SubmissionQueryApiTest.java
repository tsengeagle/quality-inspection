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
