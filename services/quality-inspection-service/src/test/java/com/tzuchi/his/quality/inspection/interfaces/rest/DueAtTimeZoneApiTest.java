package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * D-073：指派與改派的 dueAt 必須帶時區（憲法草案第一條第 2 點）。
 * 拒絕發生在讀取請求本文時、進入 use case 之前，所以案件只需要存在；狀態直接存 entity。
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DueAtTimeZoneApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ReviewCaseJpaRepository jpaRepository;

    private static final String BASE = "/review-cases";
    private final String hmToken = TestTokenFactory.hospitalManagerToken();

    private void seedCase(String caseId) {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(caseId);
        e.setBatchId("b1");
        e.setProjectId("p1");
        e.setHospitalId("H001");
        e.setSourceCaseKey("SK-001");
        e.setMedicalRecordType("OUTPATIENT");
        e.setStatus(CaseStatus.SELECTED_PENDING_ASSIGNMENT);
        jpaRepository.save(e);
    }

    private void assign(String dueAt) throws Exception {
        mockMvc.perform(post(BASE + "/c1/assign-reviewer")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewerId\":\"reviewer-1\",\"dueAt\":\"" + dueAt + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void dueAtWithTaipeiOffsetIsStoredAsTaipeiWallTime() throws Exception {
        seedCase("c1");
        assign("2026-11-26T23:59:00+08:00");
        assertEquals(LocalDateTime.of(2026, 11, 26, 23, 59), jpaRepository.findById("c1").orElseThrow().getDueAt());
    }

    @Test
    void dueAtInUtcIsConvertedToTaipeiWallTime() throws Exception {
        seedCase("c1");
        assign("2026-11-26T15:59:00Z");
        assertEquals(LocalDateTime.of(2026, 11, 26, 23, 59), jpaRepository.findById("c1").orElseThrow().getDueAt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"assign-reviewer", "reassign-reviewer", "assign-rereviewer", "reassign-rereviewer"})
    void dueAtWithoutTimeZoneIsRejected(String command) throws Exception {
        seedCase("c1");
        mockMvc.perform(post(BASE + "/c1/" + command)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewerId\":\"reviewer-1\",\"rereviewerId\":\"rr-1\","
                                + "\"dueAt\":\"2026-11-26T23:59:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
        assertNull(jpaRepository.findById("c1").orElseThrow().getDueAt());
    }

    @Test
    void otherUnreadableBodiesKeepTheExistingResponse() throws Exception {
        seedCase("c1");
        mockMvc.perform(post(BASE + "/c1/assign-reviewer")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewerId\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));
    }
}
