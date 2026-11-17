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
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReportApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ReviewCaseJpaRepository caseRepo;

    private final String hmToken = TestTokenFactory.hospitalManagerToken();

    /** 這個類別的測試共用同一個資料庫；每次從空的案件表開始，避免重複建立同一個 caseId。 */
    @org.junit.jupiter.api.BeforeEach
    void clearCases() {
        caseRepo.deleteAll();
    }

    private void seedCase(String caseId, CaseStatus status) {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(caseId);
        e.setBatchId("b1");
        e.setProjectId("p1");
        e.setHospitalId("H001");
        e.setSourceCaseKey("SK-001");
        e.setMedicalRecordType("OUTPATIENT");
        e.setStatus(status);
        e.setCreatedAt(java.time.Instant.parse("2026-04-15T00:00:00Z")); // D-057：落在匯出的時間區間內
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
        req.setFrom(java.time.Instant.parse("2026-04-01T00:00:00Z"));
        req.setTo(java.time.Instant.parse("2026-05-01T00:00:00Z"));

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
        req.setFrom(java.time.Instant.parse("2026-04-01T00:00:00Z"));
        req.setTo(java.time.Instant.parse("2026-05-01T00:00:00Z"));

        mockMvc.perform(post("/reports/export")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
