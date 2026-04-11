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
