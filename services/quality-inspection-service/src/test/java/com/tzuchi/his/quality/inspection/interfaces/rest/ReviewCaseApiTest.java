package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseJpaRepository;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReviewCaseApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ReviewCaseJpaRepository jpaRepository;

    private static final String BASE = "/review-cases";
    private final String hmToken = TestTokenFactory.hospitalManagerToken();
    private final String saToken = TestTokenFactory.systemAdminToken();

    private void seedCase(String caseId, CaseStatus status) {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(caseId);
        e.setBatchId("b1");
        e.setProjectId("p1");
        e.setHospitalId("H001");
        e.setSourceCaseKey("SK-001");
        e.setMedicalRecordType("OUTPATIENT");
        e.setStatus(status);
        jpaRepository.save(e);
    }

    private void seedAssignedCase(String caseId) {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(caseId);
        e.setBatchId("b1");
        e.setProjectId("p1");
        e.setHospitalId("H001");
        e.setSourceCaseKey("SK-001");
        e.setMedicalRecordType("OUTPATIENT");
        e.setStatus(CaseStatus.PENDING_INITIAL_REVIEW);
        e.setReviewerId("reviewer-1");
        e.setReviewerAssignedAt(LocalDateTime.now());
        e.setDueAt(LocalDateTime.of(2026, 2, 1, 23, 59));
        jpaRepository.save(e);
    }

    // --- ASSIGN ---

    @Test
    void assignReviewer_shouldReturn200() throws Exception {
        seedCase("c1", CaseStatus.SELECTED_PENDING_ASSIGNMENT);

        AssignReviewerRequest req = new AssignReviewerRequest();
        req.setReviewerId("reviewer-1");
        req.setDueAt(LocalDateTime.of(2026, 2, 1, 23, 59));

        mockMvc.perform(post(BASE + "/c1/assign-reviewer")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void assignReviewer_missingReviewerId_shouldReturn400() throws Exception {
        seedCase("c1", CaseStatus.SELECTED_PENDING_ASSIGNMENT);

        String body = "{\"dueAt\":\"2026-02-01T23:59:00\"}";

        mockMvc.perform(post(BASE + "/c1/assign-reviewer")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignReviewer_systemAdmin_shouldReturn403() throws Exception {
        seedCase("c1", CaseStatus.SELECTED_PENDING_ASSIGNMENT);

        AssignReviewerRequest req = new AssignReviewerRequest();
        req.setReviewerId("reviewer-1");
        req.setDueAt(LocalDateTime.of(2026, 2, 1, 23, 59));

        mockMvc.perform(post(BASE + "/c1/assign-reviewer")
                        .header("api-key", saToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignReviewer_notFound_shouldReturn404() throws Exception {
        AssignReviewerRequest req = new AssignReviewerRequest();
        req.setReviewerId("reviewer-1");
        req.setDueAt(LocalDateTime.of(2026, 2, 1, 23, 59));

        mockMvc.perform(post(BASE + "/no-such/assign-reviewer")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignReviewer_alreadyAssigned_shouldReturn409() throws Exception {
        seedAssignedCase("c1");

        AssignReviewerRequest req = new AssignReviewerRequest();
        req.setReviewerId("reviewer-2");
        req.setDueAt(LocalDateTime.of(2026, 2, 1, 23, 59));

        mockMvc.perform(post(BASE + "/c1/assign-reviewer")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // --- REASSIGN ---

    @Test
    void reassignReviewer_shouldReturn200() throws Exception {
        seedAssignedCase("c1");

        ReassignReviewerRequest req = new ReassignReviewerRequest();
        req.setReviewerId("reviewer-2");
        req.setDueAt(LocalDateTime.of(2026, 3, 1, 23, 59));

        mockMvc.perform(post(BASE + "/c1/reassign-reviewer")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void reassignReviewer_notAssigned_shouldReturn409() throws Exception {
        seedCase("c1", CaseStatus.SELECTED_PENDING_ASSIGNMENT);

        ReassignReviewerRequest req = new ReassignReviewerRequest();
        req.setReviewerId("reviewer-2");
        req.setDueAt(LocalDateTime.of(2026, 3, 1, 23, 59));

        mockMvc.perform(post(BASE + "/c1/reassign-reviewer")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // --- REREVIEW ASSIGN ---

    @Test
    void assignRereviewer_missingDueAt_shouldReturn400() throws Exception {
        seedCase("c1", CaseStatus.PENDING_REREVIEW);

        String body = "{\"rereviewerId\":\"rereviewer-1\"}";

        mockMvc.perform(post(BASE + "/c1/assign-rereviewer")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reassignRereviewer_missingDueAt_shouldReturn400() throws Exception {
        seedCase("c1", CaseStatus.PENDING_REREVIEW);
        ReviewCaseEntity e = jpaRepository.findById("c1").orElseThrow();
        e.setReviewerId("reviewer-1");
        e.setReviewerAssignedAt(LocalDateTime.now());
        e.setRereviewerId("rereviewer-1");
        e.setRereviewerAssignedAt(LocalDateTime.now());
        e.setRereviewerDueAt(LocalDateTime.of(2026, 6, 30, 23, 59));
        jpaRepository.save(e);

        String body = "{\"rereviewerId\":\"rereviewer-2\"}";

        mockMvc.perform(post(BASE + "/c1/reassign-rereviewer")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // --- CANCEL ---

    @Test
    void cancel_shouldReturn200() throws Exception {
        seedCase("c1", CaseStatus.SELECTED_PENDING_ASSIGNMENT);

        CancelReviewCaseRequest req = new CancelReviewCaseRequest();
        req.setReason("project cancelled");

        mockMvc.perform(post(BASE + "/c1/cancel")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void cancel_alreadyCancelled_shouldReturn409() throws Exception {
        seedCase("c1", CaseStatus.CANCELLED);

        CancelReviewCaseRequest req = new CancelReviewCaseRequest();
        req.setReason("again");

        mockMvc.perform(post(BASE + "/c1/cancel")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void cancel_missingReason_shouldReturn400() throws Exception {
        seedCase("c1", CaseStatus.SELECTED_PENDING_ASSIGNMENT);

        mockMvc.perform(post(BASE + "/c1/cancel")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- RETURN ---

    @Test
    void returnCase_shouldReturn200() throws Exception {
        seedCase("c1", CaseStatus.INITIAL_REVIEW_IN_PROGRESS);

        ReturnReviewCaseRequest req = new ReturnReviewCaseRequest();
        req.setReason("needs correction");

        mockMvc.perform(post(BASE + "/c1/initial-review/return")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void returnCase_notInProgress_shouldReturn409() throws Exception {
        seedCase("c1", CaseStatus.SELECTED_PENDING_ASSIGNMENT);

        ReturnReviewCaseRequest req = new ReturnReviewCaseRequest();
        req.setReason("nope");

        mockMvc.perform(post(BASE + "/c1/initial-review/return")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // --- REOPEN ---

    @Test
    void reopen_shouldReturn200() throws Exception {
        seedCase("c1", CaseStatus.RETURNED);
        ReviewCaseEntity e = jpaRepository.findById("c1").orElseThrow();
        e.setReviewerId("reviewer-1");
        e.setReviewerAssignedAt(LocalDateTime.now());
        e.setDueAt(LocalDateTime.of(2026, 2, 1, 23, 59));
        jpaRepository.save(e);

        mockMvc.perform(post(BASE + "/c1/reopen")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());
    }

    @Test
    void reopen_notReturned_shouldReturn409() throws Exception {
        seedAssignedCase("c1");

        mockMvc.perform(post(BASE + "/c1/reopen")
                        .header("api-key", hmToken))
                .andExpect(status().isConflict());
    }

    @Test
    void reopen_noApiKey_shouldReturn401() throws Exception {
        mockMvc.perform(post(BASE + "/c1/reopen"))
                .andExpect(status().isUnauthorized());
    }
}
